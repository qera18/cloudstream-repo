package com.qera18.animecixprovider

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import org.jsoup.nodes.Element

class AnimecixProvider : MainAPI() {
    override var mainUrl = "https://animecix.tv"
    override var name = "Animecix"
    override val supportedTypes = setOf(TvType.Anime, TvType.Movie)
    override var lang = "tr"
    override val hasMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val items = document.select("div.item, div.anime-card").mapNotNull { it.toSearchResponse() }
        return HomePageResponse(listOf(HomePageList("Ana Sayfa", items, false)), false)
    }

    private fun Element.toSearchResponse(): SearchResponse? {
        val title = this.selectFirst("a.title, h3")?.text() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href") ?: return null)
        val poster = this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src")
        return newMovieSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = poster
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/search/$query").document
        return document.select("div.search-result, div.item").mapNotNull { it.toSearchResponse() }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1.title, .anime-title")?.text() ?: "Animecix"
        val poster = document.selectFirst("div.poster img")?.attr("src")
        val description = document.selectFirst("div.description, .anime-desc")?.text()
        val episodes = document.select("ul.episode-list li a").mapNotNull {
            val epName = it.text()
            val epUrl = fixUrl(it.attr("href"))
            Episode(epUrl, epName)
        }
        return if (episodes.isEmpty()) {
            newMovieLoadResponse(title, url, TvType.Anime, url) {
                this.posterUrl = poster
                this.plot = description
            }
        } else {
            newTvSeriesLoadResponse(title, url, TvType.Anime, episodes) {
                this.posterUrl = poster
                this.plot = description
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: SubtitleCallback,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        loadExtractor(data, "$mainUrl/", subtitleCallback, callback)
        return true
    }
}