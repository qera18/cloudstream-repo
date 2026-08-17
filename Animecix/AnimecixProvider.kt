// AnimecixProvider.kt
import cloudstream.android.providers.Provider
import cloudstream.android.providers.TvType
import cloudstream.android.utils.LanguageCode
import cloudstream.android.utils.LinkType
import cloudstream.android.utils.SubtitleType
import cloudstream.android.utils.UrlId
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.SubtitleData
import java.util.concurrent.TimeUnit

class AnimecixProvider : Provider() {
    override val mainUrl = "https://animecix.tv"
    override val name = "Animecix"
    override val lang = LanguageCode.ENGLISH
    override val supportsEpisodes = true
    override val supportedTypes = setOf(TvType.Anime, TvType.OVA)
    override val hasMainPage = true

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search?q=$query"
        val soup = app.get(url).document
        val items = soup.select("div.item")
        return items.map { item ->
            val title = item.selectFirst("h2.title")!!.text()
            val id = item.selectFirst("a")!!.href()
            SearchResponse(
                title,
                id,
                TvType.Anime,
                UrlId(id)
            )
        }
    }

    override suspend fun loadHome(): List<HomeSearchResponse> {
        val url = mainUrl
        val soup = app.get(url).document
        val items = soup.select("div.item")
        return items.map { item ->
            val title = item.selectFirst("h2.title")!!.text()
            val id = item.selectFirst("a")!!.href()
            HomeSearchResponse(
                title,
                id,
                TvType.Anime,
                UrlId(id)
            )
        }
    }

    override suspend fun loadEpisode(episodeId: String): EpisodeData {
        val url = episodeId
        val soup = app.get(url).document
        val title = soup.selectFirst("h1.title")!!.text()
        val links = soup.select("div.player")
        val episodeLinks = links.map { link ->
            ExtractorLink(
                link.selectFirst("iframe")!!.attr("src"),
                "Animecix",
                Qualities.P1080p.value,
                LinkType.VIDEO
            )
        }
        return EpisodeData(
            title,
            episodeLinks
        )
    }
}