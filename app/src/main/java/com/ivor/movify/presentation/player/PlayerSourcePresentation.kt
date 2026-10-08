package com.ivor.movify.presentation.player

import com.ivor.movify.domain.model.StreamQuality
import com.ivor.movify.domain.model.VideoServer

fun VideoServer.sourceQualityLabel(): String {
    val adaptive = streamType == "HLS" || streamType == "DASH"
    return when {
        quality == StreamQuality.UNKNOWN && adaptive -> "Adaptive"
        quality == StreamQuality.UNKNOWN -> "Quality unknown"
        adaptive -> "Up to ${quality.label}"
        else -> quality.label
    }
}

fun VideoServer.sourceSummary(): String = buildList {
    add(sourceQualityLabel())
    if (audio.label.isNotBlank()) add(audio.label)
    add(streamType)
}.joinToString(" · ")
