package com.tott.app.ui.state

import androidx.lifecycle.ViewModel
import com.tott.app.data.PrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val prefs: PrefsRepository
) : ViewModel() {
    fun completeOnboarding() = prefs.completeOnboarding()
}
