package com.example.film

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.MainAPI
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.network.*

class Film : MainAPI() {
    override var name = "Film"
    override var mainUrl = "https://filmzal.me/"
    override var lang = "tr"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val doc = app.get(url).document
        return doc.select("article").mapNotNull { element ->
            val title = element.selectFirst("h2")?.text() ?: return@mapNotNull null
            val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = element.selectFirst("img")?.attr("data-src")
            newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = poster }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1")?.text() ?: ""
        val poster = doc.selectFirst(".entry-content img")?.attr("src")
        val description = doc.selectFirst(".entry-content p")?.text()
        
        val episodes = doc.select(".episodes-list a").mapIndexed { index, el ->
            newEpisode(el.attr("href")) { this.name = el.text(); this.episode = index + 1 }
        }
        
        return if (episodes.isNotEmpty()) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) { this.posterUrl = poster; this.plot = description }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) { this.posterUrl = poster; this.plot = description }
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val doc = app.get(data).document
        val iframe = doc.selectFirst("iframe")?.attr("src") ?: return false
        loadExtractor(iframe, mainUrl, subtitleCallback, callback)
        return true
    }
}
