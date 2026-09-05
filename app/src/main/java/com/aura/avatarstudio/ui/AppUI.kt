package com.aura.avatarstudio.ui

import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.aura.avatarstudio.GltfAvatarView
import com.aura.avatarstudio.data.AvatarDatabase
import com.aura.avatarstudio.data.AvatarPreset
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun AppUI(modifier: Modifier = Modifier) {
    var topTab by remember { mutableStateOf("BUILDER") }
    val avatarState = remember { AvatarState() }
    CompositionLocalProvider(LocalAvatarState provides avatarState) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { if (avatarState.hasPlayedStartupVideo) TopHeader(topTab) { topTab = it } },
            bottomBar = { if (avatarState.hasPlayedStartupVideo) BottomActionPanel() }
        ) { innerPadding ->
            Box(
                Modifier.padding(if (avatarState.hasPlayedStartupVideo) innerPadding else PaddingValues(0.dp)).fillMaxSize()
            ) {
                when (topTab) {
                    "BUILDER" -> BuilderMode()
                    "PRESETS" -> PresetsMode()
                    "CHAT" -> ChatMode()
                    "IMPORT" -> SettingsScreen()
                }
            }
        }
    }
}

@Composable
fun TopHeader(selected: String, onTabSelected: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.padding(horizontal = 12.dp)) {
            Text("AVATAR DESIGN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("CREATE YOUR IDENTITY", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp)
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            listOf("BUILDER", "PRESETS", "CHAT", "IMPORT").forEach { tab ->
                val selectedTab = selected == tab
                Box(
                    Modifier.clickable { onTabSelected(tab) }
                        .background(if (selectedTab) MaterialTheme.colorScheme.primary.copy(alpha = .2f) else Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(tab, color = if (selectedTab) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
fun BottomActionPanel() {
    val context = LocalContext.current
    val state = LocalAvatarState.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                        val json = Json { ignoreUnknownKeys = true }.decodeFromString<JsonObject>(reader.readText())
                        json["clothingIndex"]?.jsonPrimitive?.intOrNull?.let { state.clothingIndex = it }
                        json["gender"]?.jsonPrimitive?.intOrNull?.let { state.gender = it }
                        json["hairStyleIndex"]?.jsonPrimitive?.intOrNull?.let { state.hairStyleIndex = it }
                        json["eyeShapeIndex"]?.jsonPrimitive?.intOrNull?.let { state.eyeShapeIndex = it }
                        json["augmentsIndex"]?.jsonPrimitive?.intOrNull?.let { state.augmentsIndex = it }
                        json["tattoosIndex"]?.jsonPrimitive?.intOrNull?.let { state.tattoosIndex = it }
                    }
                }
            }
        }
    }
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text("AVATAR ID", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("MATRIX_07_8X9A", fontSize = 10.sp, color = Color.White, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
            OutlinedButton(
                onClick = { launcher.launch(arrayOf("application/json", "text/plain")) },
                modifier = Modifier.height(38.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                shape = RoundedCornerShape(7.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) { Text("LOAD OUTFIT", fontSize = 10.sp) }
            Button(
                onClick = { shareImage(context) },
                modifier = Modifier.height(38.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                shape = RoundedCornerShape(7.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("SAVE AVATAR", fontSize = 10.sp) }
        }
    }
}

@Composable
fun BuilderMode() {
    var activeCategory by remember { mutableStateOf("APPEARANCE") }
    var aiPrompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var base64Image by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val state = LocalAvatarState.current
    val context = LocalContext.current
    var isStartupVideo by remember { mutableStateOf(!state.hasPlayedStartupVideo) }
    var startupMessage by remember { mutableStateOf("INITIALIZING NEURAL LINK...") }

    LaunchedEffect(isStartupVideo) {
        if (isStartupVideo) {
            val atmospheres = listOf("Neon Cityscape", "Vampire Lair", "Fiery Hellscape", "Neutral Studio")
            val messages = listOf("CALIBRATING ARMOR...", "SYNCING ATMOSPHERE...", "LOADING NEURO-OPTICS...", "BOOT SEQUENCE COMPLETE")
            for (i in 0..3) {
                startupMessage = messages[i]
                state.atmosphere = atmospheres[i % atmospheres.size]
                state.clothingIndex = i
                state.avatarView?.updateAppearance(state.skinTone, state.eyeColor, state.hairColor, state.atmosphere)
                kotlinx.coroutines.delay(700)
            }
            state.atmosphere = "Neutral Studio"
            state.clothingIndex = 0
            state.avatarView?.updateAppearance(state.skinTone, state.eyeColor, state.hairColor, state.atmosphere)
            state.hasPlayedStartupVideo = true
            isStartupVideo = false
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { base64Image = Base64.encodeToString(it.readBytes(), Base64.NO_WRAP) }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (!isStartupVideo) {
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { launcher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.size(38.dp), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (base64Image != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                ) { Text(if (base64Image != null) "✓" else "+", fontSize = 15.sp, color = Color.White) }
                Spacer(Modifier.width(6.dp))
                OutlinedTextField(
                    value = aiPrompt, onValueChange = { aiPrompt = it }, modifier = Modifier.weight(1f),
                    placeholder = { Text("e.g. Corporate Netrunner...", fontSize = 11.sp) }, singleLine = true,
                    shape = RoundedCornerShape(7.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                )
                Spacer(Modifier.width(6.dp))
                Button(
                    onClick = {
                        isGenerating = true
                        scope.launch {
                            try {
                                val resultJson = com.aura.avatarstudio.api.generateAvatarConfigWithImage(aiPrompt, base64Image)
                                val config = Json { ignoreUnknownKeys = true }.decodeFromString<JsonObject>(resultJson)
                                config["gender"]?.jsonPrimitive?.intOrNull?.let { state.gender = it }
                                config["headShape"]?.jsonPrimitive?.floatOrNull?.let { state.headShape = it }
                                config["age"]?.jsonPrimitive?.floatOrNull?.let { state.age = it }
                                config["hairStyleIndex"]?.jsonPrimitive?.intOrNull?.let { state.hairStyleIndex = it }
                                config["clothingIndex"]?.jsonPrimitive?.intOrNull?.let { state.clothingIndex = it }
                                config["eyeShapeIndex"]?.jsonPrimitive?.intOrNull?.let { state.eyeShapeIndex = it }
                                config["augmentsIndex"]?.jsonPrimitive?.intOrNull?.let { state.augmentsIndex = it }
                                config["tattoosIndex"]?.jsonPrimitive?.intOrNull?.let { state.tattoosIndex = it }
                                state.avatarView?.updateAppearance(state.skinTone, state.eyeColor, state.hairColor, state.atmosphere)
                            } catch (e: Exception) {
                                com.aura.avatarstudio.util.NetworkErrorHandler.handleError(context, e)
                            } finally {
                                isGenerating = false
                                base64Image = null
                            }
                        }
                    },
                    enabled = !isGenerating && (aiPrompt.isNotBlank() || base64Image != null),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isGenerating) CircularProgressIndicator(Modifier.size(15.dp), color = Color.White) else Text("AUTO-GEN", fontSize = 10.sp)
                }
            }
        }

        Box(Modifier.fillMaxWidth().weight(1f)) {
            AndroidView(
                factory = { GltfAvatarView(it, "avatars/my_avatar.glb").also { view -> state.avatarView = view } },
                modifier = Modifier.fillMaxSize()
            )
            if (isStartupVideo) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .6f)), contentAlignment = Alignment.Center) {
                    Text(startupMessage, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, textAlign = TextAlign.Center)
                }
            } else {
                Column(
                    Modifier.align(Alignment.CenterEnd).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .8f),
                        modifier = Modifier.size(38.dp).clickable { state.avatarView?.rotateCamera(90f, 0f) }
                    ) { Box(contentAlignment = Alignment.Center) { Text("↻", color = Color.White, fontSize = 18.sp) } }
                    Text("ROTATE", fontSize = 7.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .8f),
                        modifier = Modifier.size(38.dp).clickable { state.avatarView?.zoomCamera(.85f) }
                    ) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 18.sp) } }
                    Text("ZOOM", fontSize = 7.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .8f),
                        modifier = Modifier.size(38.dp).clickable { state.avatarView?.zoomCamera(1.15f) }
                    ) { Box(contentAlignment = Alignment.Center) { Text("−", color = Color.White, fontSize = 18.sp) } }
                    Text("ZOOM OUT", fontSize = 7.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = .8f),
                        modifier = Modifier.size(38.dp).clickable {
                            scope.launch(Dispatchers.IO) {
                                AvatarDatabase.getDatabase(context).avatarDao().insertPreset(
                                    AvatarPreset(
                                        name = "Preset ${System.currentTimeMillis() % 1000}", gender = state.gender,
                                        headShape = state.headShape, age = state.age, hairStyleIndex = state.hairStyleIndex,
                                        height = state.height, build = state.build, jaw = state.jaw, cheek = state.cheek,
                                        clothingIndex = state.clothingIndex, eyeShapeIndex = state.eyeShapeIndex,
                                        augmentsIndex = state.augmentsIndex, tattoosIndex = state.tattoosIndex
                                    )
                                )
                            }
                        }
                    ) { Box(contentAlignment = Alignment.Center) { Text("✓", color = Color.White) } }
                    Text("SAVE", fontSize = 7.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    Modifier.align(Alignment.BottomCenter).padding(8.dp)
                        .clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .8f)).padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    (0..4).forEach { index ->
                        Box(
                            Modifier.size(25.dp).clip(RoundedCornerShape(5.dp))
                                .background(if (state.clothingIndex == index) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { state.clothingIndex = index },
                            contentAlignment = Alignment.Center
                        ) { Text("${index + 1}", color = Color.White, fontSize = 9.sp) }
                    }
                }
            }
        }

        if (!isStartupVideo) {
            Row(Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.background)) {
                LazyColumn(Modifier.width(68.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface), horizontalAlignment = Alignment.CenterHorizontally) {
                    val categories = listOf("APPEARANCE", "ATMOSPHERE", "BODY", "CLOTHING", "HAIR", "FACE", "EYES", "ACCESSORIES", "AUGMENTS", "TATTOOS", "ANIMATIONS")
                    items(categories) { cat ->
                        val selectedCat = activeCategory == cat
                        Box(
                            Modifier.fillMaxWidth().clickable { activeCategory = cat }
                                .background(if (selectedCat) MaterialTheme.colorScheme.primary.copy(alpha = .1f) else Color.Transparent)
                                .padding(vertical = 9.dp), contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(18.dp).background(if (selectedCat) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape))
                                Spacer(Modifier.height(3.dp))
                                Text(cat, fontSize = 6.sp, color = if (selectedCat) MaterialTheme.colorScheme.primary else Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight().padding(10.dp)) {
                    when (activeCategory) {
                        "APPEARANCE" -> AppearancePanel()
                        "ATMOSPHERE" -> AtmospherePanel()
                        "BODY" -> BodyPanel()
                        "CLOTHING" -> ClothingPanel()
                        "HAIR" -> HairPanel()
                        "FACE" -> FacePanel()
                        "EYES" -> EyesPanel()
                        "ACCESSORIES" -> AccessoriesPanel()
                        "AUGMENTS" -> AugmentsPanel()
                        "TATTOOS" -> TattoosPanel()
                        "ANIMATIONS" -> AnimationsPanel()
                    }
                }
            }
        }
    }
}

