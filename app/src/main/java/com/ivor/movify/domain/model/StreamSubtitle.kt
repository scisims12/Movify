package com.ivor.movify.domain.model

data class StreamSubtitle(
    val url: String,
    val label: String,
    val language: String? = null,
    val headers: Map<String, String> = emptyMap()
)
