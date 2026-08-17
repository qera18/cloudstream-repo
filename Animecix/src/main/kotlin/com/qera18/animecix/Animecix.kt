package com.qera18.animecix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.network.*
import com.lagradost.cloudstream3.utils.*
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

@CloudstreamPlugin
class Animecix : MainAPI() {
    override var mainUrl = "https://animecix.tv"
    override var name = "Animecix"
    override var lang = "en"
    override val supportedTypes = setOf("tvSeries", "anime", "ova", "ona", "special")
    override val hasMainPage = true
    override val hasSearch = true
    override val hasQuickSearch = false
    override val hasDownloadSupport = false
    override val hasMetaData = true
    override val needsReferer = true

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0 Safari/537.36"

    override fun getHeaders(url: String): Map<String, String> {
        return mapOf(
            "User-Agent" to userAgent,
            "Referer" to mainUrl
        )
    }

    // Home page
    override suspend fun home(): List<HomePageList> {
        val doc = app.get(mainUrl).document
        val sections = mutableListOf<HomePageList>()

        // Latest Updates
        val latest = doc.select("section.latest-anime div.anime-item")
        val latestList = latest.mapNotNull { it.toSearchResponse() }
        if (latestList.isNotEmpty()) {
            sections.add(HomePageList("Latest Updates", latestList))
        }

        // Popular
        val popular = doc.select("section.popular-anime div.anime-item")
        val popularList = popular.mapNotNull { it.toSearchResponse() }
        if (popularList.isNotEmpty()) {
            sections.add(HomePageList("Popular", popularList))
        }

        return sections
    }

    // Search
    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search?keyword=${query.replace(" ", "+")}"
        val doc = app.get(url).document
        return doc.select("div.anime-item").mapNotNull { it.toSearchResponse() }
    }

    // Load details
    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document

        val title = doc.selectFirst("h1.title")?.text()?.trim() ?: return null