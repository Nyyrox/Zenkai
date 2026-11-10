package com.nyyrox.aniverse.data.movie

data class MovieItem(
    val id: Int,
    val type: String, // "movie" or "tv"
    val title: String,
    val poster: String?,
    val backdrop: String?,
    val year: String?,
    val rating: Double?,
    val overview: String,
    val genreIds: List<Int> = emptyList()
) {
    val posterUrl: String?
        get() = poster?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w500$it" }

    val backdropUrl: String?
        get() = backdrop?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w1280$it" }
}

data class MovieGenre(
    val id: Int,
    val name: String
)

data class MovieCast(
    val id: Int,
    val name: String,
    val character: String,
    val photo: String?
) {
    val photoUrl: String?
        get() = photo?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w185$it" }
}

data class MovieSeason(
    val seasonNumber: Int,
    val name: String,
    val episodeCount: Int
)

data class MovieEpisode(
    val episodeNumber: Int,
    val name: String,
    val overview: String,
    val still: String?,
    val runtime: Int?,
    val airDate: String?
) {
    val stillUrl: String?
        get() = still?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w500$it" }
}

data class MovieDetails(
    val id: Int,
    val type: String, // "movie" or "tv"
    val title: String,
    val poster: String?,
    val backdrop: String?,
    val year: String?,
    val rating: Double?,
    val overview: String,
    val tagline: String?,
    val runtime: Int?,
    val status: String?,
    val genres: List<MovieGenre> = emptyList(),
    val logo: String? = null,
    val trailerKey: String? = null,
    val seasons: List<MovieSeason> = emptyList(),
    val cast: List<MovieCast> = emptyList(),
    val similar: List<MovieItem> = emptyList(),
    val imdbId: String? = null
) {
    val posterUrl: String?
        get() = poster?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w500$it" }

    val backdropUrl: String?
        get() = backdrop?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w1280$it" }

    val logoUrl: String?
        get() = logo?.let { if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w500$it" }
}

data class MovieDub(
    val id: String,
    val label: String,
    val selected: Boolean = false
)

data class MovieStream(
    val quality: String,
    val resolutionNum: Int? = null,
    val format: String = "mp4",
    val size: Long? = null,
    val sizeLabel: String? = null,
    val duration: Double? = null,
    val streamUrl: String,
    val downloadUrl: String,
    val downloadFileName: String? = null
)

data class MovieSubtitle(
    val id: String,
    val label: String,
    val language: String,
    val url: String
)

data class MovieStreamResult(
    val success: Boolean,
    val server: String,
    val title: String,
    val streamUrl: String,
    val downloadUrl: String? = null,
    val quality: String,
    val format: String = "mp4",
    val duration: Double? = null,
    val dubs: List<MovieDub> = emptyList(),
    val streams: List<MovieStream> = emptyList(),
    val subtitles: List<MovieSubtitle> = emptyList(),
    val error: String? = null
)
