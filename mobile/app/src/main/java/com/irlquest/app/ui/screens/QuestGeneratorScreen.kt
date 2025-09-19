package com.irlquest.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Casino
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irlquest.app.data.network.dto.QuestGenerationRequest
import com.irlquest.app.ui.viewmodels.QuestGeneratorViewModel
import kotlinx.coroutines.launch

@Composable
fun QuestGeneratorScreen(
    viewModel: QuestGeneratorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    var todoText by remember { mutableStateOf("") }
    var selectedTheme by remember { mutableStateOf("fantasy") }
    var selectedDifficulty by remember { mutableStateOf(3) }
    var context by remember { mutableStateOf("") }
    
    val themes = listOf(
        "fantasy" to "🧙‍♂️ Fantasy",
        "sci-fi" to "🚀 Sci-Fi", 
        "modern" to "💼 Modern",
        "medieval" to "⚔️ Medieval"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            text = "IRL Quest Generator",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "Transform your boring TODO into epic D&D adventures!",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // TODO Input
        OutlinedTextField(
            value = todoText,
            onValueChange = { todoText = it },
            label = { Text("Your TODO Task") },
            placeholder = { Text("e.g., Clean my room, Learn Kotlin, Exercise...") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Context Input (Optional)
        OutlinedTextField(
            value = context,
            onValueChange = { context = it },
            label = { Text("Additional Context (Optional)") },
            placeholder = { Text("Any extra details about your task...") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Theme Selection
        Text(
            text = "Quest Theme",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        themes.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (themeKey, themeLabel) ->
                    FilterChip(
                        onClick = { selectedTheme = themeKey },
                        selected = selectedTheme == themeKey,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(themeLabel)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Difficulty Selection
        Text(
            text = "Difficulty Level: $selectedDifficulty",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        
        Slider(
            value = selectedDifficulty.toFloat(),
            onValueChange = { selectedDifficulty = it.toInt() },
            valueRange = 1f..5f,
            steps = 3,
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Easy", fontSize = 12.sp)
            Text("Normal", fontSize = 12.sp)
            Text("Hard", fontSize = 12.sp)
            Text("Epic", fontSize = 12.sp)
            Text("Legendary", fontSize = 12.sp)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Generate Button
        Button(
            onClick = {
                if (todoText.isNotBlank()) {
                    coroutineScope.launch {
                        viewModel.generateQuest(
                            QuestGenerationRequest(
                                todoText = todoText,
                                context = context.ifBlank { null },
                                difficultyPreference = selectedDifficulty,
                                themePreference = selectedTheme
                            )
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = todoText.isNotBlank() && !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colors.onPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Generate Epic Quest", fontSize = 16.sp)
            }
        }
        
        // Random Quest Button
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = {
                val randomTodos = listOf(
                    "Clean my room",
                    "Learn a new programming language",
                    "Exercise for 30 minutes", 
                    "Call my grandparents",
                    "Organize my files",
                    "Read a book",
                    "Cook a healthy meal",
                    "Practice meditation"
                )
                todoText = randomTodos.random()
                selectedTheme = themes.random().first
                selectedDifficulty = (2..4).random()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Casino,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Random Quest")
        }
        
        // Error Display
        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        // Generated Quest Display
        uiState.generatedQuest?.let { quest ->
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = quest.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Text(
                            text = "⭐ ${quest.difficulty}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colors.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = quest.description,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    
                    if (quest.storyContext != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Card(
                            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = quest.storyContext,
                                fontSize = 12.sp,
                                style = MaterialTheme.typography.caption,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Rewards
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "🏆 ${quest.rewardExperience} XP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Text(
                            text = "🏷️ ${quest.questType}",
                            fontSize = 12.sp
                        )
                    }
                    
                    // Tasks
                    if (quest.tasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Quest Tasks:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        quest.tasks.forEach { task ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                elevation = 2.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = task.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    Text(
                                        text = task.description,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                    )
                                    
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "⭐ ${task.difficulty}",
                                            fontSize = 10.sp
                                        )
                                        
                                        Text(
                                            text = "🏆 ${task.experienceReward} XP",
                                            fontSize = 10.sp
                                        )
                                        
                                        task.estimatedDuration?.let { duration ->
                                            Text(
                                                text = "⏱️ ${duration}m",
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                // TODO: Implement create quest functionality
                                // This would call the API to actually create the quest
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Create Quest")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                // TODO: Implement regenerate functionality
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Regenerate")
                        }
                    }
                }
            }
        }
    }
}