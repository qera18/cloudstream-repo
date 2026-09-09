package com.qera18.animecix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.network.*
import com.lagradost.cloudstream3.utils.*
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

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
    override val hasDownloadSupport = false
    override val hasQuickSearch = false

    override fun getHeaders(url: String): Map<String, String> {
        return mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0 Safari/537.36",
            "Referer" to mainUrl
        )