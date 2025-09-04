package com.irlquest.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.irlquest.app.ui.viewmodel.QuestViewModel

@Composable
fun QuestsScreen(viewModel: QuestViewModel = QuestViewModel()) {
    val loading by viewModel.loading.collectAsState()
    val quests by viewModel.quests.collectAsState()
    val error by viewModel.error.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("1") }

    LaunchedEffect(Unit) {
        viewModel.loadQuests()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Quests", style = MaterialTheme.typography.h5)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (error != null) {
            Text(text = error ?: "", color = MaterialTheme.colors.error)
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(quests) { q ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = q.title, style = MaterialTheme.typography.subtitle1)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Difficulty: ${q.difficulty}", style = MaterialTheme.typography.caption)
                        if (!q.description.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = q.description!!, style = MaterialTheme.typography.body2)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        FloatingActionButton(onClick = { showDialog = true }, modifier = Modifier.align(Alignment.End)) {
            Text(text = "+")
        }
    }

    if (showDialog) {
        AlertDialog(onDismissRequest = { showDialog = false }, title = { Text("Create quest") }, text = {
            Column {
                OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text("Title") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = newDesc, onValueChange = { newDesc = it }, label = { Text("Description") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = difficulty, onValueChange = { difficulty = it }, label = { Text("Difficulty") })
            }
        }, confirmButton = {
            TextButton(onClick = {
                val diff = difficulty.toIntOrNull() ?: 1
                viewModel.createQuest(newTitle, if (newDesc.isBlank()) null else newDesc, diff) {
                    showDialog = false
                    newTitle = ""
                    newDesc = ""
                    difficulty = "1"
                }
            }) { Text("Create") }
        }, dismissButton = {
            TextButton(onClick = { showDialog = false }) { Text("Cancel") }
        })
    }
}

