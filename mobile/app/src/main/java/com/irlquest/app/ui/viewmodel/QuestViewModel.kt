package com.irlquest.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.irlquest.app.data.repository.QuestRepository
import com.irlquest.app.data.network.dto.QuestDto

class QuestViewModel(private val repo: QuestRepository = QuestRepository()) : ViewModel() {
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _quests = MutableStateFlow<List<QuestDto>>(emptyList())
    val quests: StateFlow<List<QuestDto>> = _quests

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadQuests() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val list = repo.listQuests()
                _quests.value = list
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load quests"
            } finally {
                _loading.value = false
            }
        }
    }

    fun createQuest(title: String, description: String?, difficulty: Int = 1, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                repo.createQuest(title, description, difficulty)
                loadQuests()
                onSuccess?.invoke()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create quest"
            } finally {
                _loading.value = false
            }
        }
    }
}

