package com.aura.avatarstudio.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.aura.avatarstudio.data.AvatarDatabase
import com.aura.avatarstudio.data.AvatarPreset
import kotlinx.coroutines.flow.firstOrNull
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import android.util.Base64
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.aura.avatarstudio.GltfAvatarView
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.floatOrNull

@Composable
fun AppUI(modifier: Modifier = Modifier) {
    var topTab by remember { mutableStateOf("BUILDER") }
    val avatarState = remember { AvatarState() }

    CompositionLocalProvider(LocalAvatarState provides avatarState) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { if (avatarState.hasPlayedStartupVideo) TopHeader(topTab, onTabSelected = { topTab = it }) },
            bottomBar = { if (avatarState.hasPlayedStartupVideo) BottomActionPanel() }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(if (avatarState.hasPlayedStartupVideo) innerPadding else PaddingValues(0.dp)).fillMaxSize()) {
                if (topTab == "BUILDER") {
                    BuilderMode()
                } else if (topTab == "PRESETS") {
                    PresetsMode()
                } else if (topTab == "CHAT") {
                    ChatMode()
                } else if (topTab == "IMPORT") {
                    SettingsScreen()
                }
            }
        }
    }
}

@Composable
fun TopHeader(selected: String, onTabSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp).background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("AVATAR DESIGN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("CREATE YOUR IDENTITY", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            listOf("BUILDER", "PRESETS", "CHAT", "IMPORT").forEach { tab ->
                val isSelected = selected == tab
                Box(
                    modifier = Modifier
                        .clickable { onTabSelected(tab) }
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        tab,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Composable
fun BottomActionPanel() {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("AVATAR ID", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("MATRIX_07_8X9A", fontSize = 12.sp, color = Color.White, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {},
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) { Text("LOAD OUTFIT", fontSize = 12.sp) }

            Button(
                onClick = { shareImage(context) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("SAVE AVATAR", fontSize = 12.sp) }
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
            val atmospheres = listOf("Neon Cityscape", "Vampire Lair", "Fiery Hellscape", "Neutral Studio", "Blood Red Mist")
            val messages = listOf("CALIBRATING ARMOR...", "SYNCING ATMOSPHERE...", "LOADING NEURO-OPTICS...", "BOOT SEQUENCE COMPLETE")
            for (i in 0..3) {
                startupMessage = messages[i]
                state.atmosphere = atmospheres[i % atmospheres.size]
                state.clothingIndex = (0..7).random()
                state.avatarView?.updateAppearance(state.skinTone, state.eyeColor, state.hairColor, state.atmosphere)
                kotlinx.coroutines.delay(900)
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
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                if (bytes != null) {
                    base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!isStartupVideo) {
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            Button(
                onClick = { launcher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (base64Image != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(if (base64Image != null) "✓" else "+", fontSize = 16.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = aiPrompt,
                onValueChange = { aiPrompt = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("e.g. Corporate Netrunner...", fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )
            Spacer(modifier = Modifier.width(8.dp))
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
                        } catch (e: Exception) {
                            com.aura.avatarstudio.util.NetworkErrorHandler.handleError(context, e)
                        } finally {
                            isGenerating = false
                            base64Image = null // reset after generation
                        }
                    }
                },
                enabled = !isGenerating && (aiPrompt.isNotBlank() || base64Image != null),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isGenerating) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                else Text("AUTO-GEN", fontSize = 12.sp)
            }
        }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            AndroidView(
                factory = { context -> GltfAvatarView(context, "avatars/my_avatar.glb").also { state.avatarView = it } },
                modifier = Modifier.fillMaxSize()
            )

            if (isStartupVideo) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                    Text(startupMessage, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else {

            Column(
                modifier = Modifier.align(Alignment.CenterEnd).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listOf("R" to "ROTATE", "Q" to "ZOOM", "+" to "PAN", "S" to "SAVE").forEach { (icon, label) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.7f),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable {
                                    if (label == "SAVE") {
                                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                            val preset = AvatarPreset(
                                                name = "Preset \${System.currentTimeMillis() % 1000}",
                                                gender = state.gender,
                                                headShape = state.headShape,
                                                age = state.age,
                                                hairStyleIndex = state.hairStyleIndex,
                                                height = state.height,
                                                build = state.build,
                                                jaw = state.jaw,
                                                cheek = state.cheek,
                                                clothingIndex = state.clothingIndex,
                                                eyeShapeIndex = state.eyeShapeIndex,
                                                augmentsIndex = state.augmentsIndex,
                                                tattoosIndex = state.tattoosIndex
                                            )
                                            AvatarDatabase.getDatabase(context).avatarDao().insertPreset(preset)
                                        }
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) { Text(icon, color = Color.White) }
                        }
                        Text(label, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top=4.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.8f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listOf("1", "2", "3", "4", "5").forEachIndexed { index, icon ->
                    Text(icon, color = if(index == 4) MaterialTheme.colorScheme.primary else Color.White)
                }
            }
        }
        }

        if (!isStartupVideo) {
        Row(modifier = Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.background)) {
            LazyColumn(
                modifier = Modifier.width(80.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val categories = listOf(
                    "APPEARANCE", "ATMOSPHERE", "BODY", "CLOTHING", "HAIR", "FACE",
                    "EYES", "ACCESSORIES", "AUGMENTS", "TATTOOS", "ANIMATIONS"
                )
                items(categories) { cat ->
                    val isSelected = activeCategory == cat
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeCategory = cat }
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha=0.1f) else Color.Transparent)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(20.dp).background(if(isSelected) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape))
                            Spacer(Modifier.height(4.dp))
                            Text(cat, fontSize = 7.sp, color = if(isSelected) MaterialTheme.colorScheme.primary else Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(12.dp)) {
                when(activeCategory) {
                    "APPEARANCE" -> AppearancePanel()
                    "ATMOSPHERE" -> AtmospherePanel()
                    "HAIR" -> HairPanel()
                    "BODY" -> BodyPanel()
                    "CLOTHING" -> ClothingPanel()
                    "FACE" -> FacePanel()
                    "EYES" -> EyesPanel()
                    "ACCESSORIES" -> AccessoriesPanel()
                    "AUGMENTS" -> AugmentsPanel()
                    "TATTOOS" -> TattoosPanel()
                    "ANIMATIONS" -> AnimationsPanel()
                    else -> AppearancePanel()
                }
            }
        }
        }
    }
}

@Composable
fun AppearancePanel() {
    val state = LocalAvatarState.current

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("GENDER", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth().padding(top=8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("M", "F", "N", "O").forEachIndexed { index, g ->
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if(state.gender == index) MaterialTheme.colorScheme.primary.copy(alpha=0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if(state.gender == index) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { state.gender = index },
                        contentAlignment = Alignment.Center
                    ) { Text(g, color = if(state.gender == index) MaterialTheme.colorScheme.primary else Color.White) }
                }
            }
        }
        item {
            Text("SKIN TONE", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth().padding(top=8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tones = listOf(Color(0xFFFFDFC4), Color(0xFFF0D5BE), Color(0xFFD2B49D), Color(0xFFB48A6F), Color(0xFF8D5524), Color(0xFF5C3317), Color(0xFF291509))
                tones.forEach { tone ->
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(tone))
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("HEAD SHAPE", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("04 / 12", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Slider(value = state.headShape, onValueChange = { state.headShape = it }, valueRange = 1f..12f, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("AGE", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${state.age.toInt()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Slider(value = state.age, onValueChange = { state.age = it }, valueRange = 18f..80f, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
        }
    }
}

@Composable
fun HairPanel() {
    val state = LocalAvatarState.current
    var subTab by remember { mutableStateOf("STYLE") }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf("STYLE", "COLOR", "FACIAL", "EYEBROWS").forEach { tab ->
                Text(
                    tab,
                    fontSize = 10.sp,
                    color = if(subTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { subTab = tab }.padding(bottom=8.dp)
                )
            }
        }
        Divider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))
        val hairstyles = listOf("Buzz Cut", "Undercut", "Cyber Dreads", "Neon Bob", "Mohawk", "Slicked Back", "Pixie Cut", "Long Waves", "Braided")
        LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(hairstyles) { index, style ->
                val isSelected = state.hairStyleIndex == index
                Box(
                    modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { state.hairStyleIndex = index },
                    contentAlignment = Alignment.Center
                ) {
                    Text(style, color = Color.White, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun AtmospherePanel() {
    val state = LocalAvatarState.current
    val atmospheres = listOf("Neon Cityscape", "Dark Studio", "Blood Red Mist", "Vampire Lair", "Fiery Hellscape", "Neutral Studio")

    LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(atmospheres) { item ->
            val isSelected = state.atmosphere == item
            Box(
                modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .clickable {
                    state.atmosphere = item
                    state.avatarView?.updateAppearance(
                        state.skinTone, state.eyeColor, state.hairColor, state.atmosphere
                    )
                },
                contentAlignment = Alignment.Center
            ) {
                Text(item, color = Color.White, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
fun BodyPanel() {
    val state = LocalAvatarState.current
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("BODY CONFIGURATION", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Text("HEIGHT", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = state.height, onValueChange = { state.height = it }, valueRange = 0f..100f)
        Text("BUILD", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = state.build, onValueChange = { state.build = it }, valueRange = 0f..100f)
    }
}

@Composable
fun ClothingPanel() {
    val state = LocalAvatarState.current
    val clothingItems = listOf(
        "Nomad Jacket", "Corpo Suit", "Streetwear Hoodie", "Tactical Vest", "Netrunner Suit", "Casual Tee",
        "Demonic Knight Armor", "Medieval Leather Armor"
    )
    LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(clothingItems) { index, item ->
            val isSelected = state.clothingIndex == index
            Box(
                modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .clickable { state.clothingIndex = index },
                contentAlignment = Alignment.Center
            ) {
                Text(item, color = Color.White, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
fun FacePanel() {
    val state = LocalAvatarState.current
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("JAWLINE", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = state.jaw, onValueChange = { state.jaw = it }, valueRange = 0f..100f)
        Text("CHEEKBONES", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = state.cheek, onValueChange = { state.cheek = it }, valueRange = 0f..100f)
    }
}

@Composable
fun EyesPanel() {
    val state = LocalAvatarState.current
    Column(modifier = Modifier.fillMaxSize()) {
        Text("EYE COLOR", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val colors = listOf(Color.Blue, Color.Green, Color(0xFF6B4423), Color.Red, Color.Magenta)
            colors.forEach { c -> Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(c)) }
        }
        Spacer(Modifier.height(16.dp))
        Text("EYE SHAPE", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val eyeShapes = listOf("Natural", "Cyber-Optic", "Feline", "Synthetic", "Wide", "Narrow")
        LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top=8.dp)) {
            itemsIndexed(eyeShapes) { index, shape ->
                val isSelected = state.eyeShapeIndex == index
                Box(
                    modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { state.eyeShapeIndex = index },
                    contentAlignment = Alignment.Center
                ) {
                    Text(shape, color = Color.White, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun AccessoriesPanel() {
    val accessories = listOf("Aviators", "Respirator", "Holo-Visor", "Ear Cuff", "Goggles", "Choker", "Neural Link", "Bandana", "Cyber-Patch")
    LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(accessories) { acc ->
            Box(modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Text(acc, color = Color.White, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
fun AugmentsPanel() {
    val state = LocalAvatarState.current
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("CYBERWARE & IMPLANTS", fontSize = 12.sp, color = Color.White)
        val augments = listOf("Mantis Blades", "Gorilla Arms", "Subdermal Armor", "Optic Scanner")
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(augments) { index, aug ->
                val isSelected = state.augmentsIndex == index
                Box(
                    modifier = Modifier.aspectRatio(1.5f).clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { state.augmentsIndex = index },
                    contentAlignment = Alignment.Center
                ) {
                    Text(aug, color = Color.White, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun TattoosPanel() {
    val state = LocalAvatarState.current
    Text("BODY MODIFICATIONS", fontSize = 12.sp, color = Color.White)
    Spacer(Modifier.height(8.dp))
    val tattoos = listOf("Barcode", "Circuitry", "Yakuza Dragon", "Neon Lotus", "Tribal", "Hex Pattern", "Cyber-Skull", "Kanji", "Geometric")
    LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(tattoos) { index, tattoo ->
            val isSelected = state.tattoosIndex == index
            Box(
                modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .clickable { state.tattoosIndex = index },
                contentAlignment = Alignment.Center
            ) {
                Text(tattoo, color = Color.White, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
fun AnimationsPanel() {
    val state = LocalAvatarState.current
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf("IDLE NEUTRAL", "COMBAT READY", "RELAXED", "WALK CYCLE").forEachIndexed { index, anim ->
            Button(
                onClick = { state.avatarView?.playAnimation(index) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(anim)
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("PRESETS", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        if (presets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No saved presets. Save one in the Builder!", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(presets) { preset ->
                    Card(
                        modifier = Modifier
                            .aspectRatio(0.7f)
                            .clickable {
                                state.gender = preset.gender
                                state.headShape = preset.headShape
                                state.age = preset.age
                                state.hairStyleIndex = preset.hairStyleIndex
                                state.height = preset.height
                                state.build = preset.build
                                state.jaw = preset.jaw
                                state.cheek = preset.cheek
                                state.clothingIndex = preset.clothingIndex
                                state.eyeShapeIndex = preset.eyeShapeIndex
                                state.augmentsIndex = preset.augmentsIndex
                                state.tattoosIndex = preset.tattoosIndex
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(preset.name, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.align(Alignment.Center))
                            IconButton(
                                onClick = { scope.launch(kotlinx.coroutines.Dispatchers.IO) { dao.deletePreset(preset.id) } },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Text("X", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val highQuality by remember { mutableStateOf(true) }
    var cacheClearedMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("SYSTEM CONFIGURATION", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Photorealistic High Quality Rendering", color = Color.White)
                    Text("Enables full PBR pipelines, MSAA, and 2K textures.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Switch(checked = highQuality, onCheckedChange = { }, enabled = false)
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Storage", color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    context.cacheDir.deleteRecursively()
                    cacheClearedMessage = "Cache cleared successfully."
                }) { Text("Clear Local Cache") }
                if (cacheClearedMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(cacheClearedMessage, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

fun shareImage(context: Context) {
    try {
        val imageFile = File(context.cacheDir, "images/shared_avatar.png")
        imageFile.parentFile?.mkdirs()
        context.assets.open("avatars/avatar_preview.png").use { input ->
            FileOutputStream(imageFile).use { output ->
                input.copyTo(output)
            }
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Avatar"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun ChatMode() {
    val context = LocalContext.current
    var messages by remember { mutableStateOf(listOf(Pair("Avatar", "Neural link established. What's the job, chombatta?"))) }
    var input by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("NEURAL CHAT", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            messages.forEach { (sender, text) ->
                val isAvatar = sender == "Avatar"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isAvatar) Arrangement.Start else Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isAvatar) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary.copy(alpha=0.3f))
                            .border(1.dp, if (isAvatar) Color.Transparent else MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                            .fillMaxWidth(0.85f)
                    ) {
                        Text(text, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter message...") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (input.isNotBlank()) {
                        val prompt = input
                        input = ""
                        messages = messages + Pair("You", prompt)
                        isSending = true
                        scope.launch {
                            try {
                                val reply = com.aura.avatarstudio.api.chatWithAvatar(prompt)
                                messages = messages + Pair("Avatar", reply)
                            } catch (e: Exception) {
                                com.aura.avatarstudio.util.NetworkErrorHandler.handleError(context, e)
                            } finally {
                                isSending = false
                            }
                        }
                    }
                },
                enabled = !isSending && input.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isSending) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                else Text("SEND")
            }
        }
    }
}
