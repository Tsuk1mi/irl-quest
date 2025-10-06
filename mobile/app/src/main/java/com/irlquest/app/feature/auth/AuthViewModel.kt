package com.irlquest.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlquest.app.data.network.dto.UserDto
import com.irlquest.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {
    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val user = repo.login(username, password)
                _currentUser.value = user
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка входа"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(email: String, username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val user = repo.register(email, username, password)
                _currentUser.value = user
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка регистрации"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchMe() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val user = repo.getMe()
                _currentUser.value = user
            } catch (e: Exception) {
                _error.value = e.message ?: "Не удалось получить профиль"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        repo.logout()
        _currentUser.value = null
    }
}

