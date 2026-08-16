package com.lagradost.cloudstream3

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import org.jsoup.nodes.Element
import org.jsoup.Jsoup

class TestPluginProvider : MainAPI() {
    override var mainUrl = "https://example.com"
    override var name = "TestPlugin"
    
    companion object {
        val supportedTypes = setOf(
            TvType.Movie,
            TvType.TvSeries,
            TvType.Anime,
            TvType.LiveStream
        )
    }

    override val supportedTypes = Companion.supportedTypes

    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val items = document.select(".flw-item, .post-item, .movie-layout").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".film-name a, .entry-title, h2")?.text() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href") ?: return null)
        val posterUrl = fixUrl(this.select("img").attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/search?keyword=$query"
        val document = app.get(searchUrl).document
        return document.select(".flw-item, .post-item, .movie-layout").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst(".film-name, .entry-title, h2")?.text()?.trim() ?: ""
        val poster = fixUrl(document.select("img.film-poster-img, .poster img").attr("src"))
        val description = document.select(".description, .entry-content, .description-text").text().trim()
        val trailer = document.select("iframe[src*='youtube']").attr("src")

        val isTv = document.select(".episodes, .seasons, .sl-container").isNotEmpty()

        return if (isTv) {
            val episodes = document.select(".episode-item, .ep-item, .sl-item").map {
                val href = fixUrl(it.attr("href"))
                val name = it.text().trim()
                Episode(href, name)
            }
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = description
                addTrailer(trailer)
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
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
        val document = app.get(data).document
        
        // Resolve using generic embed pattern from site analysis
        document.select("iframe[src*='embed']").forEach {
            val src = fixUrl(it.attr("src"))
            loadExtractor(src, data, subtitleCallback, callback)
        }

        // Logic for the specific ID-based embed pattern
        val id = data.split("/").lastOrNull { it.isNotEmpty() }
        if (id != null) {
            val embedUrl = "https://example.com/embed/$id"
            loadExtractor(embedUrl, data, subtitleCallback, callback)
        }

        return true
    }
}