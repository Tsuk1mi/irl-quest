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

    fun updateProfile(
        username: String?,
        avatarUrl: String?,
        bio: String?,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val updated = repo.updateProfile(username, avatarUrl, bio)
                _currentUser.value = updated
                onSuccess?.invoke()
            } catch (e: Exception) {
                val message = e.message ?: "Не удалось обновить профиль"
                _error.value = message
                onError?.invoke(message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addExperienceAndGold(xp: Int, gold: Int) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val newXp = (user.experience ?: 0) + xp
            val newGold = (user.gold ?: 0) + gold
            
            _currentUser.value = user.copy(
                experience = newXp,
                gold = newGold
            )
        }
    }

    fun checkLevelUp(xp: Int): Pair<Boolean, Int?> {
        val user = _currentUser.value ?: return Pair(false, null)
        val currentLevel = user.level ?: 1
        val currentXp = user.experience ?: 0
        val xpForNext = (currentLevel + 1) * 100
        
        return if (currentXp >= xpForNext) {
            val newLevel = currentLevel + 1
            viewModelScope.launch {
                _currentUser.value = user.copy(
                    level = newLevel,
                    experience = currentXp - xpForNext
                )
            }
            Pair(true, newLevel)
        } else {
            Pair(false, null)
        }
    }
}

