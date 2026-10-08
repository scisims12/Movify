package com.ivor.movify.data.service

import android.app.Notification
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HlsDownloadService : DownloadService(
    1, // NOTIFICATION_ID
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    "download_channel",
    com.ivor.movify.R.string.download_channel_name,
    0
) {

    @Inject
    lateinit var media3DownloadManager: DownloadManager

    @Inject
    lateinit var notificationHelper: androidx.media3.exoplayer.offline.DownloadNotificationHelper

    override fun getDownloadManager(): DownloadManager = media3DownloadManager

    override fun getScheduler(): Scheduler? = null

    @OptIn(UnstableApi::class)
    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification {
        return notificationHelper.buildProgressNotification(
            this,
            com.ivor.movify.R.drawable.ic_stat_movify,
            null,
            null,
            downloads,
            notMetRequirements
        )
    }
}
