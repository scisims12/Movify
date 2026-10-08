package com.ivor.movify.data.streaming

import com.ivor.movify.domain.model.MediaIdentity
import com.ivor.movify.domain.model.VideoServer
import java.text.Normalizer
import kotlin.math.abs

enum class VerificationConfidence {
    HIGH,
    MEDIUM,
    LOW,
    REJECTED
}

data class VerificationResult(
    val server: VideoServer,
    val confidence: VerificationConfidence,
    val reason: String
)

object ContentMatchVerifier {

    fun verify(identity: MediaIdentity, servers: List<VideoServer>): List<VideoServer> {
        return servers.mapNotNull { server ->
            val result = verifyServer(identity, server)
            if (result.confidence != VerificationConfidence.REJECTED) {
                if (com.ivor.movify.BuildConfig.DEBUG) {
                    android.util.Log.d("ContentMatchVerifier", "Accepted server [${server.name}] with confidence ${result.confidence}: ${result.reason}")
                }
                server
            } else {
                if (com.ivor.movify.BuildConfig.DEBUG) {
                    android.util.Log.d("ContentMatchVerifier", "Rejected server [${server.name}]: ${result.reason}")
                }
                null
            }
        }
    }

    fun verifyServer(identity: MediaIdentity, server: VideoServer): VerificationResult {
        val serverText = "${server.name} ${server.url} ${server.id} ${server.providerId} ${server.audioLanguage.orEmpty()}".lowercase()

        // 1. Exact TMDB ID match
        if (serverText.contains("tmdb${identity.tmdbId}") ||
            serverText.contains("tmdb_id=${identity.tmdbId}") ||
            serverText.contains("/${identity.tmdbId}/") ||
            serverText.contains("tmdbid=${identity.tmdbId}")) {
            return VerificationResult(server, VerificationConfidence.HIGH, "Exact TMDB ID match")
        }

        // 2. Exact IMDb ID match
        if (!identity.imdbId.isNullOrBlank() && serverText.contains(identity.imdbId.lowercase())) {
            return VerificationResult(server, VerificationConfidence.HIGH, "Exact IMDb ID match")
        }

        // 3. Movie vs TV Series Check
        val isTv = identity.tmdbType.equals("tv", ignoreCase = true)

        if (isTv) {
            val se = parseSeasonEpisode(serverText)
            if (se != null) {
                val (candidateSeason, candidateEpisode) = se
                if (candidateSeason != identity.season || candidateEpisode != identity.episode) {
                    return VerificationResult(
                        server,
                        VerificationConfidence.REJECTED,
                        "TV Episode mismatch: requested S${identity.season}E${identity.episode}, got S${candidateSeason}E${candidateEpisode}"
                    )
                }
            }
        } else {
            // Movie requested, but candidate has TV episode format
            if (Regex("s\\d+e\\d+").containsMatchIn(serverText) || serverText.contains("season") || serverText.contains("episode")) {
                return VerificationResult(server, VerificationConfidence.REJECTED, "Movie vs TV mismatch: requested movie, got TV episode format")
            }
        }

        // 4. Release Year Check
        val candidateYear = parseYear(serverText)
        if (identity.year != null && candidateYear != null) {
            val yearDiff = abs(identity.year - candidateYear)
            if (yearDiff > 1) {
                return VerificationResult(
                    server,
                    VerificationConfidence.REJECTED,
                    "Release year conflict: requested ${identity.year}, got $candidateYear"
                )
            }
        }

        // 5. False Positive Safety Checks
        if (isFalsePositiveMatch(identity.title, serverText)) {
            return VerificationResult(server, VerificationConfidence.REJECTED, "False positive safety check failed")
        }

        // 6. Title Normalization & Matching
        val acceptedTitles = buildAcceptedTitles(identity)
        val matchScore = scoreTitleMatch(serverText, acceptedTitles)

        val selectedNormalized = normalizeTitle(identity.title)
        if (selectedNormalized.length <= 3) {
            val isExactWordMatch = acceptedTitles.any { acc ->
                val normAcc = normalizeTitle(acc)
                Regex("\\b${Regex.escape(normAcc)}\\b").containsMatchIn(normalizeTitle(serverText))
            }
            if (!isExactWordMatch && matchScore < 0.9) {
                return VerificationResult(server, VerificationConfidence.REJECTED, "Short title strict match failed for '${identity.title}'")
            }
        }

        when {
            matchScore >= 0.8 -> {
                val confidence = if (identity.year == null || candidateYear == null || identity.year == candidateYear) {
                    VerificationConfidence.HIGH
                } else {
                    VerificationConfidence.MEDIUM
                }
                return VerificationResult(server, confidence, "Strong title match (score: $matchScore)")
            }
            matchScore >= 0.5 -> {
                return VerificationResult(server, VerificationConfidence.MEDIUM, "Moderate title match (score: $matchScore)")
            }
            server.providerId.startsWith("vidking") || server.id.startsWith("vidking") -> {
                if (candidateYear == null || identity.year == null || identity.year == candidateYear) {
                    return VerificationResult(server, VerificationConfidence.MEDIUM, "Direct provider response accepted")
                }
            }
        }

        if (matchScore >= 0.4) {
            return VerificationResult(server, VerificationConfidence.MEDIUM, "Acceptable match")
        }

        return VerificationResult(server, VerificationConfidence.REJECTED, "Low confidence title match (score: $matchScore)")
    }

