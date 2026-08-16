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
        val searchUrl = "${mainUrl}?&s=$query"
        val doc = app.get(searchUrl).document
        return doc.select(".result-item").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".title")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val poster = this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src")
        return newMovieSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = poster }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1")?.text() ?: ""
        val poster = doc.selectFirst(".poster img")?.attr("src")
        val description = doc.selectFirst(".description")?.text()
        val episodes = doc.select(".episodelist li").mapNotNull {
            val epHref = it.selectFirst("a")?.attr("href")
            newEpisode(fixUrlNull(epHref)) { this.name = it.text() }
        }
        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val doc = app.get(data).document
        val iframeSrc = doc.selectFirst("iframe.player-iframe")?.attr("src") ?: return false
        loadExtractor(iframeSrc, mainUrl, subtitleCallback, callback)
        return true
    }
}
