package com.example.testsite

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.network.*
import org.jsoup.nodes.Element

class TestSite : MainAPI() {
    override var name = "TestSite"
    override var mainUrl = "https://dizipal.com"
    override var lang = "tr"
    override val hasMainPage = true
    override val hasQuickSearch = true

    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/search?q=$query"
        val doc = app.get(searchUrl).document
        return doc.select("li").mapNotNull { 
            val title = it.selectFirst(".anchortext a")?.text() ?: return@mapNotNull null
            val href = it.selectFirst(".anchorhref")?.attr("href") ?: return@mapNotNull null
            newMovieSearchResponse(title, fixUrl(href), TvType.Movie)
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst(".heading")?.text() ?: ""
        val poster = doc.selectFirst("img")?.attr("src")
        return newMovieLoadResponse(title, url, TestSite::loadLinks, "") {
            this.posterUrl = fixUrlNull(poster)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val doc = app.get(data).document
        val iframeSrc = doc.selectFirst("iframe")?.attr("src")
        if (iframeSrc != null) {
            loadExtractor(iframeSrc, mainUrl, subtitleCallback, callback)
        }
        return true
    }
}