    fun normalizeTitle(title: String?): String {
        if (title.isNullOrBlank()) return ""
        val normalized = Normalizer.normalize(title, Normalizer.Form.NFD)
        val withoutDiacritics = Regex("\\p{InCombiningDiacriticalMarks}+").replace(normalized, "")
        return withoutDiacritics.lowercase()
            .replace(Regex("[\\p{P}\\p{S}]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun buildAcceptedTitles(identity: MediaIdentity): List<String> {
        val titles = mutableSetOf<String>()
        if (identity.title.isNotBlank()) titles.add(identity.title)
        if (!identity.originalTitle.isNullOrBlank()) titles.add(identity.originalTitle)
        val base = identity.title
        if (base.contains(":")) {
            titles.add(base.replace(":", ""))
            titles.add(base.substringBefore(":"))
        }
        if (base.contains("-")) {
            titles.add(base.replace("-", " "))
            titles.add(base.substringBefore("-"))
        }
        return titles.toList()
    }

    fun scoreTitleMatch(serverText: String, acceptedTitles: List<String>): Double {
        val normServer = normalizeTitle(serverText)
        var bestScore = 0.0
        for (title in acceptedTitles) {
            val normTitle = normalizeTitle(title)
            if (normTitle.isEmpty()) continue
            if (normServer.contains(normTitle)) {
                return 1.0
            }
            val serverWords = normServer.split(" ").filter { it.isNotBlank() }.toSet()
            val titleWords = normTitle.split(" ").filter { it.isNotBlank() }.toSet()
            if (titleWords.isNotEmpty()) {
                val intersection = serverWords.intersect(titleWords).size
                val matchRatio = intersection.toDouble() / titleWords.size
                if (matchRatio > bestScore) {
                    bestScore = matchRatio
                }
            }
        }
        return bestScore
    }

    fun parseSeasonEpisode(text: String): Pair<Int, Int>? {
        val s00e00 = Regex("s(\\d+)e(\\d+)", RegexOption.IGNORE_CASE).find(text)
        if (s00e00 != null) {
            return (s00e00.groupValues[1].toIntOrNull() ?: 1) to (s00e00.groupValues[2].toIntOrNull() ?: 1)
        }
        val nxn = Regex("(\\d+)x(\\d+)", RegexOption.IGNORE_CASE).find(text)
        if (nxn != null) {
            return (nxn.groupValues[1].toIntOrNull() ?: 1) to (nxn.groupValues[2].toIntOrNull() ?: 1)
        }
        val seasonEp = Regex("season[._ -]?(\\d+)[._ -]?[a-z]*[._ -]?episode[._ -]?(\\d+)", RegexOption.IGNORE_CASE).find(text)
        if (seasonEp != null) {
            return (seasonEp.groupValues[1].toIntOrNull() ?: 1) to (seasonEp.groupValues[2].toIntOrNull() ?: 1)
        }
        return null
    }

    fun parseYear(text: String): Int? {
        val match = Regex("\\b(19\\d{2}|20\\d{2})\\b").find(text)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    fun isFalsePositiveMatch(selectedTitle: String, serverText: String): Boolean {
        val normSelected = normalizeTitle(selectedTitle)
        val normServer = normalizeTitle(serverText)

        val sequelRegex = Regex("\\b${Regex.escape(normSelected)}\\s+(?:2|ii|part\\s+2|vol\\s+2)\\b", RegexOption.IGNORE_CASE)
        if (sequelRegex.containsMatchIn(normServer)) {
            return true
        }
        if (normSelected == "the batman" && normServer.contains("batman begins")) {
            return true
        }
        if (normSelected == "it" && (normServer.contains("chapter two") || normServer.contains("chapter 2"))) {
            return true
        }
        return false
    }
}
