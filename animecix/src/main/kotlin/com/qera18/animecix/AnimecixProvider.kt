package com.qera18.animecix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import org.jsoup.nodes.Element

class AnimecixProvider : MainAPI() {
    override var mainUrl = "https://animecix.tv"
    override var name = "AnimeCix"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "en"
    override val hasMainPage = true

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get(mainUrl + "".replace("{query}", query)).document
        return doc.select("").mapNotNull { element -> element.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = selectFirst("")?.text() ?: return null
        val href = fixUrl(selectFirst("")?.attr("href") ?: return null)
        val poster = selectFirst("")?.attr("src")?.let(::fixUrlNull)
        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = poster }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("")?.text() ?: name
        val poster = doc.selectFirst("")?.attr("src")?.let(::fixUrlNull)
        val description = doc.selectFirst("")?.text()
        return newMovieLoadResponse(title, url, TvType.Movie, url) { posterUrl = poster; plot = description }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val doc = app.get(data).document
        doc.select("").map { it.attr("") }.filter { it.isNotBlank() }.forEach { loadExtractor(fixUrl(it), data, subtitleCallback, callback) }
        return true
    }
}