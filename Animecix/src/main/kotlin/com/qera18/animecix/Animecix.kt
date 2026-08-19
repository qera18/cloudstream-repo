package com.qera18.animecix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.network.*
import com.lagradost.cloudstream3.utils.*
import kotlinx.coroutines.*
import org.jsoup.Jsoup

@CloudstreamPlugin
class Animecix : MainAPI() {
    override var name = "Animecix"
    override var mainUrl = "https://animecix.tv"
    override var lang = "en"
    override var hasMainPage = true
    override var supportedTypes = setOf(
        TvType.Anime,
        TvType.Movie,
        TvType.Ova,
        TvType.OnAnime,
        TvType.Special
    )
    override val mainPage = mainPageOf(
        Pair("Latest", "$mainUrl/latest"),
        Pair("Popular", "$mainUrl/popular")
    )

    companion object {
        const val PLUGIN_NAME = "Animecix"
        const val VERSION = "1.0"
    }

    private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0 Safari/537.36",
        "Referer" to mainUrl
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): List<MainPageResponse> {
        val url = request.data
        val doc = Jsoup.parse(app.get(url, headers = headers).text)