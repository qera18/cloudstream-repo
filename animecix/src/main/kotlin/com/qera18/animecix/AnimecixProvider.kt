package com.qera18.animecix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import org.jsoup.nodes.Element

class AnimecixProvider : MainAPI() {
    override var mainUrl = "https://animecix.tv"
    override var name = "Animecix"
    override val supportedTypes = setOf(TvType.Anime)
    override var lang = "en"
    override val hasMainPage = true

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "https://animecix.tv/search/{query}".replace("{query}", query)
        val doc = app.get(mainUrl + searchUrl).document
        return doc.select("").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = selectFirst("")?.text() ?: return null
        val href = fixUrl(selectFirst("a")?.attr("href") ?: return null)
        val poster = selectFirst("")?.attr("src")?.let(::fixUrlNull)
        return newMovieSearchResponse(title, href, TvType.Anime) { this.posterUrl = poster }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("")?.text() ?: name
        val poster = doc.selectFirst("")?.attr("src")?.let(::fixUrlNull)
        return newMovieLoadResponse(title, url, TvType.Anime, url) { posterUrl = poster }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val doc = app.get(data).document
        doc.select("").forEach { el ->
            val src = el.attr("src").ifBlank { el.attr("data-src") }
            if (src.isNotBlank()) loadExtractor(fixUrl(src), data, subtitleCallback, callback)
        }
        return true
    }
}