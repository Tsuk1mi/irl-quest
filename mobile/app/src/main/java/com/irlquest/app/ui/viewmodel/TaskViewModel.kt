package com.irlquest.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.irlquest.app.data.repository.TaskRepository
import com.irlquest.app.data.network.dto.TaskDto
import com.irlquest.app.data.network.dto.RagQuestGenerationResponse
import com.irlquest.app.data.network.dto.RagClassifyResponse
import timber.log.Timber

class TaskViewModel(private val repo: TaskRepository = TaskRepository()) : ViewModel() {
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _tasks = MutableStateFlow<List<TaskDto>>(emptyList())
    val tasks: StateFlow<List<TaskDto>> = _tasks

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Новое состояние: результат генерации квеста из задачи
    private val _generatedQuest = MutableStateFlow<RagQuestGenerationResponse?>(null)
    val generatedQuest: StateFlow<RagQuestGenerationResponse?> = _generatedQuest

    fun loadTasks() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val list = repo.listTasks()
                _tasks.value = list
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load tasks"
            } finally {
                _loading.value = false
            }
        }
    }

    fun createTask(title: String, description: String?, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                repo.createTask(title, description)
                loadTasks()
                onSuccess?.invoke()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create task"
            } finally {
                _loading.value = false
            }
        }
    }

    fun updateTask(id: Int, title: String? = null, description: String? = null, completed: Boolean? = null, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                repo.updateTask(id, title = title, description = description, completed = completed)
                loadTasks()
                onSuccess?.invoke()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update task"
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteTask(id: Int, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                repo.deleteTask(id)
                loadTasks()
                onSuccess?.invoke()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete task"
            } finally {
                _loading.value = false
            }
        }
    }

    // Новая функция: классификация задачи (теги, сложность) через RAG/ML и автоматическое применение к задаче
    fun classifyAndApplyAttributes(taskId: Int, title: String, description: String?) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val resp: RagClassifyResponse = repo.classifyTask(title + (description?.let { " \n$it" } ?: ""))
                // Обновим задачу с предсказанными атрибутами
                val priority = when (resp.estimatedDifficulty) {
                    in 4..5 -> "high"
                    3 -> "medium"
                    else -> "low"
                }
                repo.updateTask(taskId, priority = priority, difficulty = resp.estimatedDifficulty, tags = resp.tags)
                loadTasks()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to classify task"
                Timber.e(e, "classifyAndApplyAttributes failed")
            } finally {
                _loading.value = false
            }
        }
    }

    // Новая функция: генерация квеста из задачи через RAG/ML
    fun generateQuestFromTask(title: String, description: String?, priority: String, estimatedDuration: Int?, difficulty: Int, tags: List<String>) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val resp: RagQuestGenerationResponse = repo.generateQuestFromTask(title, description, priority, estimatedDuration, difficulty, tags)
                _generatedQuest.value = resp
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to generate quest"
                Timber.e(e, "generateQuestFromTask failed")
                _generatedQuest.value = null
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearGeneratedQuest() {
        _generatedQuest.value = null
    }

    fun saveGeneratedQuest(onSuccess: (() -> Unit)? = null) {
        val gen = _generatedQuest.value ?: return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                repo.createQuestFromGenerated(gen)
                // после сохранения можно очистить и обновить список квестов/задач
                _generatedQuest.value = null
                loadTasks()
                onSuccess?.invoke()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to save generated quest"
                Timber.e(e, "saveGeneratedQuest failed")
            } finally {
                _loading.value = false
            }
        }
    }
}
