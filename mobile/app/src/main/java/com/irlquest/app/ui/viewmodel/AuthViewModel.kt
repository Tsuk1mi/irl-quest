package com.irlquest.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.irlquest.app.data.repository.AuthRepository
import com.irlquest.app.data.network.dto.UserDto
import timber.log.Timber

class AuthViewModel(private val repo: AuthRepository = AuthRepository()) : ViewModel() {
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
                Timber.d("AuthViewModel: login successful, user=%s", user.username)
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка входа"
                Timber.e(e, "AuthViewModel: login failed for %s", username)
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
                // register may not return token — register then login
                repo.register(email, username, password)
                val user = repo.login(username, password)
                _currentUser.value = user
                Timber.d("AuthViewModel: register+login successful, user=%s", user.username)
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка регистрации"
                Timber.e(e, "AuthViewModel: register failed for %s", username)
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
                Timber.d("AuthViewModel: fetchMe -> %s", user?.username ?: "null")
            } catch (e: Exception) {
                _error.value = e.message ?: "Не удалось получить профиль"
                Timber.e(e, "AuthViewModel: fetchMe failed")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        repo.logout()
        _currentUser.value = null
        Timber.d("AuthViewModel: logout")
    }
    
    /**
     * 🎁 Добавить опыт и золото (локально, пока без сервера)
     */
    fun addExperienceAndGold(xp: Int, gold: Int) {
        val current = _currentUser.value ?: return
        
        val newXp = current.experience + xp
        val newGold = current.gold + gold
        
        // Проверка повышения уровня
        val xpForNextLevel = calculateXpForLevel(current.level + 1)
        val levelUp = newXp >= xpForNextLevel
        val newLevel = if (levelUp) current.level + 1 else current.level
        val finalXp = if (levelUp) newXp - xpForNextLevel else newXp
        
        _currentUser.value = current.copy(
            experience = finalXp,
            gold = newGold,
            level = newLevel
        )
        
        Timber.d("AuthViewModel: added XP=$xp, Gold=$gold, LevelUp=$levelUp, NewLevel=$newLevel")
        
        // TODO: Отправить на сервер обновление
        viewModelScope.launch {
            try {
                // repo.updateUserProgress(xp, gold)
                // fetchMe() // Обновить с сервера
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync progress with server")
            }
        }
    }
    
    /**
     * Расчёт опыта для следующего уровня
     */
    private fun calculateXpForLevel(level: Int): Int {
        return level * 100 // Простая формула: каждый уровень требует level * 100 XP
    }
    
    /**
     * Проверить, достаточно ли XP для повышения уровня
     */
    fun checkLevelUp(addedXp: Int): Pair<Boolean, Int?> {
        val current = _currentUser.value ?: return Pair(false, null)
        val newTotalXp = current.experience + addedXp
        val xpForNext = calculateXpForLevel(current.level + 1)
        return if (newTotalXp >= xpForNext) {
            Pair(true, current.level + 1)
        } else {
            Pair(false, null)
        }
    }
}
