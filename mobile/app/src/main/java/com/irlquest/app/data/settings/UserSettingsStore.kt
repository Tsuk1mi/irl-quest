package com.irlquest.app.data.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object UserSettingsStore {
    private val _useDarkTheme = MutableStateFlow(false)
    val useDarkTheme: StateFlow<Boolean> = _useDarkTheme

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled

    private val _allowLocalNetwork = MutableStateFlow(true)
    val allowLocalNetwork: StateFlow<Boolean> = _allowLocalNetwork

    fun setDarkTheme(enabled: Boolean) {
        _useDarkTheme.value = enabled
    }

    fun setNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun setAllowLocalNetwork(enabled: Boolean) {
        _allowLocalNetwork.value = enabled
    }
}

