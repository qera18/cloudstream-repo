package com.example.dizi

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.MainAPI
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.network.*

class Dizi : MainAPI() {
    override var name = "Dizi"
    override var mainUrl = "https://dizi73.life/"
    override var lang = "tr"
    override val hasMainPage = true
    override val hasQuickSearch = true

    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl?s=$query"
        val doc = app.get(searchUrl).document
        return doc.select(".post-item, .item").mapNotNull { element ->
            val title = element.selectFirst("h2, .title")?.text() ?: return@mapNotNull null
            val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val posterUrl = element.selectFirst("img")?.attr("src")
            newMovieSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst(".entry-title, h1")?.text() ?: ""
        val poster = doc.selectFirst(".poster img")?.attr("src")
        val description = doc.selectFirst(".entry-content, .description")?.text()
        val episodes = doc.select(".episode-list a, .episodes li a").mapNotNull { ep ->
            newEpisode(ep.attr("href")) { this.name = ep.text() }
        }
        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val doc = app.get(data).document
        val iframeSrc = doc.selectFirst("iframe")?.attr("src") ?: return false
        loadExtractor(iframeSrc, mainUrl, subtitleCallback, callback)
        return true
    }
}