@Composable
fun PresetsMode() {
    val context = LocalContext.current
    val dao = remember { AvatarDatabase.getDatabase(context).avatarDao() }
    val presets by dao.getAllPresets().collectAsState(initial = emptyList())
    val state = LocalAvatarState.current
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("PRESETS", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(Modifier.height(10.dp))
        if (presets.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No saved presets. Save one in the Builder!", color = Color.Gray) }
        else LazyVerticalGrid(GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(presets) { preset ->
                Card(Modifier.aspectRatio(.85f).clickable {
                    state.gender = preset.gender; state.headShape = preset.headShape; state.age = preset.age; state.hairStyleIndex = preset.hairStyleIndex
                    state.height = preset.height; state.build = preset.build; state.jaw = preset.jaw; state.cheek = preset.cheek; state.clothingIndex = preset.clothingIndex
                    state.eyeShapeIndex = preset.eyeShapeIndex; state.augmentsIndex = preset.augmentsIndex; state.tattoosIndex = preset.tattoosIndex
                }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(Modifier.fillMaxSize()) { Text(preset.name, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center)); IconButton(onClick = { scope.launch(Dispatchers.IO) { dao.deletePreset(preset.id) } }, modifier = Modifier.align(Alignment.TopEnd)) { Text("X", color = Color.Red) } }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    var highQuality by remember { mutableStateOf(true) }
    var cacheClearedMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("SYSTEM CONFIGURATION", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Photorealistic High Quality Rendering", color = Color.White); Text("Enables full PBR pipelines, MSAA, and 2K textures.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) }
                Switch(checked = highQuality, onCheckedChange = { highQuality = it })
            }
        }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("Storage", color = Color.White); Spacer(Modifier.height(6.dp))
                Button(onClick = { context.cacheDir.deleteRecursively(); cacheClearedMessage = "Cache cleared successfully." }) { Text("Clear Local Cache") }
                if (cacheClearedMessage.isNotEmpty()) Text(cacheClearedMessage, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

fun shareImage(context: Context) {
    runCatching {
        val imageFile = File(context.cacheDir, "images/shared_avatar.png")
        imageFile.parentFile?.mkdirs()
        context.assets.open("avatars/avatar_preview.png").use { input -> FileOutputStream(imageFile).use { output -> input.copyTo(output) } }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share Avatar"))
    }
}

@Composable
fun ChatMode() {
    val context = LocalContext.current
    var messages by remember { mutableStateOf(listOf("Avatar" to "Neural link established. What's the job, chombatta?")) }
    var input by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("NEURAL CHAT", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(Modifier.height(10.dp))
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            messages.forEach { (sender, text) ->
                val avatar = sender == "Avatar"
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (avatar) Arrangement.Start else Arrangement.End) {
                    Box(Modifier.clip(RoundedCornerShape(7.dp)).background(if (avatar) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary.copy(alpha = .3f)).border(1.dp, if (avatar) Color.Transparent else MaterialTheme.colorScheme.primary, RoundedCornerShape(7.dp)).padding(10.dp).fillMaxWidth(.85f)) { Text(text, color = Color.White, fontSize = 13.sp) }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Enter message...") }, singleLine = true, shape = RoundedCornerShape(7.dp))
            Spacer(Modifier.width(6.dp))
            Button(onClick = {
                if (input.isNotBlank()) {
                    val prompt = input
                    input = ""
                    messages = messages + ("You" to prompt)
                    isSending = true
                    scope.launch {
                        try {
                            val history = messages.map { it.first to it.second }
                            val aiResponse = com.aura.avatarstudio.api.LocalLlamaService.chat(prompt, history)
                            messages = messages + ("Avatar" to aiResponse)
                        } catch (e: Exception) {
                            com.aura.avatarstudio.util.NetworkErrorHandler.handleError(context, e)
                        } finally {
                            isSending = false
                        }
                    }
                }
            }, enabled = !isSending && input.isNotBlank()) { if (isSending) CircularProgressIndicator(Modifier.size(15.dp), color = Color.White) else Text("SEND", fontSize = 10.sp) }
        }
    }
}
