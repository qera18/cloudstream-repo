package com.example.dizitest

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.MainAPI
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.network.appGet

class DiziTest : MainAPI() {
    override var name = "DiziTest"
    override var mainUrl = "https://dizipal5.com"
    override var lang = "tr"
    override val hasMainPage = true
    override val hasQuickSearch = true

    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/arama?q=$query"
        val doc = app.get(searchUrl).document
        return doc.select(".content-card").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".title")?.text() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1")?.text() ?: ""
        val poster = doc.selectFirst("img.poster")?.attr("src")
        val description = doc.selectFirst(".description")?.text()
        val episodes = doc.select(".episode-item").mapNotNull {
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
        val iframeSrc = doc.selectFirst("iframe")?.attr("src") ?: return false
        loadExtractor(iframeSrc, mainUrl, subtitleCallback, callback)
        return true
    }
}
