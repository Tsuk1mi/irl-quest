package com.irlquest.app.feature.quests

import com.irlquest.app.data.network.dto.TaskDto
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class QuestStatus {
    ACTIVE, COMPLETED, PAUSED, ARCHIVED
}

enum class QuestPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class QuestFilter(val displayName: String, val icon: ImageVector) {
    ALL("Все", Icons.Default.List),
    ACTIVE("Активные", Icons.Default.PlayArrow),
    COMPLETED("Завершенные", Icons.Default.CheckCircle),
    HIGH_PRIORITY("Важные", Icons.Default.PriorityHigh),
    OVERDUE("Просроченные", Icons.Default.Warning)
}

data class QuestUi(
    val id: Int,
    val title: String,
    val description: String,
    val status: QuestStatus,
    val priority: QuestPriority,
    val difficulty: Int,
    val completionPercentage: Int,
    val totalTasks: Int,
    val completedTasks: Int,
    val experienceReward: Int,
    val deadline: String?,
    val isOverdue: Boolean,
    val createdAt: String,
    val questType: String,
    val tasks: List<TaskDto> = emptyList()
)
