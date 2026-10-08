package com.ivor.movify.data.streaming

import com.ivor.movify.domain.model.MediaIdentity
import com.ivor.movify.domain.model.VideoServer

interface StreamProvider {
    val id: String
    val displayName: String
    val priority: Int
    val isEnabled: Boolean
    val isFallback: Boolean
        get() = false

    suspend fun resolve(identity: MediaIdentity): Result<List<VideoServer>>
}
