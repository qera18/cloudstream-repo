package com.qera.belgeselx

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.TvType
import org.jsoup.nodes.Element

class BelgeselxProvider : MainAPI() {
    override var mainUrl = "https://belgeselx.com/"
    override var name = "Belgeselx"
    override val supportedTypes = setOf(TvType.TvSeries)
    override var lang = "tr"
    override val hasMainPage = true

    private fun Element.toSearchResponse(): SearchResponse? {
        val title = this.selectFirst("meta[property='og:title']")?.text()?.trim() ?: return null
        val href = this.selectFirst("link[rel='canonical']")?.attr("abs:href") ?: this.attr("abs:href")
        val poster = this.selectFirst("meta[property='og:image']")?.attr("abs:src")
        return if (href.isNullOrEmpty()) null else newMovieSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = poster
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val home = document.select("meta[property='og:title']").mapNotNull { it.toSearchResponse() }
        return newHomePageResponse(listOf(HomePageList(this.name, home)), hasNext = false)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "".replace("${query}", query)
        val document = app.get(url).document
        return document.select("meta[property='og:title']").mapNotNull { it.toSearchResponse() }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("meta[property='og:title']")?.text()?.trim() ?: ""
        val poster = document.selectFirst("meta[property='og:image']")?.attr("abs:src")
        val description = document.selectFirst("meta[property='og:description']")?.text()?.trim() ?: ""
        val year = document.selectFirst("")?.text()?.trim()?.toIntOrNull()
        return newMovieLoadResponse(title, url, TvType.TvSeries, url) {
            this.posterUrl = poster
            this.year = year
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        document.select("div.px-yt-lazy[data-yt-id]").forEach { el ->
            val embedUrl = el.attr("abs:src").ifEmpty { el.attr("abs:data-src") }
            if (!embedUrl.isNullOrEmpty()) {
                loadExtractor(embedUrl, data, subtitleCallback, callback)
            }
        }
        return true
    }
}
