package com.irlquest.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.irlquest.app.data.repository.AuthRepository
import com.irlquest.app.data.network.dto.UserOutResponse

class AuthViewModel(private val repo: AuthRepository = AuthRepository()) : ViewModel() {
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _currentUser = MutableStateFlow<UserOutResponse?>(null)
    val currentUser: StateFlow<UserOutResponse?> = _currentUser

    fun login(username: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                repo.login(username, password)
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message ?: "Login failed"
            } finally {
                _loading.value = false
            }
        }
    }

    fun register(email: String, username: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                repo.register(email, username, password)
                // После успешной регистрации — логинимся (используем email как username для токена)
                repo.login(email, password)
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message ?: "Registration failed"
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadCurrentUser(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val u = repo.me()
                _currentUser.value = u
                onSuccess?.invoke()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load user"
            } finally {
                _loading.value = false
            }
        }
    }
}
