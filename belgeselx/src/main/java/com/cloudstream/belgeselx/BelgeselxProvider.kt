package com.cloudstream.belgeselx

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.app
import org.jsoup.nodes.Element

class BelgeselxProvider : MainAPI() {
    override var mainUrl = "https://belgeselx.com/"
    override var name = "Belgeselx"
    override val supportedTypes = setOf(TvType.Movie)
    override var lang = "tr"
    override val hasMainPage = true
    override val hasQuickSearch = false

    private fun searchUrlFor(query: String): String {
        return "https://belgeselx.com/search?q=${query}".replace("${query}", query)
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val home = ArrayList<HomePageList>()
        val doc = app.get("$mainUrl").document
        val items = doc.select(".movie-card a")
        val results = items.mapNotNull { it.toSearchResponse() }
        home.add(HomePageList("Onerilen", results))
        return HomePageResponse(home)
    }

    private fun Element.toSearchResponse(): SearchResponse? {
        val title = select(".movie-card h3").text().ifBlank { text() }
        val href = select(".movie-card a").attr("href").ifBlank { attr("href") }
        val posterUrl = select(".movie-card img").attr("src").ifBlank { attr("data-src") }
        if (title.isBlank() || href.isBlank()) return null
        return newMovieSearchResponse(title, fixUrl(href), TvType.Movie) {
            this.posterUrl = fixUrlNull(posterUrl)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = searchUrlFor(query)
        val doc = app.get(url).document
        return doc.select(".movie-card a").mapNotNull { it.toSearchResponse() }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.select(".movie-card h3").text().ifBlank { "Bilinmeyen" }
        val description = doc.select(".movie-card .description").text()
        val poster = doc.select(".movie-card img").attr("src").ifBlank { attr("data-src") }
        val year = doc.select(".movie-card .year").text().filter { it.isDigit() }.toIntOrNull()
        val genres = doc.select(".movie-card .genre").eachText()
        val embed = doc.select(".embed iframe").attr("src").ifBlank { attr("data-src") }
        return newMovieLoadResponse(title, url, TvType.Movie, embed) {
            this.posterUrl = fixUrlNull(poster)
            this.year = year
            this.plot = description
            this.tags = genres
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        if (data.isBlank()) return false
        loadExtractor(data, "$mainUrl", subtitleCallback, callback)
        return true
    }
}
