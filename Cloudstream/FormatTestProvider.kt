package com.lagradost.cloudstream3

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import org.jsoup.nodes.Element
import org.jsoup.Jsoup

class FormatTestProvider : MainAPI() {
    override var mainUrl = "https://testsite.example"
    override var name = "FormatTest"
    override val hasMainPage = true

    companion object {
        val supportedTypes = setOf(
            TvType.Movie,
            TvType.TvSeries,
            TvType.Anime
        )
    }

    override val mainPage = mainPageOf(
        "$mainUrl/movies" to "Latest Movies",
        "$mainUrl/series" to "Latest Series",
        "$mainUrl/anime" to "Latest Anime"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data).document
        val home = document.select("div.item, .card, .movie-card").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".title, h3, .card-title")?.text() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src")
        
        return if (href.contains("/anime/")) {
            newAnimeSearchResponse(title, fixUrl(href)) {
                this.posterUrl = fixUrlNull(posterUrl)
            }
        } else {
            newMovieSearchResponse(title, fixUrl(href), TvType.Movie) {
                this.posterUrl = fixUrlNull(posterUrl)
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search?q=$query"
        val document = app.get(url).document
        return document.select("div.item, .card, .movie-card").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst(".title, h1")?.text() ?: return null
        val poster = document.selectFirst("img.poster, .detail-poster img")?.attr("src")
        val plot = document.selectFirst(".description, .plot")?.text()
        val year = document.selectFirst(".year")?.text()?.toIntOrNull()
        val trailer = document.selectFirst("a.trailer")?.attr("href")
        
        val type = when {
            url.contains("/anime/") -> TvType.Anime
            url.contains("/series/") -> TvType.TvSeries
            else -> TvType.Movie
        }

        val tmdbId = document.selectFirst("meta[name=tmdb-id]")?.attr("content")
        val anilistId = document.selectFirst("meta[name=anilist-id]")?.attr("content")
        val contentId = tmdbId ?: anilistId ?: url.split("/").lastOrNull() ?: return null

        if (type == TvType.Movie) {
            return newMovieLoadResponse(title, url, TvType.Movie, "$contentId|movie") {
                this.posterUrl = fixUrlNull(poster)
                this.plot = plot
                this.year = year
                addTrailer(trailer)
            }
        } else {
            val episodes = document.select(".episode-item, .ep-link").mapNotNull {
                val epHref = it.attr("href")
                val epTitle = it.text()
                val s = it.attr("data-season").toIntOrNull() ?: 1
                val e = it.attr("data-episode").toIntOrNull() ?: 1
                Episode("$contentId|${if(type == TvType.Anime) "anime" else "tv"}|$s|$e", epTitle, s, e)
            }
            return newTvSeriesLoadResponse(title, url, type, episodes) {
                this.posterUrl = fixUrlNull(poster)
                this.plot = plot
                this.year = year
                addTrailer(trailer)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val parts = data.split("|")
        val id = parts.getOrNull(0) ?: return false
        val type = parts.getOrNull(1) ?: "movie"
        val season = parts.getOrNull(2) ?: "1"
        val episode = parts.getOrNull(3) ?: "1"

        val embedUrl = if (type == "movie") {
            "https://player.voidverse.me/embed/movie/$id"
        } else {
            "https://player.voidverse.me/embed/$type/$id?s=$season&e=$episode"
        }

        // VoidVerse / VidPlus typical extraction logic
        loadExtractor(embedUrl, subtitleCallback, callback)
        
        return true
    }
}