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
    override val hasMainPage = false
    override var lang = "en"
    override val supportedTypes = setOf(
        TvType.Anime
    )

    override val mainPage = mainPageOf(
        "$mainUrl/movies?page=" to "Movies",
        "$mainUrl/tv-shows?page=" to "TV Shows",
        "$mainUrl/latest?page=" to "Latest",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("div.film_list-wrap div.flw-item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3.film-name a")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("h3.film-name a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))
            ?: fixUrlNull(this.selectFirst("img")?.attr("src"))
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/search/$query").document
        return document.select("div.film_list-wrap div.flw-item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h2.heading-name")?.text()?.trim() ?: "Untitled"
        val poster = fixUrlNull(document.selectFirst("div.film-poster img")?.attr("src"))
        val plot = document.selectFirst("div.description")?.text()?.trim()
        val tags = document.select("div.row-line a").map { it.text() }
        val year = document.selectFirst("span.year")?.text()?.trim()?.toIntOrNull()
        val trailer = document.selectFirst("iframe.trailer")?.attr("src")

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = plot
            this.tags = tags
            this.year = year
            addTrailer(trailer)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        document.select("div.server-item a").apmap { server ->
            val embedUrl = fixUrlNull(server.attr("data-embed")) ?: return@apmap
            loadExtractor(embedUrl, data, subtitleCallback, callback)
        }
        return true
    }
}
