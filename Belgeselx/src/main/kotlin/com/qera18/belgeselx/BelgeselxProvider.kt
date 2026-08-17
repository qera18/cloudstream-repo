package com.qera18.belgeselx

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities

class BelgeselxProvider : MainAPI() {
    override val mainUrl = "https://belgeselx.com"
    override val name = "Belgeselx"
    override val lang = "tr"
    override val hasMainPage = true

    override fun getMainPage(): HomePageResponse {
        val document = app.get(mainUrl).document
        val home = document.select("div.dizi-kutu").map { it.toSearchResult() }
        return newHomePageResponse(name, home)
    }

    override fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val document = app.get(url).document
        return document.select("div.dizi-kutu").map { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse {
        val title = this.selectFirst("h2")!!.text().trim()
        val link = this.selectFirst("a")!!.attr("href")
        val image = this.selectFirst("img")!!.attr("src")
        return newMovieSearchResponse(title, link, image)
    }

    override fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1")!!.text().trim()
        val description = document.selectFirst("div.konu-ozeti")!!.text().trim()
        val poster = document.selectFirst("img.dizi-resmi")!!.attr("src")
        val tags = document.select("div.konu-turleri").map { it.text().trim() }
        val recommendations = document.select("div.dizi-kutu").map { it.toSearchResult() }
        return newMovieLoadResponse(title, url, description, poster, tags, recommendations)
    }

    override fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val document = app.get(data).document
        val links = document.select("div.bolum-linkleri").map { link ->
            val url = link.selectFirst("a")!!.attr("href")
            val name = link.selectFirst("a")!!.text().trim()
            ExtractorLink(
                name,
                url,
                "",
                Qualities.Unknown.value,
                false
            )
        }
        links.forEach(callback)
    }
}