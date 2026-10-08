package com.ivor.movify.domain.repository

import com.ivor.movify.data.remote.model.SubtitleDto
import com.ivor.movify.domain.model.MediaIdentity

interface SubtitleRepository {
    /** External subtitles for a movie or episode, best matches first. Empty when none are found. */
    suspend fun search(identity: MediaIdentity): List<SubtitleDto>
}
