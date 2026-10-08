package com.ivor.movify.presentation.onboarding

import androidx.lifecycle.ViewModel
import com.ivor.movify.data.settings.AppSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appSettingsStore: AppSettingsStore
) : ViewModel() {

    fun completeOnboarding() {
        appSettingsStore.update { it.copy(onboardingCompleted = true) }
    }
}
