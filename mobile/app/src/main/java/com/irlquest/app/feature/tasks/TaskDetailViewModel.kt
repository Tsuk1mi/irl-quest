package com.irlquest.app.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlquest.app.data.network.dto.TaskDto
import com.irlquest.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TaskDetailUiState(
    val isLoading: Boolean = false,
    val task: TaskDto? = null,
    val error: String? = null
)

class TaskDetailViewModel(
    private val repo: TaskRepository = TaskRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    fun loadTask(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val task = repo.getTask(id)
                _uiState.value = _uiState.value.copy(isLoading = false, task = task)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun toggleCompleted() {
        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            try {
                if (!task.completed) {
                    // Сервер поддерживает только завершение задачи
                    repo.updateTask(task.id, completed = true)
                    loadTask(task.id)
                } else {
                    // Сервер не поддерживает "разавершение" задачи
                    _uiState.value = _uiState.value.copy(error = "Разавершение задачи не поддерживается")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateTask(@Suppress("UNUSED_PARAMETER") title: String?, @Suppress("UNUSED_PARAMETER") description: String?) {
        if (_uiState.value.task == null) return
        viewModelScope.launch {
            try {
                // ⚠️ Сервер не поддерживает обновление задач (только завершение)
                _uiState.value = _uiState.value.copy(error = "Обновление задач не поддерживается сервером. Можно только завершить задачу.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteTask(@Suppress("UNUSED_PARAMETER") onDeleted: () -> Unit = {}) {
        if (_uiState.value.task == null) return
        viewModelScope.launch {
            try {
                // ⚠️ Сервер не поддерживает удаление задач
                _uiState.value = _uiState.value.copy(error = "Удаление задач не поддерживается. Завершите задачу вместо этого.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
