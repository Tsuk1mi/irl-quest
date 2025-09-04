package com.irlquest.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.irlquest.app.data.repository.TaskRepository
import com.irlquest.app.data.network.dto.TaskDto

class TaskViewModel(private val repo: TaskRepository = TaskRepository()) : ViewModel() {
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _tasks = MutableStateFlow<List<TaskDto>>(emptyList())
    val tasks: StateFlow<List<TaskDto>> = _tasks

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

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
}
