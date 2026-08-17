package com.lagradost.cloudstream3

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element
import org.jsoup.Jsoup

class AnimeProvider : MainAPI() {
    override var mainUrl = "https://cizgimax.online"
    override var name = "Anime"
    override val hasMainPage = true
    override var lang = "tr"

    companion object {
        val supportedTypes = setOf(
            TvType.Anime,
            TvType.Cartoon,
            TvType.TvSeries,
            TvType.Movie
        )
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val categories = listOf(
            Pair("Çizgi Film", "$mainUrl/kategori/cizgi-film/page/$page/"),
            Pair("Anime", "$mainUrl/kategori/anime/page/$page/"),
            Pair("Diziler", "$mainUrl/kategori/diziler/page/$page/"),
            Pair("Filmler", "$mainUrl/kategori/filmler/page/$page/")
        )

        val lists = categories.map { (name, url) ->
            val document = app.get(url).document
            val items = document.select(".video-item, .post-column").mapNotNull {
                it.toSearchResult()
            }
            HomePageList(name, items)
        }
        return HomePageResponse(lists)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".video-title, .entry-title")?.text() ?: return null
        val href = this.selectFirst("a")?.attr("abs:href") ?: return null
        val posterUrl = this.selectFirst("img")?.attr("abs:src")
        
        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val document = app.get(url).document
        return document.select(".video-item, .post-column").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst(".video-title, .entry-title")?.text() ?: return null
        val poster = document.selectFirst("meta[property=\"og:image\"]")?.attr("content")
        val description = document.selectFirst(".entry-content, .video-details, .post-content")?.text()

        val episodes = document.select(".video-episodes a, .episode-list a").map {
            Episode(
                data = it.attr("abs:href"),
                name = it.text().trim()
            )
        }

        return if (episodes.isEmpty()) {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
            }
        } else {
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
        val iframes = document.select("iframe[src*='/player/'], iframe[src*='vidmoly.to'], iframe[src*='ok.ru'], iframe[src*='vk.com']")
        
        iframes.forEach { iframe ->
            val src = iframe.attr("abs:src")
            loadExtractor(src, subtitleCallback, callback)
        }
        
        return true
    }
}