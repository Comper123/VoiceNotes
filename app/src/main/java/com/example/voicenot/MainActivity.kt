package com.example.voicenot

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.voicenot.di.AppContainer
import com.example.voicenot.view.screens.NotesListScreen
import com.example.voicenot.view.screens.PlayerScreen
import com.example.voicenot.view.screens.RecorderScreen
import com.example.voicenot.view.theme.VoiceNotesTheme
import com.example.voicenot.viewmodel.NotesListViewModel
import com.example.voicenot.viewmodel.PlayerViewModel
import com.example.voicenot.viewmodel.RecorderViewModel

class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appContainer = AppContainer(applicationContext)

        setContent {
            VoiceNotesTheme {
                val navController = rememberNavController()

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
                    PermissionScreen(
                        onRequestPermission = {
                            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(
                                    Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.READ_MEDIA_AUDIO
                                )
                            } else {
                                arrayOf(
                                    Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                )
                            }
                            requestPermissionLauncher.launch(permissions)
                        }
                    )
                } else {
                    NavHost(navController, startDestination = "notes_list") {
                        composable("notes_list") {
                            NotesListScreen(
                                viewModel = NotesListViewModel(appContainer.repository),
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
                                    appContainer.repository,
                                    appContainer.audioRecorder
                                ),
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("player/{noteId}") { backStackEntry ->
                            val noteId = backStackEntry.arguments?.getString("noteId")?.toLongOrNull()
                            if (noteId != null) {
                                PlayerScreen(
                                    viewModel = PlayerViewModel(appContainer.repository),
                                    noteId = noteId,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Для работы приложения нужны разрешения")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRequestPermission) {
                    Text("Предоставить разрешения")
                }
            }
        }
    }
}