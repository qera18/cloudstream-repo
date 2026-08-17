package com.qera18.animecix

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainAPI.Companion.app
import com.lagradost.cloudstream3.MainAPI.Companion.base64Decode
import com.lagradost.cloudstream3.MainAPI.Companion.getQuality
import com.lagradost.cloudstream3.MainAPI.Companion.newMovieSearchResponse
import com.lagradost.cloudstream3.MainAPI.Companion.newMovieLoadResponse
import com.lagradost.cloudstream3.MainAPI.Companion.newHomePageResponse
import com.lagradost.cloudstream3.MainAPI.Companion.newSearchResponse
import com.lagradost.cloudstream3.MainAPI.Companion.newShowSearchResponse
import com.lagradost.cloudstream3.MainAPI.Companion.resolveUrl
import com.lagradost.cloudstream3.MainAPI.Companion.setSearchResponse
import com.lagradost.cloudstream3.MainAPI.Companion.setSubSearchResponse
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import org.jsoup.nodes.Element
import java.util.*

class AnimecixProvider : MainAPI() {
    override var mainUrl = "https://animecix.tv"
    override var name = "Animecix"
    override val hasMainPage = true
    override val hasChromecastSupport = false
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Anime, TvType.OVA)

    override suspend fun getMainPage(): HomePageResponse {
        val document = app.get(mainUrl).document
        val home = newHomePageResponse()
        document.select("div.post").forEach { post ->
            val title = post.selectFirst("h2.entry-title")?.text() ?: ""
            val link = post.selectFirst("a")?.attr("href") ?: ""
            val image = post.selectFirst("img")?.attr("src") ?: ""
            home.add(newMovieSearchResponse(title, link, image))
        }
        return home
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        val search = newSearchResponse()
        document.select("div.post").forEach { post ->
            val title = post.selectFirst("h2.entry-title")?.text() ?: ""
            val link = post.selectFirst("a")?.attr("href") ?: ""
            val image = post.selectFirst("img")?.attr("src") ?: ""
            search.add(newMovieSearchResponse(title, link, image))
        }
        return search
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1.entry-title")?.text() ?: ""
        val description = document.selectFirst("div.entry-content")?.text() ?: ""
        val poster = document.selectFirst("img.featured-image")?.attr("src") ?: ""
        val load = newMovieLoadResponse(title, url, poster, description)
        document.select("div.episode").forEach { episode ->
            val episodeTitle = episode.selectFirst("a")?.text() ?: ""
            val episodeLink = episode.selectFirst("a")?.attr("href") ?: ""
            load.addEpisode(episodeTitle, episodeLink)
        }
        return load
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val document = app.get(data).document
        val links = document.select("div.download")
        links.forEach { link ->
            val quality = getQuality(link.selectFirst("span.quality")?.text() ?: "")
            val linkUrl = link.selectFirst("a")?.attr("href") ?: ""
            callback(
                ExtractorLink(
                    name,
                    name,
                    linkUrl,
                    "",
                    quality,
                    false
                )
            )
        }
    }
}