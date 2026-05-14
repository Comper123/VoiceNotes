package com.example.voicenot

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.voicenot.audio.AudioRecorder
import com.example.voicenot.model.database.VoiceNoteDatabase
import com.example.voicenot.model.entities.VoiceNoteEntity as VoiceNote
import com.example.voicenot.model.entities.FolderEntity as Folder
import com.example.voicenot.ui.theme.VoiceNotesTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.lazy.LazyRow
import android.util.Log
import com.example.voicenot.model.entities.VoiceNoteEntity
import android.widget.Toast
import androidx.navigation.NavHostController

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VoiceNotesTheme {
                val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                } else {
                    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                }

                if (!hasPermission) {
                    PermissionScreen {
                        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_MEDIA_AUDIO)
                        } else {
                            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                        requestPermissionLauncher.launch(permissions)
                    }
                } else {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun PermissionScreen(onRequest: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Нужны разрешения для записи аудио")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRequest) {
                Text("Предоставить разрешения")
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Отслеживаем текущий маршрут для подсветки вкладки
    var selectedTab by remember { mutableStateOf(0) }

    // Слушаем изменения навигации
    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            selectedTab = when (destination.route) {
                "notes" -> 0
                "folders" -> 1
                "recorder" -> 2
                "profile" -> 3
                else -> selectedTab
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val items = listOf(
                    "Заметки" to Icons.Default.Note,
                    "Папки" to Icons.Default.Folder,
                    "Запись" to Icons.Default.Mic,
                    "Профиль" to Icons.Default.Person
                )
                items.forEachIndexed { index, (title, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            // Навигация при клике на вкладку
                            when (index) {
                                0 -> navController.navigate("notes") {
                                    popUpTo("notes") { inclusive = true }
                                    launchSingleTop = true
                                }
                                1 -> navController.navigate("folders") {
                                    popUpTo("folders") { inclusive = true }
                                    launchSingleTop = true
                                }
                                2 -> navController.navigate("recorder") {
                                    popUpTo("recorder") { inclusive = true }
                                    launchSingleTop = true
                                }
                                3 -> navController.navigate("profile") {
                                    popUpTo("profile") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = { Icon(icon, contentDescription = title) },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(navController, startDestination = "notes") {
                composable("notes") {
                    NotesScreen(navController)
                }
                composable("folders") {
                    FoldersScreen()
                }
                composable("recorder") {
                    RecorderScreen(navController)
                }
                composable("profile") {
                    ProfileScreen()
                }
                composable("player/{noteId}") { backStackEntry ->
                    val noteId = backStackEntry.arguments?.getString("noteId")?.toLongOrNull()
                    if (noteId != null) {
                        PlayerScreen(noteId, onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(navController: androidx.navigation.NavHostController) {
    val context = LocalContext.current
    val database = remember { VoiceNoteDatabase.getInstance(context) }
    val noteDao = remember { database.voiceNoteDao() }
    val folderDao = remember { database.folderDao() }
    val scope = rememberCoroutineScope()

    val notes by noteDao.getAllNotes().collectAsStateWithLifecycle(initialValue = emptyList())
    val folders by folderDao.getAllFolders().collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedFolderId by remember { mutableStateOf(0L) }
    var showFolderFilter by remember { mutableStateOf(false) }

    val filteredNotes = if (selectedFolderId == 0L) notes else notes.filter { it.folderId == selectedFolderId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои заметки", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Красивый фильтр по папкам (горизонтальный скролл)
            if (folders.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Кнопка "Все заметки"
                    item {
                        FilterChip(
                            selected = selectedFolderId == 0L,
                            onClick = { selectedFolderId = 0L },
                            label = { Text("Все") },
                            leadingIcon = {
                                Icon(Icons.Default.AllInbox, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }

                    items(folders) { folder ->
                        FilterChip(
                            selected = selectedFolderId == folder.id,
                            onClick = { selectedFolderId = folder.id },
                            label = { Text(folder.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(folder.color)))
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(android.graphics.Color.parseColor(folder.color)).copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredNotes) { note ->
                    NoteCard(
                        note = note,
                        onClick = { navController.navigate("player/${note.id}") },
                        onDelete = {
                            scope.launch {
                                noteDao.deleteNote(note)
                                File(note.filePath).delete()
                            }
                        }
                    )
                }

                if (filteredNotes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Нет заметок. Нажмите на вкладку \"Запись\" чтобы создать первую заметку")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteCard(note: VoiceNote, onClick: () -> Unit, onDelete: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifEmpty { "Заметка" },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Text(
                        text = formatDuration(note.duration),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = dateFormat.format(note.createdAt),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(noteId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val database = remember { VoiceNoteDatabase.getInstance(context) }
    val noteDao = remember { database.voiceNoteDao() }
    var note by remember { mutableStateOf<VoiceNoteEntity?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val handler = remember { Handler(Looper.getMainLooper()) }

    LaunchedEffect(noteId) {
        note = noteDao.getNoteById(noteId)
        note?.let {
            val file = File(it.filePath)
            Log.d("Player", "Загружена заметка: ${it.title}")
            Log.d("Player", "Путь: ${it.filePath}")
            Log.d("Player", "Файл существует: ${file.exists()}")
            Log.d("Player", "Размер файла: ${file.length()} байт")

            if (!file.exists()) {
                errorMessage = "Файл не найден: ${it.filePath}"
            }
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val updateRunnable = object : Runnable {
                override fun run() {
                    mediaPlayer?.let {
                        currentPosition = it.currentPosition
                        handler.postDelayed(this, 100)
                    }
                }
            }
            handler.post(updateRunnable)
        } else {
            handler.removeCallbacksAndMessages(null)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            handler.removeCallbacksAndMessages(null)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Плеер", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (errorMessage != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(errorMessage!!, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                }
                Button(onClick = onBack) { Text("Назад") }
                return@Scaffold
            }

            if (note == null) {
                CircularProgressIndicator()
                return@Scaffold
            }

            // Иконка
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
            }

            Text(note!!.title.ifEmpty { "Заметка" }, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(formatDuration(note!!.duration), fontSize = 14.sp, color = Color.Gray)
            Text(SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(note!!.createdAt), fontSize = 14.sp, color = Color.Gray)

            // Кнопка воспроизведения
            Button(
                onClick = {
                    try {
                        if (mediaPlayer == null) {
                            val file = File(note!!.filePath)
                            if (!file.exists()) {
                                errorMessage = "Файл не найден: ${note!!.filePath}"
                                return@Button
                            }
                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(note!!.filePath)
                                prepare()
                                start()
                                setOnCompletionListener {
                                    isPlaying = false
                                    currentPosition = 0
                                }
                            }
                            isPlaying = true
                        } else {
                            if (isPlaying) {
                                mediaPlayer?.pause()
                                isPlaying = false
                            } else {
                                mediaPlayer?.start()
                                isPlaying = true
                            }
                        }
                    } catch (e: Exception) {
                        errorMessage = "Ошибка: ${e.message}"
                        Log.e("Player", "Ошибка", e)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isPlaying) "Пауза" else "Воспроизвести")
            }

            // Прогресс (если есть)
            if (note!!.duration > 0) {
                LinearProgressIndicator(
                    progress = currentPosition.toFloat() / note!!.duration.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatDuration(currentPosition.toLong()), fontSize = 12.sp, color = Color.Gray)
                    Text(formatDuration(note!!.duration), fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen() {
    val context = LocalContext.current
    val database = remember { VoiceNoteDatabase.getInstance(context) }
    val folderDao = remember { database.folderDao() }
    val scope = rememberCoroutineScope()

    val folders by folderDao.getAllFolders().collectAsStateWithLifecycle(initialValue = emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#6200EE") }

    val colors = listOf("#6200EE", "#FF5722", "#4CAF50", "#2196F3", "#9C27B0", "#FF9800", "#E91E63", "#00BCD4")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Папки", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Новая папка")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(folders) { folder ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(android.graphics.Color.parseColor(folder.color)).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(folder.color))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(folder.name, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                        }
                        IconButton(onClick = {
                            scope.launch {
                                folderDao.deleteFolder(folder)
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Red.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            if (folders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Нет папок. Нажмите + чтобы создать")
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Новая папка") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Название папки") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Выберите цвет:", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(colors) { color ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(color)))
                                    .clickable { selectedColor = color },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColor == color) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank()) {
                        scope.launch {
                            folderDao.insertFolder(Folder(name = newFolderName, color = selectedColor, createdAt = Date()))
                        }
                        showDialog = false
                        newFolderName = ""
                        selectedColor = "#6200EE"
                    }
                }) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecorderScreen(navController: NavHostController) {
    val context = LocalContext.current
    val database = remember { VoiceNoteDatabase.getInstance(context) }
    val noteDao = remember { database.voiceNoteDao() }
    val audioRecorder = remember { AudioRecorder(context) }
    val scope = rememberCoroutineScope()

    var isRecording by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf(0L) }
    var noteTitle by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ✅ Исправлено: обновляем duration каждые 100ms
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(100)
                val currentDuration = audioRecorder.getCurrentDuration()
                Log.d("Recorder", "Текущая длительность: $currentDuration ms")
                duration = currentDuration
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Запись", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Таймер (показывает реальную длительность)
            Text(
                text = formatDuration(duration),
                fontSize = 56.sp,
                color = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = noteTitle,
                onValueChange = { noteTitle = it },
                label = { Text("Название заметки") },
                placeholder = { Text("Введите название...") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRecording,
                singleLine = true
            )

            Button(
                onClick = {
                    if (isRecording) {
                        val filePath = audioRecorder.stopRecording()
                        Log.d("Recorder", "Запись остановлена. Длительность: $duration ms")
                        if (filePath != null) {
                            scope.launch {
                                val finalDuration = duration  // Сохраняем финальную длительность
                                val note = VoiceNoteEntity(
                                    title = noteTitle.ifEmpty { "Заметка" },
                                    filePath = filePath,
                                    duration = finalDuration,  // ✅ Исправлено: сохраняем реальную длительность
                                    fileSize = File(filePath).length(),
                                    createdAt = Date(),
                                    updatedAt = Date(),
                                    folderId = 0,
                                    tags = "",
                                    content = "",
                                    isFavorite = false
                                )
                                val id = noteDao.insertNote(note)
                                Log.d("Recorder", "Сохранена заметка id:$id, duration:$finalDuration")
                                Toast.makeText(context, "Запись сохранена!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            errorMessage = "Ошибка сохранения"
                        }
                        isRecording = false
                        duration = 0
                        noteTitle = ""
                        navController.popBackStack()
                    } else {
                        if (audioRecorder.startRecording()) {
                            isRecording = true
                            duration = 0
                            errorMessage = null
                            Toast.makeText(context, "Запись начата", Toast.LENGTH_SHORT).show()
                        } else {
                            errorMessage = "Не удалось начать запись"
                        }
                    }
                },
                modifier = Modifier.size(100.dp, 100.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isRecording) "⏹" else "🎤", fontSize = 48.sp)
            }

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    val dir = context.filesDir
                    val recordingsDir = File(dir, "recordings")
                    val files = recordingsDir.listFiles() ?: emptyArray()
                    Toast.makeText(
                        context,
                        "Файлов: ${files.size}\nПуть: ${recordingsDir.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            ) {
                Text("Проверить папку")
            }
        }
    }
}

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val database = remember { VoiceNoteDatabase.getInstance(context) }
    val noteDao = remember { database.voiceNoteDao() }
    val folderDao = remember { database.folderDao() }

    val notes by noteDao.getAllNotes().collectAsStateWithLifecycle(initialValue = emptyList())
    val folders by folderDao.getAllFolders().collectAsStateWithLifecycle(initialValue = emptyList())

    val totalSize = remember(notes) { notes.sumOf { it.fileSize } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(50.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Статистика", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("Всего заметок", notes.size)
                    StatItem("Папок", folders.size)
                    StatItem("Избранных", notes.count { it.isFavorite })
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    StatItem("Размер", formatFileSize(totalSize))
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun StatItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))
    return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}