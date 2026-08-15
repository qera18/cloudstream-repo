#!/usr/bin/env python3
"""
build.py — scan for pre-built .cs3 artifacts, compute SHA-256 hashes, and
emit plugins.json and repo.json. Does not run Gradle.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import sys
from pathlib import Path
from datetime import datetime, timezone

def sha256_of_file(path: Path, bufsize: int = 1 << 20) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        while True:
            chunk = f.read(bufsize)
            if not chunk:
                break
            h.update(chunk)
    return h.hexdigest()

def scan_cs3_files(root: Path):
    # match pattern like <module>/build/<name>.cs3
    results = []
    for cs3 in sorted(root.glob("*/build/*.cs3")):
        if not cs3.is_file():
            continue
        rel = cs3.relative_to(root)
        module_dir = cs3.parent.parent.name  # parent is build/, parent.parent is module dir
        entry = {
            "module": module_dir,
            "path": str(rel.as_posix()),
            "filename": cs3.name,
            "size": cs3.stat().st_size,
            "sha256": sha256_of_file(cs3),
            "modified_at": datetime.fromtimestamp(cs3.stat().st_mtime, tz=timezone.utc).isoformat(),
        }
        results.append(entry)
    return results

def write_plugins_json(entries, out: Path):
    # Basic structure: list of plugin objects
    plugins = sorted(entries, key=lambda e: (e["module"], e["filename"]))
    out_text = json.dumps({"generated_at": datetime.now(timezone.utc).isoformat(), "plugins": plugins}, indent=2, ensure_ascii=False)
    out.write_text(out_text, encoding="utf-8")
    print(f"Wrote {out} ({len(plugins)} plugins)")

def write_repo_json(entries, out: Path):
    plugin_count = len(entries)
    modules = sorted({e["module"] for e in entries})
    out_text = json.dumps({
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "plugin_count": plugin_count,
        "modules": modules
    }, indent=2, ensure_ascii=False)
    out.write_text(out_text, encoding="utf-8")
    print(f"Wrote {out}")

def main(argv=None):
    parser = argparse.ArgumentParser(description="Scan for .cs3 files and emit plugins.json and repo.json (no Gradle calls).")
    parser.add_argument("--scan-root", "-s", type=Path, default=Path("."), help="Directory root to scan for */build/*.cs3 (default: repo root)")
    parser.add_argument("--plugins-out", type=Path, default=Path("plugins.json"), help="Output plugins.json path")
    parser.add_argument("--repo-out", type=Path, default=Path("repo.json"), help="Output repo.json path")
    args = parser.parse_args(argv)

    entries = scan_cs3_files(args.scan_root)
    write_plugins_json(entries, args.plugins_out)
    write_repo_json(entries, args.repo_out)

if __name__ == "__main__":
    main()
