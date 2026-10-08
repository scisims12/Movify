package com.ivor.movify.data.streaming

import com.ivor.movify.domain.model.VideoServer

internal object ServerRanker {
    fun mergeAndRank(
        existing: List<VideoServer>,
        incoming: List<VideoServer>,
        providerPriorities: Map<String, Int>,
        preferredServerId: String?
    ): List<VideoServer> {
        val merged = linkedMapOf<String, VideoServer>()
        (existing + incoming).forEach { candidate ->
            val key = candidate.url.substringBefore('#')
            val current = merged[key]
            if (current == null || compare(candidate, current, providerPriorities) < 0) {
                merged[key] = candidate
            }
        }

        return merged.values.sortedWith { left, right ->
            when {
                left.id == preferredServerId && right.id != preferredServerId -> -1
                right.id == preferredServerId && left.id != preferredServerId -> 1
                else -> compare(left, right, providerPriorities)
            }
        }
    }

    private fun compare(
        left: VideoServer,
        right: VideoServer,
        providerPriorities: Map<String, Int>
    ): Int {
        // Priority 1: Hindi audio preference
        val leftIsHindi = left.audioLanguage.equals("Hindi", ignoreCase = true) || left.audio.name.equals("Hindi", ignoreCase = true)
        val rightIsHindi = right.audioLanguage.equals("Hindi", ignoreCase = true) || right.audio.name.equals("Hindi", ignoreCase = true)
        if (leftIsHindi && !rightIsHindi) return -1
        if (rightIsHindi && !leftIsHindi) return 1

        // Priority 2: Quality rank
        val quality = right.quality.rank.compareTo(left.quality.rank)
        if (quality != 0) return quality

        // Priority 3: Audio type rank
        val audio = right.audio.rank.compareTo(left.audio.rank)
        if (audio != 0) return audio

        // Priority 4: Provider priority
        val provider = (providerPriorities[left.providerId] ?: Int.MAX_VALUE)
            .compareTo(providerPriorities[right.providerId] ?: Int.MAX_VALUE)
        if (provider != 0) return provider

        // Priority 5: Server name
        return left.name.compareTo(right.name, ignoreCase = true)
    }
}
