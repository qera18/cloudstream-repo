import cloudstream.android.plugins.Plugin
import cloudstream.android.plugins.PluginManager

class AsyawatchPlugin : Plugin() {
    override fun getProvider(): Provider {
        return AsyawatchProvider()
    }

    override fun getLanguage(): LanguageCode {
        return LanguageCode.ENGLISH
    }

    override fun getMainUrl(): String {
        return "https://asyawatch.com/"
    }

    override fun getSupportedTypes(): List<TvType> {
        return listOf(TvType.Movie, TvType.Series)
    }
}