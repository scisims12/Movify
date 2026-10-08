package com.ivor.movify.data.streaming

import com.ivor.movify.domain.model.MediaIdentity
import com.ivor.movify.domain.model.StreamQuality
import com.ivor.movify.domain.model.VideoServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentMatchVerifierTest {

    @Test
    fun passesTitleColonVariant() {
        val identity = MediaIdentity(
            tmdbId = 123,
            tmdbType = "movie",
            title = "Drishyam: The Conclusion",
            year = 2022
        )
        val server = createServer(name = "Drishyam The Conclusion", url = "https://example.com/drishyam-the-conclusion-2022.mp4")
        val result = ContentMatchVerifier.verifyServer(identity, server)
        assertEquals(VerificationConfidence.HIGH, result.confidence)
    }

    @Test
    fun passesExactTmdbId() {
        val identity = MediaIdentity(
            tmdbId = 999,
            tmdbType = "movie",
            title = "Some Movie",
            year = 2020
        )
        val server = createServer(name = "Random Stream", url = "https://example.com/tmdb999/video.mp4")
        val result = ContentMatchVerifier.verifyServer(identity, server)
        assertEquals(VerificationConfidence.HIGH, result.confidence)
    }

    @Test
    fun passesExactImdbId() {
        val identity = MediaIdentity(
            tmdbId = 888,
            tmdbType = "movie",
            imdbId = "tt1234567",
            title = "Another Movie",
            year = 2021
        )
        val server = createServer(name = "Stream Source", url = "https://example.com/tt1234567/stream.m3u8")
        val result = ContentMatchVerifier.verifyServer(identity, server)
        assertEquals(VerificationConfidence.HIGH, result.confidence)
    }

    @Test
    fun failsSequelMismatch() {
        val identity = MediaIdentity(
            tmdbId = 100,
            tmdbType = "movie",
            title = "Drishyam",
            year = 2015
        )
        val server = createServer(name = "Drishyam 2 The Resumption", url = "https://example.com/drishyam-2.mp4")
        val result = ContentMatchVerifier.verifyServer(identity, server)
        assertEquals(VerificationConfidence.REJECTED, result.confidence)
    }

    @Test
    fun failsFranchiseFalsePositive() {
        val identity = MediaIdentity(
            tmdbId = 101,
            tmdbType = "movie",
            title = "The Batman",
            year = 2022
        )
        val server = createServer(name = "Batman Begins", url = "https://example.com/batman-begins.mp4")
        val result = ContentMatchVerifier.verifyServer(identity, server)
        assertEquals(VerificationConfidence.REJECTED, result.confidence)
    }

    @Test
    fun failsShortTitleFalsePositive() {
        val identity = MediaIdentity(
            tmdbId = 102,
            tmdbType = "movie",
            title = "It",
            year = 2017
        )
        val server = createServer(name = "It Chapter Two", url = "https://example.com/it-chapter-two.mp4")
        val result = ContentMatchVerifier.verifyServer(identity, server)
        assertEquals(VerificationConfidence.REJECTED, result.confidence)
    }

    @Test
    fun failsMovieVsTvSeries() {
        val identity = MediaIdentity(
            tmdbId = 103,
            tmdbType = "movie",
            title = "Breaking Bad",
            year = 2008
        )
        val server = createServer(name = "Breaking Bad S01E01", url = "https://example.com/breaking-bad-s01e01.mp4")
        val result = ContentMatchVerifier.verifyServer(identity, server)
        assertEquals(VerificationConfidence.REJECTED, result.confidence)
    }

    @Test
    fun passesTvEpisodeMatch() {
        val identity = MediaIdentity(
            tmdbId = 200,
            tmdbType = "tv",
            title = "Stranger Things",
            season = 2,
            episode = 5,
            year = 2017
        )
        val server = createServer(name = "Stranger Things S02E05 Episode 5", url = "https://example.com/stranger-things-s02e05.mp4")
        val result = ContentMatchVerifier.verifyServer(identity, server)
        assertTrue(result.confidence == VerificationConfidence.HIGH || result.confidence == VerificationConfidence.MEDIUM)
    }

    @Test
    fun failsTvWrongEpisode() {
        val identity = MediaIdentity(
            tmdbId = 200,
            tmdbType = "tv",
            title = "Stranger Things",
            season = 2,
            episode = 5,
            year = 2017
        )
        val server = createServer(name = "Stranger Things S02E04", url = "https://example.com/stranger-things-s02e04.mp4")
        val result = ContentMatchVerifier.verifyServer(identity, server)
        assertEquals(VerificationConfidence.REJECTED, result.confidence)
    }

    @Test
    fun failsConflictingYear() {
        val identity = MediaIdentity(
            tmdbId = 300,
            tmdbType = "movie",
            title = "Drishyam",
            year = 2015
        )
        val server = createServer(name = "Drishyam (2013)", url = "https://example.com/drishyam-2013.mp4")
        val result = ContentMatchVerifier.verifyServer(identity, server)
        assertEquals(VerificationConfidence.REJECTED, result.confidence)
    }

    private fun createServer(name: String, url: String) = VideoServer(
        id = "srv-${url.hashCode()}",
        providerId = "test-provider",
        providerName = "Test Provider",
        name = name,
        url = url,
        quality = StreamQuality.Q1080
    )
}
