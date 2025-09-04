package com.irlquest.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.irlquest.app.ui.viewmodel.TaskViewModel
import com.irlquest.app.ui.viewmodel.AuthViewModel

@Composable
fun TasksScreen(onNavigateToQuests: () -> Unit, onLogout: () -> Unit = {}, viewModel: TaskViewModel = TaskViewModel(), authViewModel: AuthViewModel = AuthViewModel()) {
    val loading by viewModel.loading.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val error by viewModel.error.collectAsState()

    val currentUser by authViewModel.currentUser.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        authViewModel.loadCurrentUser()
        viewModel.loadTasks()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = "Tasks", style = MaterialTheme.typography.h5)
                if (currentUser != null) {
                    Text(text = "${currentUser!!.username} · ${currentUser!!.email}", style = MaterialTheme.typography.caption)
                }
            }
            Row {
                Button(onClick = { onNavigateToQuests() }) { Text("Quests") }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { onLogout() }) { Text("Logout") }
            }
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
            items(tasks) { t ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = t.title, style = MaterialTheme.typography.subtitle1)
                                if (!t.description.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = t.description!!, style = MaterialTheme.typography.body2)
                                }
                            }
                            Row {
                                val doneText = if (t.completed) "Undo" else "Done"
                                TextButton(onClick = { viewModel.updateTask(t.id, completed = !t.completed) }) {
                                    Text(doneText)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                TextButton(onClick = { viewModel.deleteTask(t.id) }) {
                                    Text("Delete")
                                }
                            }
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
        AlertDialog(onDismissRequest = { showDialog = false }, title = { Text("Create task") }, text = {
            Column {
                OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text("Title") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = newDesc, onValueChange = { newDesc = it }, label = { Text("Description") })
            }
        }, confirmButton = {
            TextButton(onClick = {
                viewModel.createTask(newTitle, if (newDesc.isBlank()) null else newDesc) {
                    showDialog = false
                    newTitle = ""
                    newDesc = ""
                }
            }) { Text("Create") }
        }, dismissButton = {
            TextButton(onClick = { showDialog = false }) { Text("Cancel") }
        })
    }
}
