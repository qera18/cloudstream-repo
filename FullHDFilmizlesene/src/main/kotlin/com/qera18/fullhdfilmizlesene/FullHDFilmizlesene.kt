package com.qera18.fullhdfilmizlesene

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.network.*
import com.lagradost.cloudstream3.utils.*
import kotlinx.coroutines.*
import org.jsoup.Jsoup

@CloudstreamPlugin
class FullHDFilmizlesene : MainAPI() {
    companion object {
        const val NAME = "FullHDFilmizlesene"
        const val MAIN_URL = "https://www.fullhdfilmizlesene.now"
    }

    override var name = NAME
    override var mainUrl = MAIN_URL
    override var hasMainPage = true
    override var hasSearch = true
    override var supportedTypes = setOf(TvType.Movie)
    override var lang = "tr"

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val response = app.get(mainUrl).text
        val doc = Jsoup.parse(response)
        val sections = mutableListOf<HomePageList>()

        // Generic parsing: each <section> with a <h2> title and links to movies
        doc.select("section").forEach { sec ->
            val title = sec.selectFirst("h2")?.text()?.trim() ?: return@forEach
            val items = sec.select("a[href^='/film/']").mapNotNull { a ->
                val href = a.absUrl("href")