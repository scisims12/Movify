package com.ivor.movify.domain.model

/** The curated lists shown on Home: everything by default, with dedicated anime rows. */
enum class AnimeCatalog {
    /** Movies and series trending on TMDB this week. */
    TRENDING,

    /** Latest and popular Hindi movies. */
    HINDI_MOVIES,

    /** Latest and popular Hindi series. */
    HINDI_SERIES,

    /** Popular content in India. */
    POPULAR_IN_INDIA,

    /** Series with new episodes airing this week. */
    NEW_EPISODES,

    POPULAR_MOVIES,
    POPULAR_SERIES,

    /** Highly rated films with enough votes to trust the score. */
    TOP_RATED_MOVIES,

    /** Trending series narrowed to Japanese animation. */
    TRENDING_ANIME,

    ANIME_MOVIES
}
