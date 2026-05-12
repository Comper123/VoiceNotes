package com.example.voicenot.view.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.voicenot.view.components.VoiceNoteCard
import com.example.voicenot.viewmodel.NotesListEvent
import com.example.voicenot.viewmodel.NotesListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    viewModel: NotesListViewModel,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToRecorder: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Голосовые заметки") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToRecorder) {
                Icon(Icons.Default.Mic, contentDescription = "Записать")
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error!!)
                        Button(onClick = { viewModel.onEvent(NotesListEvent.Retry) }) {
                            Text("Повторить")
                        }
                    }
                }
            }

            uiState.notes.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет заметок. Нажмите + для записи")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.notes, key = { it.id }) { note ->
                        VoiceNoteCard(
                            note = note,
                            onClick = { onNavigateToPlayer(note.id) },
                            onFavoriteClick = { viewModel.onEvent(NotesListEvent.ToggleFavorite(note)) },
                            onDeleteClick = { viewModel.onEvent(NotesListEvent.DeleteNote(note)) }
                        )
                    }
                }
            }
        }
    }
}