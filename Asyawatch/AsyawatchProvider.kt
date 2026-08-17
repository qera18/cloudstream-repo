import cloudstream.android.providers.Provider
import cloudstream.android.providers.SearchResult
import cloudstream.android.providers.TvType
import cloudstream.android.utils.LanguageCode
import cloudstream.android.utils.LinkType
import cloudstream.android.utils.UrlId
import cloudstream.android.utils.getQualityFromName
import cloudstream.android.utils.parseHtml
import cloudstream.android.utils.parseHtmlDocument
import cloudstream.android.utils.parseHtmlList
import okhttp3.Request
import java.util.*

class AsyawatchProvider : Provider() {
    override fun getSearchRequest(query: String): Request {
        return Request.Builder()
            .url("https://asyawatch.com/search/$query")
            .build()
    }

    override fun getSearchResult(html: String): List<SearchResult> {
        val document = parseHtmlDocument(html)
        val searchResults = mutableListOf<SearchResult>()
        val list = document.select("div.post")
        for (item in list) {
            val title = item.select("h2.entry-title").text()
            val url = item.select("a").attr("href")
            val image = item.select("img").attr("src")
            searchResults.add(
                SearchResult(
                    title = title,
                    url = url,
                    type = TvType.Movie,
                    id = UrlId(url),
                    image = image
                )
            )
        }
        return searchResults
    }

    override fun getEpisodeRequest(episodeId: String): Request {
        return Request.Builder()
            .url(episodeId)
            .build()
    }

    override fun getEpisodeList(html: String): List<String> {
        val document = parseHtmlDocument(html)
        val episodeList = mutableListOf<String>()
        val list = document.select("div.episode")
        for (item in list) {
            val url = item.select("a").attr("href")
            episodeList.add(url)
        }
        return episodeList
    }

    override fun getVideoRequest(episodeId: String): Request {
        return Request.Builder()
            .url(episodeId)
            .build()
    }

    override fun getVideoUrl(html: String): String {
        val document = parseHtmlDocument(html)
        val iframe = document.select("iframe").attr("src")
        return iframe
    }
}