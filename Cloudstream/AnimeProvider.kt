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
    override var name = "Anime"
    override var mainUrl = "https://cizgimax.online"
    override var lang = "tr"
    override val hasMainPage = true

    override val supportedTypes = setOf(
        TvType.Anime,
        TvType.TvSeries,
        TvType.Movie,
        TvType.Cartoon
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val items = mutableListOf<HomePageList>()
        val soup = app.get(mainUrl).document

        val sections = listOf(
            "Son Eklenenler" to "div.items article.item",
            "Popüler Diziler" to "div.featured-list div.item",
            "Yeni Bölümler" to ".episodes-list article.item"
        )

        sections.forEach { (title, selector) ->
            val elements = soup.select(selector)
            if (elements.isNotEmpty()) {
                val homeItems = elements.mapNotNull { it.toSearchResult() }
                items.add(HomePageList(title, homeItems))
            }
        }

        return HomePageResponse(items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".data h3 a, .entry-title a, h3")?.text() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src")

        return if (href.contains("/bolum/") || href.contains("-bolum")) {
            newAnimeSearchResponse(title, href, TvType.Anime) {
                this.posterUrl = posterUrl
            }
        } else {
            newAnimeSearchResponse(title, href, TvType.Anime) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val soup = app.get(url).document
        return soup.select("div.result-item article, article.item").mapNotNull {
            val title = it.selectFirst(".title a")?.text() ?: it.selectFirst(".data h3 a")?.text() ?: return@mapNotNull null
            val href = it.selectFirst(".title a")?.attr("href") ?: it.selectFirst(".data h3 a")?.attr("href") ?: return@mapNotNull null
            val posterUrl = it.selectFirst("img")?.attr("src")
            
            newAnimeSearchResponse(title, href, TvType.Anime) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document
        val title = doc.selectFirst(".data h1")?.text() ?: doc.selectFirst(".entry-title")?.text() ?: return null
        val poster = doc.selectFirst(".poster img")?.attr("src")
        val description = doc.selectFirst(".wp-content p, .description p")?.text()
        val year = doc.selectFirst(".date")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        
        val isMovie = url.contains("/film/") || doc.select("#episodios").isEmpty()

        if (isMovie) {
            return newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
            }
        } else {
            val episodes = mutableListOf<Episode>()
            doc.select(".se-c").forEach { seasonElement ->
                val seasonNum = seasonElement.selectFirst(".title")?.text()?.filter { it.isDigit() }?.toIntOrNull() ?: 1
                seasonElement.select("ul.episodios li").forEach { epElement ->
                    val epTitle = epElement.selectFirst(".episodiotitle a")?.text()
                    val epHref = epElement.selectFirst(".episodiotitle a")?.attr("href") ?: return@forEach
                    val epNum = epElement.selectFirst(".numerando")?.text()?.split("-")?.lastOrNull()?.trim()?.toIntOrNull()
                    
                    episodes.add(Episode(
                        data = epHref,
                        name = epTitle,
                        episode = epNum,
                        season = seasonNum
                    ))
                }
            }
            
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        
        doc.select("ul#playeroptionsul li").forEach { option ->
            val type = option.attr("data-type")
            val post = option.attr("data-post")
            val nume = option.attr("data-nume")
            
            if (post.isNotEmpty() && nume.isNotEmpty()) {
                val response = app.post(
                    url = "$mainUrl/wp-admin/admin-ajax.php",
                    data = mapOf(
                        "action" to "doo_player_ajax",
                        "post" to post,
                        "nume" to nume,
                        "type" to type
                    ),
                    referer = data,
                    headers = mapOf("X-Requested-With" to "XMLHttpRequest")
                ).text
                
                val embedUrl = Jsoup.parse(response).selectFirst("iframe")?.attr("src")
                if (!embedUrl.isNullOrEmpty()) {
                    loadExtractor(embedUrl, subtitleCallback, callback)
                }
            }
        }
        
        doc.select("iframe[src*='vidmoly'], iframe[src*='ok.ru'], iframe[src*='cizgimax.online/embed/']").forEach { iframe ->
            loadExtractor(iframe.attr("src"), subtitleCallback, callback)
        }

        return true
    }
}