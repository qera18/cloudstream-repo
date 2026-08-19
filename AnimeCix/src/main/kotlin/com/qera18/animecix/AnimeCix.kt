package com.qera18.animecix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.network.*
import com.lagradost.cloudstream3.utils.*
import kotlinx.coroutines.*
import org.jsoup.Jsoup

@CloudstreamPlugin
class AnimeCix : MainAPI() {
    override var mainUrl = "https://animecix.tv"
    override var name = "AnimeCix"
    override var hasMainPage = true
    override var lang = "en"
    override var supportedTypes = setOf(
        TvType.Anime,
        TvType.TvSeries,
        TvType.OVA,
        TvType.ONA,
        TvType.Special
    )
    override var needsHeaders = true
    override var headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36",
        "Referer" to mainUrl
    )

    companion object {
        const val PLUGIN_NAME = "AnimeCix"
        const val PLUGIN_VERSION = "1.0"
    }

    // Home page
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get(mainUrl).document
        val items = doc.select("div.item, .movie-item, .film-item").mapNotNull { element ->
            val titleElem = element.selectFirst("h2.title, .movie-title")
            val linkElem = element.selectFirst("a[href]")
            val imgElem = element.selectFirst("img[src]")
            if (titleElem != null && linkElem != null) {
                val title = titleElem.text().trim()
                val url = linkElem.absUrl("href")
                val img = imgElem?.absUrl("src")
                SearchResponse(
                    name = title,
                    url = url,
                    apiName = this.name,
                    posterUrl = img
                )
            } else null
        }
        return HomePageResponse(listOf(HomePageList("Latest", items)))
    }

    // Search
    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/search?keyword=${query.replace(" ", "+")}"
        val doc = app.get(searchUrl).document
        return doc.select("div.item, .movie-item, .film-item").mapNotNull { element ->
            val titleElem = element.selectFirst("h2.title, .movie-title")
            val linkElem = element.selectFirst("a[href]")
            val imgElem = element.selectFirst("img[src]")
            if (titleElem != null && linkElem != null) {
                val title = titleElem.text().trim()
                val url = linkElem.absUrl("href")
                val img = imgElem?.absUrl("src")
                SearchResponse(
                    name = title,
                    url = url,
                    apiName = this.name,
                    posterUrl = img
                )
            } else null
        }
    }

    // Load details and episodes
    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document

        val title = doc.selectFirst("h1.title, .movie-title")?.text()?.trim() ?: "AnimeCix"
        val poster = doc.selectFirst("