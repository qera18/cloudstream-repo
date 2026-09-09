#!/usr/bin/env python3
"""
Build CloudStream plugins with Gradle and serve a status page on port 3000.
The HTTP server starts immediately with a 'building' page; the Gradle build
runs in a background thread and the page is regenerated when it finishes.
"""
import subprocess, os, json, threading, html, shutil
from http.server import HTTPServer, SimpleHTTPRequestHandler
from pathlib import Path
from datetime import datetime, timezone

APP_DIR = Path("/app")
OUTPUT_DIR = APP_DIR / ".base44-output"
PORT = 3000

state = {"status": "building", "log_tail": "", "plugins": [], "error": ""}


def run_build():
    global state
    # Create local.properties so Gradle can find the Android SDK
    (APP_DIR / "local.properties").write_text(
        f"sdk.dir={os.environ.get('ANDROID_HOME', '/opt/android-sdk')}\n"
    )
    gradlew = APP_DIR / "gradlew"
    if gradlew.exists():
        gradlew.chmod(0o755)

    try:
        result = subprocess.run(
            ["./gradlew", ":Animecix:make", "--no-daemon", "--stacktrace"],
            cwd=str(APP_DIR),
            capture_output=True,
            text=True,
            timeout=900,
        )
        log = (result.stdout or "") + (result.stderr or "")
        tail = "\n".join(log.strip().splitlines()[-40:])

        if result.returncode != 0:
            state = {"status": "failed", "log_tail": tail, "plugins": [], "error": f"Gradle exited with code {result.returncode}"}
        else:
            plugins = scan_cs3()
            state = {"status": "success", "log_tail": tail, "plugins": plugins, "error": ""}
    except subprocess.TimeoutExpired:
        state = {"status": "failed", "log_tail": "Build timed out after 900s", "plugins": [], "error": "timeout"}
    except Exception as e:
        state = {"status": "failed", "log_tail": str(e), "plugins": [], "error": str(e)}

    copy_artifacts()
    generate_page()


def scan_cs3():
    plugins = []
    for f in sorted(APP_DIR.glob("*/build/*.cs3")):
        plugins.append({
            "name": f.name,
            "module": f.parent.parent.name,
            "path": str(f.relative_to(APP_DIR)),
            "size": f.stat().st_size,
        })
    return plugins


def copy_artifacts():
    """Copy .cs3 files and plugins.json to the output dir for download."""
    for plugin in state.get("plugins", []):
        src = APP_DIR / plugin["path"]
        dst = OUTPUT_DIR / plugin["name"]
        if src.exists():
            shutil.copy2(src, dst)
    # Also copy plugins.json if it exists
    pj = APP_DIR / "plugins.json"
    if pj.exists():
        shutil.copy2(pj, OUTPUT_DIR / "plugins.json")


def generate_page():
    OUTPUT_DIR.mkdir(exist_ok=True)
    (OUTPUT_DIR / "index.html").write_text(build_html(), encoding="utf-8")


