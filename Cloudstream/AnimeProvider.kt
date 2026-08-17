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
    private val apiUrl = "https://api.animecix.tv"
    override val supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie, TvType.Movie, TvType.TvSeries)
    override var lang = "tr"

    companion object {
        val supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie, TvType.Movie, TvType.TvSeries)

        data class SearchResult(
            val name: String?,
            val slug: String?,
            val poster: String?,
            val type: String?
        )

        data class SearchData(
            val data: List<SearchResult>?
        )

        data class TitleResponse(
            val name: String?,
            val description: String?,
            val poster: String?,
            val banner: String?,
            val type: String?,
            val release_date: String?,
            val seasons: List<SeasonData>?,
            val trailer: String?
        )

        data class SeasonData(
            val name: String?,
            val episodes: List<EpisodeData>?
        )

        data class EpisodeData(
            val id: Int,
            val name: String?,
            val number: String?
        )

        data class WatchResponse(
            val video: List<VideoSource>?,
            val subtitle: List<SubtitleSource>?
        )

        data class VideoSource(
            val url: String?,
            val name: String?
        )

        data class SubtitleSource(
            val url: String?,
            val lang: String?
        )
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val items = mutableListOf<HomePageList>()
        val categories = listOf(
            Pair("Son Eklenenler", "$apiUrl/titles/latest"),
            Pair("Popüler Animeler", "$apiUrl/titles/popular?type=anime"),
            Pair("Popüler Diziler", "$apiUrl/titles/popular?type=tv")
        )

        for (cat in categories) {
            val response = app.get(cat.second).parsedSafe<SearchData>()
            val searchResponses = response?.data?.mapNotNull {
                newAnimeSearchResponse(it.name ?: return@mapNotNull null, "$mainUrl/anime/${it.slug}", TvType.Anime) {
                    posterUrl = it.poster
                }
            }
            if (!searchResponses.isNullOrEmpty()) {
                items.add(HomePageList(cat.first, searchResponses))
            }
        }

        return HomePageResponse(items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val response = app.get("$apiUrl/search?query=$query").parsedSafe<SearchData>()
        return response?.data?.mapNotNull {
            newAnimeSearchResponse(it.name ?: return@mapNotNull null, "$mainUrl/anime/${it.slug}", TvType.Anime) {
                posterUrl = it.poster
            }
        } ?: emptyList()
    }

    override suspend fun load(url: String): LoadResponse? {
        val slug = url.split("/").last()
        val titleData = app.get("$apiUrl/titles/$slug").parsedSafe<TitleResponse>() ?: return null
        
        val type = if (titleData.type?.contains("movie", ignoreCase = true) == true) TvType.AnimeMovie else TvType.Anime
        val episodes = mutableListOf<Episode>()
        
        titleData.seasons?.forEachIndexed { seasonIdx, season ->
            season.episodes?.forEach { ep ->
                episodes.add(
                    Episode(
                        data = ep.id.toString(),
                        name = ep.name,
                        episode = ep.number?.toIntOrNull(),
                        season = seasonIdx + 1
                    )
                )
            }
        }

        val loadResponse = if (type == TvType.AnimeMovie) {
            newMovieLoadResponse(titleData.name ?: "", url, type, episodes.firstOrNull()?.data ?: "") {
                posterUrl = titleData.poster
                plot = titleData.description
                year = titleData.release_date?.split("-")?.firstOrNull()?.toIntOrNull()
            }
        } else {
            newTvSeriesLoadResponse(titleData.name ?: "", url, type, episodes) {
                posterUrl = titleData.poster
                plot = titleData.description
                year = titleData.release_date?.split("-")?.firstOrNull()?.toIntOrNull()
            }
        }

        titleData.trailer?.let { loadResponse.addTrailer(it) }
        return loadResponse
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val watchData = app.get("$apiUrl/watch/$data").parsedSafe<WatchResponse>()
        
        watchData?.subtitle?.forEach { sub ->
            subtitleCallback(SubtitleFile(sub.lang ?: "Turkish", sub.url ?: return@forEach))
        }

        watchData?.video?.forEach { video ->
            val videoUrl = video.url ?: return@forEach
            if (videoUrl.contains("animecix.tv/player")) {
                loadExtractor(videoUrl, "$mainUrl/", subtitleCallback, callback)
            } else {
                callback(
                    ExtractorLink(
                        video.name ?: "Animecix",
                        video.name ?: "Animecix",
                        videoUrl,
                        "$mainUrl/",
                        Qualities.Unknown.value,
                        isM3u8 = videoUrl.contains(".m3u8")
                    )
                )
            }
        }
        
        return true
    }
}