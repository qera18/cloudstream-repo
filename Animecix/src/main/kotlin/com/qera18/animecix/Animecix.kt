package com.qera18.animecix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.network.*
import com.lagradost.cloudstream3.utils.*
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.net.URLEncoder

@CloudstreamPlugin
class Animecix : MainAPI() {
    override var mainUrl = "https://animecix.tv"
    override var name = "Animecix"
    override var lang = "en"
    override val supportedTypes = setOf(
        TvType.Anime,
        TvType.TvSeries,
        TvType.Movie,
        TvType.OVA,
        TvType.ONA,
        TvType.Special
    )
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val hasSearch = true
    override val hasLoad = true
    override val hasLoadLinks = true
    override val needsHeaders = true
    override val headers: Map<String, String> = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    )

    companion object {
        const val VERSION = "1.0"
    }

    private fun fixUrl(url: String): String {
        return if (url.startsWith("http")) url else "$mainUrl${if (url.startsWith("/")) "" else "/"}$url"
    }

    private fun getQualityFromUrl(url: String): Int {
        return when {
            "1080" in url -> Qualities.P1080
            "720" in url -> Qualities.P720
            "480" in url -> Qualities.P480
            else -> Qualities.Unknown
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): List<MainPageResponse> {
        val doc = app.get(mainUrl, headers = headers).document
        val results = mutableListOf<MainPageResponse>()
        doc.select("div.film-poster, .film-poster, .posters .item").forEach { element ->
            val a = element.selectFirst("a") ?: return@forEach
            val href = a.attr("href")?.let { fixUrl(it) } ?: return@forEach
            val title = a.attr("title").ifBlank { a.text() }.trim()
            val img = element.selectFirst("img")?.attr("src")?.let { fixUrl(it) } ?: ""
            results.add(MainPageResponse(title, href, img))
        }
        return results
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search?keyword=${URLEncoder.encode(query, "UTF-8")}"
        val doc = app.get(url, headers = headers).document
        val results = mutableListOf<SearchResponse>()
        doc.select("div.film-poster, .film-poster, .posters .item").forEach { element ->
            val a = element.select