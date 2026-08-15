package com.example.asya

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.network.*
import org.jsoup.nodes.Element

class Asya : MainAPI() {
    override var name = "Asya"
    override var mainUrl = "https://asyaminik.com/"
    override var lang = "tr"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "${mainUrl}?s=$query"
        val doc = app.get(searchUrl).document
        return doc.select("article.post").mapNotNull {
            val title = it.selectFirst("h2.entry-title a")?.text() ?: return@mapNotNull null
            val href = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = it.selectFirst("img")?.attr("data-src") ?: it.selectFirst("img")?.attr("src")
            newMovieSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = poster }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1.entry-title")?.text() ?: ""
        val poster = doc.selectFirst(".entry-content img")?.attr("src")
        val description = doc.selectFirst(".entry-content p")?.text()
        val episodes = doc.select(".entry-content a").filter { it.attr("href").contains("asyaminik") }.mapIndexed { i, it ->
            newEpisode(it.attr("href")) { this.name = "Bölüm ${i + 1}" }
        }
        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) { this.posterUrl = poster; this.plot = description }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val doc = app.get(data).document
        doc.select("iframe").forEach { 
            loadExtractor(it.attr("src"), data, subtitleCallback, callback)
        }
        return true
    }
}