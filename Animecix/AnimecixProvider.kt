package com.qera18.animecix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class AnimecixProvider : MainAPI() {
    override var mainUrl = "https://animecix.tv"
    override var name = "Animecix"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Anime, TvType.OVA)

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse? {
        val document = app.get(mainUrl).document
        val items = document.select("div.item")
        
        val homeItems = items.mapNotNull { item ->
            val title = item.selectFirst("h2.title")?.text() ?: return@mapNotNull null
            val href = item.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val posterUrl = item.selectFirst("img")?.attr("src")
            
            newAnimeSearchResponse(title, fixUrl(href), TvType.Anime) {
                this.posterUrl = fixUrlNull(posterUrl)
            }
        }
        
        return newHomePageResponse(
            listOf(HomePageList("Ana Sayfa", homeItems)),
            hasNext = false
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search?q=$query"
        val document = app.get(url).document
        val items = document.select("div.item")
        
        return items.mapNotNull { item ->
            val title = item.selectFirst("h2.title")?.text() ?: return@mapNotNull null
            val href = item.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val posterUrl = item.selectFirst("img")?.attr("src")
            
            newAnimeSearchResponse(title, fixUrl(href), TvType.Anime) {
                this.posterUrl = fixUrlNull(posterUrl)
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1.title")?.text() ?: return null
        val posterUrl = document.selectFirst("div.poster img")?.attr("src")
        val plot = document.selectFirst("div.description")?.text()

        return newAnimeLoadResponse(title, url, TvType.Anime) {
            this.posterUrl = fixUrlNull(posterUrl)
            this.plot = plot
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCdn: Boolean,
        handler: PlaylistUtils,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val iframes = document.select("div.player iframe")
        
        iframes.forEach { iframe ->
            val src = iframe.attr("src")
            if (src.isNotEmpty()) {
                callback(
                    ExtractorLink(
                        name = this.name,
                        source = this.name,
                        url = fixUrl(src),
                        referer = mainUrl,
                        quality = Qualities.P1080.value
                    )
                )
            }
        }
        return true
    }
}
