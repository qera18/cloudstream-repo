package com.example.dizi

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.MainAPI
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import org.jsoup.nodes.Element

class Dizi : MainAPI() {
    override var name = "Dizi"
    override var mainUrl = "https://www.dizimom.surf/"
    override var lang = "tr"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl?s=$query"
        val doc = app.get(searchUrl).document
        return doc.select(".post-item").mapNotNull { element ->
            val title = element.selectFirst("h2")?.text() ?: return@mapNotNull null
            val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = element.selectFirst("img")?.attr("src")
            MovieSearchResponse(title, href, this.name, TvType.TvSeries, poster, null, null)
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst(".entry-title")?.text() ?: ""
        val description = doc.selectFirst(".entry-content")?.text()
        val poster = doc.selectFirst(".attachment-post-thumbnail")?.attr("src")
        val episodes = doc.select(".episodelist li a").mapNotNull { ep ->
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
        loadExtractor(iframeSrc, data, subtitleCallback, callback)
        return true
    }
}
