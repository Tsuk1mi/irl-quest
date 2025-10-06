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
                repo.updateTask(task.id, completed = !task.completed)
                loadTask(task.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateTask(title: String?, description: String?) {
        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            try {
                repo.updateTask(task.id, title = title, description = description, completed = task.completed)
                loadTask(task.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteTask(onDeleted: () -> Unit = {}) {
        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            try {
                repo.deleteTask(task.id)
                onDeleted()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
