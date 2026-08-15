package com.example.belgesel

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.MainAPI
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import org.jsoup.nodes.Element

class Belgesel : MainAPI() {
    override var name = "Belgesel"
    override var mainUrl = "https://belgeselx.com/"
    override var lang = "tr"
    override val hasMainPage = true
    override val hasQuickSearch = true

    override val supportedTypes = setOf(TvType.TvSeries)

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl?s=$query"
        val doc = app.get(searchUrl).document
        return doc.select(".px-today-card").mapNotNull { 
            val title = it.selectFirst(".px-today-card-title")?.text() ?: return@mapNotNull null
            val href = it.attr("href")
            val posterUrl = it.selectFirst("img")?.attr("src")
            newTvSeriesSearchResponse(title, href) { this.posterUrl = posterUrl }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1")?.text() ?: ""
        val poster = doc.selectFirst("img.poster")?.attr("src")
        val description = doc.selectFirst(".plot")?.text()
        val episodes = doc.select(".px-today-card").mapNotNull { 
            val epHref = it.attr("href")
            newEpisode(fixUrlNull(epHref)) { this.name = it.selectFirst(".px-today-card-title")?.text() ?: "Bölüm" }
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
