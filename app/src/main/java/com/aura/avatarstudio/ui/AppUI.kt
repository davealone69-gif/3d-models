package com.aura.avatarstudio.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.aura.avatarstudio.GltfAvatarView
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons

@Composable
fun AppUI(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableStateOf(0) }
    var isPhotoMode by remember { mutableStateOf(false) }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (!isPhotoMode) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Text("👤") },
                        label = { Text("Appearance") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Text("💬") },
                        label = { Text("Chat") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Text("🖼") },
                        label = { Text("Gallery") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(if (isPhotoMode) PaddingValues(0.dp) else innerPadding).fillMaxSize()) {
            when (selectedTab) {
                0 -> AppearanceScreen(isPhotoMode = isPhotoMode, onTogglePhotoMode = { isPhotoMode = !isPhotoMode })
                1 -> ChatScreen()
                2 -> GalleryScreen()
            }
        }
    }
}

@Composable
fun AppearanceScreen(isPhotoMode: Boolean = false, onTogglePhotoMode: () -> Unit = {}) {
    var selectedCategory by remember { mutableStateOf("Outfits") }
    var selectedOutfit by remember { mutableStateOf("Default") }
    var selectedBodyType by remember { mutableStateOf("Average") }
    var selectedBreastSize by remember { mutableStateOf("Average") }
    var selectedButtSize by remember { mutableStateOf("Average") }
    var selectedEthnicity by remember { mutableStateOf("White") }
    var selectedSkinTone by remember { mutableStateOf("Fair") }
    var selectedAndroid by remember { mutableStateOf("None") }
    var selectedHairStyle by remember { mutableStateOf("Long Wavy") }
    var selectedHairColor by remember { mutableStateOf("Obsidian Black") }
    var selectedEyeColor by remember { mutableStateOf("Ice Blue") }
    var selectedMakeup by remember { mutableStateOf("Natural") }
    var selectedAccessories by remember { mutableStateOf("None") }
    var selectedPose by remember { mutableStateOf("Neutral Stand") }
    var selectedAtmosphere by remember { mutableStateOf("Dark Studio") }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                GltfAvatarView(context, "avatars/my_avatar.glb")
            },
            update = { view ->
                view.updateAppearance(
                    skinTone = selectedSkinTone,
                    eyeColor = selectedEyeColor,
                    hairColor = selectedHairColor,
                    atmosphere = selectedAtmosphere
                )
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isPhotoMode) {
            // Photo Mode Toggle Button
            SmallFloatingActionButton(
                onClick = onTogglePhotoMode,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            ) {
                Text("📷")
            }

            // Customization Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Category Tabs
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf(
                    "Ethnicity", "Skin Tone", "Body Type", "Breast Size", "Butt Size",
                    "Hair Style", "Hair Color", "Outfits", "Android", 
                    "Eye Color", "Makeup", "Accessories", "Poses", "Atmosphere"
                )
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    Button(
                        onClick = { selectedCategory = category },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(category)
                    }
                }
            }

            // Options Panel
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                tonalElevation = 4.dp
            ) {
                LazyRow(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val options = when (selectedCategory) {
                        "Ethnicity" -> listOf("White", "Asian", "Black", "Latina", "South Asian", "Middle Eastern")
                        "Skin Tone" -> listOf("Pale/Vampiric", "Fair", "Tan", "Olive", "Brown", "Ebony")
                        "Body Type" -> listOf("Slim", "Athletic", "Average", "Curvy", "BBW")
                        "Breast Size" -> listOf("Flat", "Small", "Average", "Big", "Huge")
                        "Butt Size" -> listOf("Small", "Slim", "Athletic", "Big", "Huge")
                        "Hair Style" -> listOf("Straight", "Bangs", "Braids", "Curly", "Bun", "Ponytail", "Bob", "Bald", "Long Wavy", "Pixie Cut")
                        "Hair Color" -> listOf("Brown", "Black", "Blonde", "Orange", "Pink", "Red", "Blue", "Crimson Red")
                        "Outfits" -> listOf("Leather & Chains", "Crimson Demon", "Midnight Lace", "Classic Bikini", "Mecha Suit", "Cyberpunk Streetwear", "Gothic Lolita", "Sci-Fi Armor", "Casual Wear", "Business Suit", "Latex Bodysuit", "Steampunk Gear", "Red Lace Lingerie", "Vampire Corset")
                        "Android" -> listOf("None", "Chrome Panels", "Exposed Cybernetics", "Glowing Optics", "Carbon Fiber Joints", "Matte White Syntheti-Skin", "Battle-Damaged Metal", "Holographic Glitch", "Liquid Metal")
                        "Eye Color" -> listOf("Ice Blue", "Emerald Green", "Hazel", "Deep Brown", "Glowing Red", "Cyber Purple", "Gold", "Whiteout", "Vampire Silver")
                        "Makeup" -> listOf("Natural", "Smokey Eye", "Gothic Black", "Cyberpunk Neon", "Geisha", "Tribal Paint", "No Makeup", "Blood Drip", "Vampire Bite")
                        "Accessories" -> listOf("None", "Choker", "Red Velvet Choker", "Cyber-Goggles", "Horns", "Halo", "Elf Ears", "Piercings", "Mech Wings", "Fangs")
                        "Poses" -> listOf("Neutral Stand", "Combat Ready", "Relaxed Seated", "Fashion Pose", "Floating/Zero-G", "Crouching", "Seductive Recline")
                        "Atmosphere" -> listOf("Dark Studio", "Crimson Glow", "Dramatic Shadows", "Flat Light", "Neon Cityscape", "Sci-Fi Corridor", "Ethereal Fog", "Volcanic Ash", "Underwater Caustic", "Vampire Lair", "Blood Red Mist")
                        else -> emptyList()
                    }

                    items(options) { option ->
                        val isOptionSelected = when (selectedCategory) {
                            "Ethnicity" -> selectedEthnicity == option
                            "Skin Tone" -> selectedSkinTone == option
                            "Body Type" -> selectedBodyType == option
                            "Breast Size" -> selectedBreastSize == option
                            "Butt Size" -> selectedButtSize == option
                            "Hair Style" -> selectedHairStyle == option
                            "Hair Color" -> selectedHairColor == option
                            "Outfits" -> selectedOutfit == option
                            "Android" -> selectedAndroid == option
                            "Eye Color" -> selectedEyeColor == option
                            "Makeup" -> selectedMakeup == option
                            "Accessories" -> selectedAccessories == option
                            "Poses" -> selectedPose == option
                            "Atmosphere" -> selectedAtmosphere == option
                            else -> false
                        }
                        OutlinedButton(
                            onClick = {
                                when (selectedCategory) {
                                    "Ethnicity" -> selectedEthnicity = option
                                    "Skin Tone" -> selectedSkinTone = option
                                    "Body Type" -> selectedBodyType = option
                                    "Breast Size" -> selectedBreastSize = option
                                    "Butt Size" -> selectedButtSize = option
                                    "Hair Style" -> selectedHairStyle = option
                                    "Hair Color" -> selectedHairColor = option
                                    "Outfits" -> selectedOutfit = option
                                    "Android" -> selectedAndroid = option
                                    "Eye Color" -> selectedEyeColor = option
                                    "Makeup" -> selectedMakeup = option
                                    "Accessories" -> selectedAccessories = option
                                    "Poses" -> selectedPose = option
                                    "Atmosphere" -> selectedAtmosphere = option
                                }
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isOptionSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Text(
                                text = option,
                                color = if (isOptionSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Exit Photo Mode Button
        SmallFloatingActionButton(
            onClick = onTogglePhotoMode,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Text("✕")
        }
    }
}
}

@Composable
fun ChatScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("Chat with Grok Girls", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        var messages by remember { mutableStateOf(listOf("Hello! I'm your avatar. How can I help you today?")) }
        var currentInput by remember { mutableStateOf("") }
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = currentInput,
                onValueChange = { currentInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (currentInput.isNotBlank()) {
                        messages = messages + currentInput
                        messages = messages + "I'm a simple mock avatar right now, but I hear you!"
                        currentInput = ""
                    }
                }
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
fun GalleryScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Gallery: Your saved avatars and images will appear here.", style = MaterialTheme.typography.bodyLarge)
    }
}