def build_html():
    status = state["status"]
    plugins = state.get("plugins", [])
    log_tail = state.get("log_tail", "")
    error = state.get("error", "")

    # Status badge
    if status == "building":
        badge_cls = "building"
        badge_text = "Building…"
        refresh = '<meta http-equiv="refresh" content="5">'
    elif status == "success":
        badge_cls = "success"
        badge_text = "Build Successful"
        refresh = ""
    else:
        badge_cls = "failed"
        badge_text = "Build Failed"
        refresh = ""

    # Plugin cards
    plugin_cards = ""
    if plugins:
        for p in plugins:
            size_kb = p["size"] / 1024
            plugin_cards += f"""
            <div class="card">
                <div class="card-header">
                    <span class="plugin-name">{html.escape(p['name'])}</span>
                    <span class="badge badge-sm">{p['module']}</span>
                </div>
                <div class="card-body">
                    <span class="meta">{size_kb:.1f} KB</span>
                    <a href="/{html.escape(p['name'])}" download>Download .cs3</a>
                </div>
            </div>"""
    elif status == "success":
        plugin_cards = '<p class="empty">No .cs3 files were produced. Check the build log.</p>'
    else:
        plugin_cards = ""

    # Log section
    log_section = ""
    if log_tail:
        log_section = f"""
        <div class="log-section">
            <h2>Build Log (last 40 lines)</h2>
            <pre class="log">{html.escape(log_tail)}</pre>
        </div>"""

    # Error section
    error_section = ""
    if error:
        error_section = f'<div class="error-banner">{html.escape(error)}</div>'

    now = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
{refresh}
<title>CloudStream Plugin Build</title>
<style>
  * {{ margin: 0; padding: 0; box-sizing: border-box; }}
  body {{
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    background: #0f1117; color: #e1e4e8; min-height: 100vh; padding: 24px;
  }}
  .container {{ max-width: 800px; margin: 0 auto; }}
  header {{ text-align: center; margin-bottom: 32px; }}
  header h1 {{ font-size: 1.6rem; margin-bottom: 4px; }}
  header p {{ color: #8b949e; font-size: 0.95rem; }}
  .status-badge {{
    display: inline-block; padding: 8px 20px; border-radius: 999px;
    font-weight: 600; font-size: 0.9rem; margin: 16px 0;
  }}
  .status-badge.building {{ background: #1f3a5f; color: #79c0ff; }}
  .status-badge.success {{ background: #1a3a1a; color: #56d364; }}
  .status-badge.failed {{ background: #3a1a1a; color: #f85149; }}
  .building .status-badge.building::before {{ content: "⏳ "; }}
  .success .status-badge.success::before {{ content: "✅ "; }}
  .failed .status-badge.failed::before {{ content: "❌ "; }}
  .error-banner {{
    background: #3a1a1a; border: 1px solid #f8514940; color: #f85149;
    padding: 12px 16px; border-radius: 8px; margin-bottom: 16px;
    font-size: 0.9rem;
  }}
  .plugins {{ display: flex; flex-direction: column; gap: 12px; margin-bottom: 24px; }}
  .card {{
    background: #161b22; border: 1px solid #30363d; border-radius: 8px;
    padding: 16px; transition: border-color 0.2s;
  }}
  .card:hover {{ border-color: #586069; }}
  .card-header {{ display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }}
  .plugin-name {{ font-weight: 600; font-size: 1rem; }}
  .badge {{
    display: inline-block; padding: 2px 10px; border-radius: 999px;
    font-size: 0.75rem; font-weight: 600;
    background: #1f2937; color: #9ca3af;
  }}
  .badge-sm {{ background: #1f2a1f; color: #56d364; }}
  .card-body {{ display: flex; justify-content: space-between; align-items: center; }}
  .meta {{ color: #8b949e; font-size: 0.85rem; }}
  .card-body a {{
    color: #58a6ff; text-decoration: none; font-size: 0.85rem;
    padding: 4px 12px; border: 1px solid #30363d; border-radius: 6px;
  }}
  .card-body a:hover {{ background: #1f2937; }}
  .empty {{ color: #8b949e; text-align: center; padding: 24px; }}
  .log-section {{ margin-top: 24px; }}
  .log-section h2 {{ font-size: 1rem; margin-bottom: 8px; color: #8b949e; }}
  .log {{
    background: #0d1117; border: 1px solid #30363d; border-radius: 8px;
    padding: 16px; font-size: 0.8rem; font-family: 'SF Mono', Consolas, monospace;
    overflow-x: auto; max-height: 400px; overflow-y: auto; white-space: pre-wrap;
    color: #c9d1d9;
  }}
  footer {{ text-align: center; margin-top: 32px; color: #484f58; font-size: 0.8rem; }}
  .spinner {{
    display: inline-block; width: 16px; height: 16px;
    border: 2px solid #30363d; border-top-color: #79c0ff;
    border-radius: 50%; animation: spin 0.8s linear infinite;
    vertical-align: middle; margin-right: 8px;
  }}
  @keyframes spin {{ to {{ transform: rotate(360deg); }} }}
</style>
</head>
<body class="{badge_cls}">
<div class="container">
  <header>
    <h1>CloudStream Plugin Repository</h1>
    <p>Belgesel Repo — CloudStream3 plugin build</p>
    <div class="status-badge {badge_cls}">
      {('<span class="spinner"></span>' if status == 'building' else '')}{badge_text}
    </div>
  </header>

  {error_section}

  <div class="plugins">
    {plugin_cards}
  </div>

  {log_section}

  <footer>
    <p>Generated {now} · <a href="/plugins.json" style="color:#58a6ff">plugins.json</a></p>
  </footer>
</div>
</body>
</html>"""


class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(OUTPUT_DIR), **kwargs)

    def log_message(self, fmt, *args):
        pass  # suppress access logs


def main():
    OUTPUT_DIR.mkdir(exist_ok=True)
    generate_page()  # initial "building" page

    # Start build in background
    t = threading.Thread(target=run_build, daemon=True)
    t.start()

    # Start HTTP server
    server = HTTPServer(("0.0.0.0", PORT), Handler)
    print(f"Serving on http://0.0.0.0:{PORT}")
    server.serve_forever()


if __name__ == "__main__":
    main()
