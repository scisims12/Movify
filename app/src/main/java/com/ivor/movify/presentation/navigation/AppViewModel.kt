package com.ivor.movify.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivor.movify.data.network.NetworkMonitor
import com.ivor.movify.data.update.AppUpdateManager
import com.ivor.movify.data.update.UpdateUiState
import com.ivor.movify.domain.model.WatchProgress
import com.ivor.movify.domain.repository.WatchProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** App-wide state the navigation shell needs: connectivity and launcher shortcut targets. */
@HiltViewModel
class AppViewModel @Inject constructor(
    networkMonitor: NetworkMonitor,
    private val watchProgressRepository: WatchProgressRepository,
    private val updateManager: AppUpdateManager
) : ViewModel() {

    val updateState: StateFlow<UpdateUiState> = updateManager.uiState

    init {
        updateManager.checkForUpdate(isStartup = true)
    }

    fun dismissUpdate(version: String) {
        updateManager.dismissUpdate(version)
    }

    /** Starts optimistic so the offline banner never flashes while the first reading arrives. */
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** The title the Continue Watching shortcut should resume, if any. */
    suspend fun latestContinueWatching(): WatchProgress? =
        watchProgressRepository.continueWatching(limit = 1).first().firstOrNull()
}
