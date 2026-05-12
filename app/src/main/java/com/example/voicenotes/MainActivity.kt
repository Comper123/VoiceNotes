package com.yourapp.voicenotes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.voicenotes.presentation.theme.VoiceNotesTheme
import com.example.voicenotes.view.
import com.yourapp.voicenotes.view.screens.PlayerScreen
import com.yourapp.voicenotes.view.screens.RecorderScreen
import com.yourapp.voicenotes.viewModel.NotesListViewModel
import com.yourapp.voicenotes.viewModel.PlayerViewModel
import com.yourapp.voicenotes.viewModel.RecorderViewModel

class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appContainer = (application as VoiceNotesApplication).appContainer

        setContent {
            VoiceNotesTheme {
                val navController = rememberNavController()

                // Проверка разрешений
                val hasRecordPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                }

                if (!hasRecordPermission) {
                    // Запрос разрешений
                    PermissionScreen(
                        onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                requestPermissions(
                                    arrayOf(
                                        Manifest.permission.RECORD_AUDIO,
                                        Manifest.permission.READ_MEDIA_AUDIO
                                    ),
                                    REQUEST_RECORD_AUDIO_PERMISSION
                                )
                            } else {
                                requestPermissions(
                                    arrayOf(
                                        Manifest.permission.RECORD_AUDIO,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    ),
                                    REQUEST_RECORD_AUDIO_PERMISSION
                                )
                            }
                        }
                    )
                } else {
                    NavHost(navController, startDestination = "notes_list") {
                        composable("notes_list") {
                            NotesListScreen(
                                viewModel = NotesListViewModel(
                                    repository = appContainer.repository
                                ),
                                onNavigateToPlayer = { noteId ->
                                    navController.navigate("player/$noteId")
                                },
                                onNavigateToRecorder = {
                                    navController.navigate("recorder")
                                }
                            )
                        }

                        composable("recorder") {
                            RecorderScreen(
                                viewModel = RecorderViewModel(
                                    repository = appContainer.repository,
                                    audioRecorder = appContainer.audioRecorder
                                ),
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("player/{noteId}") { backStackEntry ->
                            val noteId = backStackEntry.arguments?.getString("noteId")?.toLongOrNull()
                            if (noteId != null) {
                                PlayerScreen(
                                    viewModel = PlayerViewModel(
                                        repository = appContainer.repository
                                    ),
                                    noteId = noteId,
                                    onBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение получено, перезапускаем Activity
                recreate()
            }
        }
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 100
    }
}

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Scaffold(
        modifier = androidx.compose.ui.Modifier.fillMaxSize()
    ) { paddingValues ->
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Для работы приложения нужны разрешения",
                    modifier = androidx.compose.ui.Modifier.padding(16.dp)
                )
                Button(onClick = onRequestPermission) {
                    Text("Предоставить разрешения")
                }
            }
        }
    }
}