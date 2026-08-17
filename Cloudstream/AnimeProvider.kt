package com.lagradost.cloudstream3

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import org.jsoup.nodes.Element
import org.jsoup.Jsoup

class AnimeProvider : MainAPI() {
    override var mainUrl = "https://animecix.tv"
    override var name = "Anime"
    override val hasMainPage = true
    override var lang = "tr"
    override val supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie, TvType.TvSeries)

    private val apiUrl = "https://api.animecix.net"

    companion object {
        fun getType(type: String?): TvType {
            return when (type?.lowercase()) {
                "movie", "film" -> TvType.AnimeMovie
                "tv", "series" -> TvType.Anime
                else -> TvType.Anime
            }
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val items = mutableListOf<HomePageList>()
        val urls = listOf(
            Pair("Son Eklenenler", "$apiUrl/home"),
            Pair("Popüler", "$apiUrl/home"),
            Pair("Yeni Bölümler", "$apiUrl/home")
        )

        urls.forEach { (title, url) ->
            val response = app.get(url).parsedSafe<HomeResponse>()
            val animeList = response?.data?.new_animes?.map {
                newAnimeSearchResponse(it.name ?: "", it.slug ?: "", TvType.Anime) {
                    this.posterUrl = it.poster_url
                }
            }
            if (!animeList.isNullOrEmpty()) {
                items.add(HomePageList(title, animeList))
            }
        }

        return HomePageResponse(items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val response = app.get("$apiUrl/search?q=$query").parsedSafe<SearchData>()
        return response?.data?.map {
            newAnimeSearchResponse(it.name ?: "", it.slug ?: "", getType(it.type)) {
                this.posterUrl = it.poster_url
            }
        } ?: emptyList()
    }

    override suspend fun load(url: String): LoadResponse? {
        val slug = url.split("/").last()
        val details = app.get("$apiUrl/anime-detail/$slug").parsedSafe<AnimeDetail>() ?: return null
        
        val title = details.data?.name ?: ""
        val poster = details.data?.poster_url
        val description = details.data?.description
        val type = getType(details.data?.type)
        val year = details.data?.year?.toIntOrNull()
        val status = when (details.data?.status) {
            "1" -> ShowStatus.Ongoing
            "2" -> ShowStatus.Completed
            else -> null
        }

        val episodes = details.data?.episodes?.map { ep ->
            val epNum = ep.number?.toIntOrNull() ?: 1
            val epName = ep.name ?: "Bölüm $epNum"
            Episode(
                data = ep.id.toString(),
                name = epName,
                episode = epNum,
                season = 1,
                posterUrl = poster
            )
        }?.sortedBy { it.episode } ?: emptyList()

        return newAnimeLoadResponse(title, url, type) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.showStatus = status
            this.addEpisodes(TvType.Anime, episodes)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val episodeId = data
        val videoSources = app.get("$apiUrl/episode-videos/$episodeId").parsedSafe<VideoSources>()

        videoSources?.data?.forEach { source ->
            val videoUrl = source.url ?: return@forEach
            val label = source.name ?: "Source"

            if (videoUrl.contains("tau-player") || videoUrl.contains("anm.cx")) {
                val directData = app.get(videoUrl).text
                // Tau-player logic extraction would go here, often uses internal API or base64
                loadExtractor(videoUrl, subtitleCallback, callback)
            } else {
                loadExtractor(videoUrl, subtitleCallback, callback)
            }
        }

        return true
    }

    data class HomeResponse(val data: HomeData?)
    data class HomeData(val new_animes: List<AnimeItem>?)
    data class SearchData(val data: List<AnimeItem>?)
    data class AnimeItem(
        val name: String?,
        val slug: String?,
        val poster_url: String?,
        val type: String?
    )

    data class AnimeDetail(val data: DetailData?)
    data class DetailData(
        val name: String?,
        val description: String?,
        val poster_url: String?,
        val type: String?,
        val year: String?,
        val status: String?,
        val episodes: List<EpisodeItem>?
    )

    data class EpisodeItem(
        val id: Int?,
        val name: String?,
        val number: String?
    )

    data class VideoSources(val data: List<VideoSourceItem>?)
    data class VideoSourceItem(
        val name: String?,
        val url: String?
    )
}