package com.ivor.movify.data.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.ivor.movify.BuildConfig
import com.ivor.movify.data.remote.GithubApi
import com.ivor.movify.data.remote.GithubAssetDto
import com.ivor.movify.data.remote.GithubReleaseDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed interface UpdateUiState {
    data object Checking : UpdateUiState
    data class UpToDate(val latest: GithubReleaseDto?) : UpdateUiState
    data object NoReleaseYet : UpdateUiState
    data class Available(val release: GithubReleaseDto, val asset: GithubAssetDto?) : UpdateUiState
    data class Downloading(val release: GithubReleaseDto, val downloadedBytes: Long, val totalBytes: Long) : UpdateUiState {
        val progress: Float get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    }
    data class ReadyToInstall(val release: GithubReleaseDto, val apk: File) : UpdateUiState
    data class Failed(val message: String, val release: GithubReleaseDto? = null) : UpdateUiState
}

@Singleton
class AppUpdateManager @Inject constructor(
    private val githubApi: GithubApi,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Checking)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    val currentVersion: String = BuildConfig.VERSION_NAME

    private val downloads = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private var downloadId = -1L
    private var downloadJob: Job? = null
    private var hasCheckedOnStartup = false
    private var dismissedVersion: String? = null

    fun checkForUpdate(isStartup: Boolean = false) {
        if (isStartup) {
            if (hasCheckedOnStartup) return
            hasCheckedOnStartup = true
        }
        
        scope.launch {
            if (!isStartup) _uiState.value = UpdateUiState.Checking
            _uiState.value = runCatching { githubApi.getLatestRelease() }.fold(
                onSuccess = { release ->
                    val latestIsLegacy = release.tagName.startsWith("2.") && currentVersion.startsWith("1.")
                    if (!latestIsLegacy && isNewerVersion(release.tagName, currentVersion)) {
                        if (isStartup && dismissedVersion == release.tagName) {
                            UpdateUiState.Checking // or UpToDate to hide it
                        } else {
                            UpdateUiState.Available(release, pickAsset(release))
                        }
                    } else {
                        UpdateUiState.UpToDate(release)
                    }
                },
                onFailure = { e ->
                    if (isStartup) {
                        UpdateUiState.Checking
                    } else {
                        when (e) {
                            is HttpException -> {
                                when (e.code()) {
                                    404 -> UpdateUiState.NoReleaseYet
                                    403 -> UpdateUiState.Failed("GitHub rate limit exceeded. Try again later.")
                                    else -> UpdateUiState.Failed("Something went wrong (${e.code()}).")
                                }
                            }
                            is IOException -> UpdateUiState.Failed("Couldn't reach GitHub. Check your connection and try again.")
                            else -> UpdateUiState.Failed("Something went wrong (${e.message}).")
                        }
                    }
                }
            )
        }
    }

    fun dismissUpdate(version: String) {
        dismissedVersion = version
        _uiState.value = UpdateUiState.Checking
    }

    fun download(release: GithubReleaseDto, asset: GithubAssetDto) {
        val target = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), asset.name)
        target.delete() // A stale copy would make DownloadManager pick a different name.

        val request = DownloadManager.Request(Uri.parse(asset.downloadUrl))
            .setTitle("Movify ${release.tagName}")
            .setDescription("Downloading update")
            .setMimeType(APK_MIME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, asset.name)
        downloadId = downloads.enqueue(request)
        _uiState.value = UpdateUiState.Downloading(release, 0L, asset.size)

        downloadJob?.cancel()
        downloadJob = scope.launch { trackDownload(release, target) }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        if (downloadId != -1L) downloads.remove(downloadId)
        downloadId = -1L
        checkForUpdate()
    }

    fun canInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun install(apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, APK_MIME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
    }

    private suspend fun trackDownload(release: GithubReleaseDto, target: File) {
        while (true) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val snapshot = downloads.query(query)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                Triple(
                    cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                )
            }
            when (snapshot?.first) {
                null, DownloadManager.STATUS_FAILED -> {
                    _uiState.value = UpdateUiState.Failed("The download didn't finish. Try again.", release)
                    return
                }
                DownloadManager.STATUS_SUCCESSFUL -> {
                    _uiState.value = UpdateUiState.ReadyToInstall(release, target)
                    return
                }
                else -> _uiState.value = UpdateUiState.Downloading(release, snapshot.second, snapshot.third)
            }
            delay(PROGRESS_POLL_MS)
        }
    }

    private fun pickAsset(release: GithubReleaseDto): GithubAssetDto? {
        val apks = release.assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        val abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
        
        val movifyApks = apks.filter { it.name.contains("Movify", ignoreCase = true) || it.name.contains("app-release", ignoreCase = true) }
        val targets = if (movifyApks.isNotEmpty()) movifyApks else apks
        
        return targets.firstOrNull { abi.isNotEmpty() && it.name.contains(abi, ignoreCase = true) }
            ?: targets.firstOrNull { it.name.contains("universal", ignoreCase = true) }
            ?: targets.firstOrNull { SPLIT_ABIS.none { split -> it.name.contains(split, ignoreCase = true) } }
            ?: targets.firstOrNull()
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        fun numbers(version: String): List<Int>? =
            Regex("""\d+(\.\d+)*""").find(version)?.value?.split(".")?.map { it.toInt() }
        val l = numbers(latest) ?: return false
        val c = numbers(current) ?: return false
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv != cv) return lv > cv
        }
        return false
    }

    private companion object {
        const val APK_MIME = "application/vnd.android.package-archive"
        const val PROGRESS_POLL_MS = 400L
        val SPLIT_ABIS = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
    }
}
