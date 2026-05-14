//package com.example.voicenot.view.screens
//
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavHostController
//import com.example.voicenot.view.components.NoteCard
//import com.example.voicenot.viewmodel.MainViewModel
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun NotesListScreen(
//    navController: NavHostController,
//    viewModel: MainViewModel
//) {
//    val notes by viewModel.notes.collectAsState()
//    val folders by viewModel.folders.collectAsState()
//    var showCreateDialog by remember { mutableStateOf(false) }
//    var newNoteTitle by remember { mutableStateOf("") }
//    var selectedFolderId by remember { mutableStateOf(0L) }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Мои заметки") },
//                actions = {
//                    IconButton(onClick = { showCreateDialog = true }) {
//                        Icon(Icons.Default.Add, contentDescription = "Новая заметка")
//                    }
//                }
//            )
//        },
//        floatingActionButton = {
//            FloatingActionButton(
//                onClick = { navController.navigate("recorder") }
//            ) {
//                Icon(Icons.Default.Mic, contentDescription = "Записать")
//            }
//        }
//    ) { paddingValues ->
//        LazyColumn(
//            modifier = Modifier.padding(paddingValues),
//            contentPadding = PaddingValues(16.dp),
//            verticalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            items(notes) { note ->
//                NoteCard(note = note, onClick = {})
//            }
//        }
//    }
//
//    if (showCreateDialog) {
//        AlertDialog(
//            onDismissRequest = { showCreateDialog = false },
//            title = { Text("Новая заметка") },
//            text = {
//                Column {
//                    OutlinedTextField(
//                        value = newNoteTitle,
//                        onValueChange = { newNoteTitle = it },
//                        label = { Text("Название") },
//                        modifier = Modifier.fillMaxWidth()
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Text("Папка:")
//                    folders.forEach { folder ->
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .clickable { selectedFolderId = folder.id }
//                                .padding(8.dp),
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            RadioButton(
//                                selected = selectedFolderId == folder.id,
//                                onClick = { selectedFolderId = folder.id }
//                            )
//                            Text(folder.name)
//                        }
//                    }
//                }
//            },
//            confirmButton = {
//                TextButton(onClick = {
//                    if (newNoteTitle.isNotBlank()) {
//                        viewModel.createNote(newNoteTitle, selectedFolderId)
//                        showCreateDialog = false
//                        newNoteTitle = ""
//                    }
//                }) {
//                    Text("Создать")
//                }
//            },
//            dismissButton = {
//                TextButton(onClick = { showCreateDialog = false }) {
//                    Text("Отмена")
//                }
//            }
//        )
//    }
//}