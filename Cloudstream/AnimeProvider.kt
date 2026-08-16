package com.lagradost.cloudstream3

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import org.jsoup.nodes.Element
import org.jsoup.Jsoup

class AnimeProvider : MainAPI() {
    companion object {
        val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Anime)
    }

    override var mainUrl = "https://asyaminik.com"
    override var name = "Anime"
    override val supportedTypes = AnimeProvider.supportedTypes

    override val mainPage = mainPageOf(
        "/" to "Son Eklenenler",
        "/category/anime/" to "Anime",
        "/category/diziler/" to "Diziler",
        "/category/filmler/" to "Filmler",
        "/category/haberler/" to "Haberler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) "$mainUrl${request.data}" else "$mainUrl${request.data}page/$page/"
        val document = app.get(url).document
        val home = document.select("article.post, .item, .post-column").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val titleElement = this.selectFirst(".entry-title a, .post-title a, h2 a") ?: return null
        val title = titleElement.text().trim()
        val href = titleElement.attr("abs:href")
        val posterUrl = this.selectFirst("img")?.attr("abs:src")
        
        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("article.post, .item, .post-column").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1.entry-title, .post-title")?.text()?.trim() ?: ""
        val poster = document.selectFirst("meta[property='og:image']")?.attr("content")
        val plot = document.selectFirst(".entry-content p, .post-content p")?.text()
        
        val isMovie = url.contains("/filmler/") || url.contains("-filmi/") || document.select(".category-filmler").isNotEmpty()
        
        return if (isMovie) {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = plot
            }
        } else {
            // WordPress posts are often individual episodes, but we treat them as a series entry point
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, listOf(Episode(url, "İzle"))) {
                this.posterUrl = poster
                this.plot = plot
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isDataJob: Boolean,
        callback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        // Look for common WordPress embed patterns
        val frames = document.select("iframe[src*='embed'], iframe[src*='player'], iframe[src*='video'], .video-container iframe")
        
        frames.forEach { iframe ->
            var src = iframe.attr("src")
            if (src.startsWith("//")) src = "https:$src"
            
            if (src.isNotEmpty() && !src.contains("facebook.com") && !src.contains("twitter.com")) {
                loadExtractor(src, data, callback, callback)
            }
        }

        // Check for links in content that might be players
        document.select(".entry-content a[href*='drive.google'], .entry-content a[href*='ok.ru'], .entry-content a[href*='mail.ru']").forEach {
            val href = it.attr("abs:href")
            loadExtractor(href, data, callback, callback)
        }

        return true
    }
}