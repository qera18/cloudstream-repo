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

class BelgeselProvider : MainAPI() {
    override var mainUrl = "https://belgeselx.com"
    override var name = "Belgesel"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Documentary)
    override var lang = "tr"
    override val hasMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val url = if (page <= 1) mainUrl else "$mainUrl/page/$page/"
        val document = app.get(url).document
        val home = document.select("article.item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div.data h3 a, h2.entry-title a")?.text() ?: return null
        val href = this.selectFirst("div.data h3 a, h2.entry-title a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("div.poster img")?.attr("data-src")
            ?: this.selectFirst("div.poster img")?.attr("src")

        return MovieSearchResponse(
            title,
            href,
            this@BelgeselProvider.name,
            TvType.Movie,
            posterUrl,
            null
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val document = app.get(url).document
        return document.select("article.item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("div.data h1")?.text() 
            ?: document.selectFirst("h1.entry-title")?.text() 
            ?: return null
        val poster = document.selectFirst("div.poster img")?.attr("src")
            ?: document.selectFirst("div.poster img")?.attr("data-src")
        val description = document.selectFirst("div.wp-content p")?.text()
        val year = document.selectFirst("span.date")?.text()?.take(4)?.toIntOrNull()
        
        val tvType = if (document.selectFirst("ul.episodios") != null) TvType.TvSeries else TvType.Movie
        val trailer = document.selectFirst("iframe[src*='youtube']")?.attr("src")

        if (tvType == TvType.TvSeries) {
            val episodes = document.select("ul.episodios li").mapNotNull {
                val epTitle = it.selectFirst("div.episodiotitle a")?.text() ?: return@mapNotNull null
                val epHref = it.selectFirst("div.episodiotitle a")?.attr("href") ?: return@mapNotNull null
                val seasonNum = it.selectFirst("div.numerando")?.text()?.split("-")?.firstOrNull()?.trim()?.toIntOrNull()
                val episodeNum = it.selectFirst("div.numerando")?.text()?.split("-")?.lastOrNull()?.trim()?.toIntOrNull()
                Episode(epHref, epTitle, seasonNum, episodeNum)
            }
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                addTrailer(trailer)
            }
        } else {
            return newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
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
        
        val iframes = document.select("iframe[src*='embed'], iframe[src*='player'], .video-responsive iframe")
        iframes.forEach {
            val src = it.attr("src")
            if (src.isNotEmpty()) {
                val finalSrc = if (src.startsWith("//")) "https:$src" else src
                loadExtractor(finalSrc, subtitleCallback, callback)
            }
        }

        document.select("nav.player-nav ul li").forEach {
            val postId = it.attr("data-post")
            val type = it.attr("data-type")
            val nume = it.attr("data-nume")

            if (postId.isNotEmpty()) {
                val ajaxUrl = "$mainUrl/wp-admin/admin-ajax.php"
                val response = app.post(
                    ajaxUrl,
                    data = mapOf(
                        "action" to "doo_player_ajax",
                        "post" to postId,
                        "nume" to nume,
                        "type" to type
                    ),
                    referer = data,
                    headers = mapOf("X-Requested-With" to "XMLHttpRequest")
                ).document
                
                val embedUrl = response.selectFirst("iframe")?.attr("src")
                if (!embedUrl.isNullOrEmpty()) {
                    val finalEmbedUrl = if (embedUrl.startsWith("//")) "https:$embedUrl" else embedUrl
                    loadExtractor(finalEmbedUrl, subtitleCallback, callback)
                }
            }
        }

        return true
    }
}