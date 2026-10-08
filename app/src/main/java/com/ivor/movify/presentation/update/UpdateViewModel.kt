package com.ivor.movify.presentation.update

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.ivor.movify.BuildConfig
import com.ivor.movify.data.remote.GithubAssetDto
import com.ivor.movify.data.remote.GithubReleaseDto
import com.ivor.movify.data.update.AppUpdateManager
import com.ivor.movify.data.update.UpdateUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateManager: AppUpdateManager
) : ViewModel() {

    val uiState: StateFlow<UpdateUiState> = updateManager.uiState

    val currentVersion: String = BuildConfig.VERSION_NAME

    init {
        checkForUpdate()
    }

    fun checkForUpdate() {
        updateManager.checkForUpdate(isStartup = false)
    }

    fun download(release: GithubReleaseDto, asset: GithubAssetDto) {
        updateManager.download(release, asset)
    }

    fun cancelDownload() {
        updateManager.cancelDownload()
    }

    fun canInstall(): Boolean = updateManager.canInstall()

    fun installPermissionIntent(): Intent = updateManager.installPermissionIntent()

    fun install(apk: File) {
        updateManager.install(apk)
    }

    override fun onCleared() {
        // We don't cancel the job on UpdateViewModel cleared anymore
        // because we want downloads to continue in the background via the manager.
    }
}
