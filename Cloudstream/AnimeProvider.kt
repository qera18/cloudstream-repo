package com.lagradost.cloudstream3

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import org.jsoup.nodes.Element
import org.jsoup.Jsoup

class AnimeProvider : MainAPI() {
    override var mainUrl = "https://asyaminik.com"
    override var name = "Anime"
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie, TvType.Anime)
    override var lang = "tr"
    override val hasMainPage = true

    companion object {
        val supportedTypes = setOf(TvType.TvSeries, TvType.Movie, TvType.Anime)
    }

    override val mainPage = mainPageOf(
        "category/diziler/" to "Diziler",
        "category/filmler/" to "Filmler",
        "category/anime/" to "Anime",
        "category/haberler/" to "Haberler",
        "category/fragmanlar/" to "Fragmanlar"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) "$mainUrl/${request.data}" else "$mainUrl/${request.data}page/$page/"
        val document = app.get(url).document
        val home = document.select("article").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".entry-title a")?.text() ?: return null
        val href = this.selectFirst(".entry-title a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.attr("src")
        
        return if (title.contains("Film", ignoreCase = true)) {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
            }
        } else {
            newAnimeSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("article").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1.entry-title")?.text()?.trim() ?: return null
        val poster = document.selectFirst("meta[property='og:image']")?.attr("content") 
            ?: document.selectFirst(".post-thumbnail img")?.attr("src")
        val description = document.select(".entry-content p").firstOrNull()?.text()?.trim()

        val episodeLinks = document.select(".entry-content a").filter { 
            val text = it.text().lowercase()
            text.contains("bölüm") || text.contains("izle") || it.attr("href").contains("bolum")
        }

        return if (episodeLinks.isEmpty()) {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
            }
        } else {
            val episodes = episodeLinks.map {
                Episode(
                    data = it.attr("href"),
                    name = it.text().trim()
                )
            }.distinctBy { it.data }
            
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = description
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
        
        // Handle direct iframes
        document.select("iframe").forEach { iframe ->
            val src = iframe.attr("src").let { 
                if (it.startsWith("//")) "https:$it" else it 
            }
            loadExtractor(src, data, subtitleCallback, callback)
        }

        // Handle links that might lead to extractors (OK.ru, Fembed, etc)
        document.select(".entry-content a").forEach { link ->
            val href = link.attr("href")
            if (href.contains("ok.ru") || href.contains("mail.ru") || href.contains("fembed") || href.contains("sbani.me")) {
                loadExtractor(href, data, subtitleCallback, callback)
            }
        }

        return true
    }
}