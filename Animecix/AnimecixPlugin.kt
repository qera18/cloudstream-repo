// AnimecixPlugin.kt
import cloudstream.android.plugins.Plugin
import cloudstream.android.plugins.PluginManager

class AnimecixPlugin : Plugin() {
    override suspend fun install(): PluginManager.InstalledPlugin {
        return PluginManager.InstalledPlugin(
            id = "Animecix",
            provider = AnimecixProvider()
        )
    }

    override suspend fun uninstall() {
        // No-op
    }

    override fun matches(url: String): Boolean {
        return url.startsWith("https://animecix.tv")
    }
}