package com.lagradost.cloudstream3

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import org.jsoup.nodes.Element
import org.jsoup.Jsoup

class AnimeProvider : MainAPI() {
    override var name = "Anime"
    override var mainUrl = "https://animecix.tv"
    override var lang = "tr"
    override val hasMainPage = true

    override val supportedTypes = setOf(
        TvType.Anime,
        TvType.AnimeMovie,
        TvType.TvSeries,
        TvType.Movie
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse? {
        val document = app.get(mainUrl).document
        val homePages = mutableListOf<HomePageList>()

        val sections = listOf(
            Pair("Son Eklenenler", "app-anime-card"),
            Pair("Popüler", ".card")
        )

        sections.forEach { (title, selector) ->
            val items = document.select(selector).mapNotNull {
                it.toSearchResult()
            }
            if (items.isNotEmpty()) {
                homePages.add(HomePageList(title, items))
            }
        }

        return HomePageResponse(homePages)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".card-title, .title, h5")?.text() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.attr("src")

        return newAnimeSearchResponse(title, fixUrl(href)) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search?query=$query"
        val response = app.get(url).document

        return response.select("app-anime-card, .card").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1, .anime-title")?.text() ?: ""
        val poster = document.selectFirst(".poster img, img.anime-image")?.attr("src")
        val description = document.selectFirst(".description, .synopsis")?.text()
        val year = document.selectFirst(".year, .date")?.text()?.toIntOrNull()
        val trailer = document.selectFirst("iframe[src*=youtube]")?.attr("src")

        val type = if (url.contains("movie")) TvType.AnimeMovie else TvType.Anime

        if (type == TvType.AnimeMovie) {
            return newMovieLoadResponse(title, url, TvType.AnimeMovie, url) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                addTrailer(trailer)
            }
        }

        val episodes = document.select(".episode-list a, .episodes a").mapNotNull {
            val epName = it.text()
            val epHref = it.attr("href")
            val epNum = epName.filter { char -> char.isDigit() }.toIntOrNull()
            Episode(fixUrl(epHref), epName, episode = epNum)
        }

        return newTvSeriesLoadResponse(title, url, TvType.Anime, episodes) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            addTrailer(trailer)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        // Extraction logic for internal player and HLS sources
        // Often found in script tags or iframe sources in Animecix
        document.select("iframe").forEach { iframe ->
            val src = fixUrl(iframe.attr("src"))
            if (src.contains("m3u8") || src.contains("playlist")) {
                callback.invoke(
                    ExtractorLink(
                        name,
                        "HLS Source",
                        src,
                        referer = mainUrl,
                        quality = Qualities.Unknown.value,
                        isM3u8 = true
                    )
                )
            }
            loadExtractor(src, mainUrl, subtitleCallback, callback)
        }

        // Direct stream search in scripts
        val scripts = document.select("script").map { it.data() }
        scripts.forEach { script ->
            val m3u8Regex = """["'](http[^"']+\.m3u8[^"']*)["']""".toRegex()
            m3u8Regex.findAll(script).forEach { match ->
                val videoUrl = match.groupValues[1]
                callback.invoke(
                    ExtractorLink(
                        name,
                        "Internal HLS",
                        videoUrl,
                        referer = mainUrl,
                        quality = Qualities.Unknown.value,
                        isM3u8 = true
                    )
                )
            }
        }

        return true
    }
}