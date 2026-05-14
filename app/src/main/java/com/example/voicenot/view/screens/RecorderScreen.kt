//package com.example.voicenot.view.screens
//
//import android.Manifest
//import android.content.pm.PackageManager
//import android.os.Build
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.example.voicenot.view.components.RecordButton
//import com.example.voicenot.view.components.TimerDisplay
//import com.example.voicenot.viewmodel.RecorderViewModel
//
//
//@OptIn(ExperimentalMaterial3Api::class)
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun RecorderScreen(navController: androidx.navigation.NavHostController) {
//    val context = LocalContext.current
//    val database = remember { VoiceNoteDatabase.getInstance(context) }
//    val noteDao = remember { database.voiceNoteDao() }
//    val audioRecorder = remember { AudioRecorder(context) }
//    val scope = rememberCoroutineScope()
//
//    var isRecording by remember { mutableStateOf(false) }
//    var duration by remember { mutableStateOf(0L) }
//    var noteTitle by remember { mutableStateOf("") }
//    var errorMessage by remember { mutableStateOf<String?>(null) }
//
//    LaunchedEffect(isRecording) {
//        if (isRecording) {
//            while (isRecording) {
//                delay(100)
//                duration = audioRecorder.getCurrentDuration()
//            }
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Запись", fontWeight = FontWeight.Bold) },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primaryContainer
//                )
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//                .padding(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            // Таймер
//            Text(
//                text = formatDuration(duration),
//                fontSize = 56.sp,
//                fontFamily = FontFamily.Monospace,
//                color = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
//            )
//
//            // Поле для названия
//            OutlinedTextField(
//                value = noteTitle,
//                onValueChange = { noteTitle = it },
//                label = { Text("Название заметки") },
//                placeholder = { Text("Введите название...") },
//                modifier = Modifier.fillMaxWidth(),
//                enabled = !isRecording,
//                singleLine = true
//            )
//
//            // Кнопка записи
//            Button(
//                onClick = {
//                    if (isRecording) {
//                        // Останавливаем запись
//                        val filePath = audioRecorder.stopRecording()
//                        if (filePath != null) {
//                            scope.launch {
//                                val note = VoiceNoteEntity(
//                                    title = noteTitle.ifEmpty { "Заметка ${formatDuration(duration)}" },
//                                    filePath = filePath,
//                                    duration = duration,
//                                    fileSize = File(filePath).length(),
//                                    createdAt = Date(),
//                                    updatedAt = Date(),
//                                    folderId = 0,
//                                    tags = "",
//                                    content = "",
//                                    isFavorite = false
//                                )
//                                val id = noteDao.insertNote(note)
//                                Log.d("Recorder", "Сохранена заметка с id: $id")
//                                Toast.makeText(context, "Запись сохранена!", Toast.LENGTH_SHORT).show()
//                            }
//                        } else {
//                            errorMessage = "Ошибка сохранения"
//                            Toast.makeText(context, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
//                        }
//                        isRecording = false
//                        duration = 0
//                        noteTitle = ""
//                        navController.popBackStack()
//                    } else {
//                        // Начинаем запись
//                        if (audioRecorder.startRecording()) {
//                            isRecording = true
//                            errorMessage = null
//                            Toast.makeText(context, "Запись начата", Toast.LENGTH_SHORT).show()
//                        } else {
//                            errorMessage = "Не удалось начать запись"
//                            Toast.makeText(context, "Не удалось начать запись", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                },
//                modifier = Modifier.size(100.dp, 100.dp),
//                shape = CircleShape,
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
//                )
//            ) {
//                Text(if (isRecording) "⏹" else "🎤", fontSize = 48.sp)
//            }
//
//            errorMessage?.let {
//                Text(it, color = MaterialTheme.colorScheme.error)
//            }
//
//            // Кнопка проверки папки
//            Button(
//                onClick = {
//                    val dir = context.filesDir
//                    val recordingsDir = File(dir, "recordings")
//                    val files = recordingsDir.listFiles() ?: emptyArray()
//                    Toast.makeText(
//                        context,
//                        "Файлов: ${files.size}\nПуть: ${recordingsDir.absolutePath}",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            ) {
//                Text("Проверить папку")
//            }
//        }
//    }
//}