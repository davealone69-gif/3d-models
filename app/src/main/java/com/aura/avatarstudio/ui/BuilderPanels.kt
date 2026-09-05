package com.aura.avatarstudio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
private fun PanelColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) { content() }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text)
    HorizontalDivider()
}

@Composable
private fun ChoiceRow(
    values: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEachIndexed { index, value ->
            FilterChip(
                selected = selected == index,
                onClick = { onSelected(index) },
                label = { Text(value) }
            )
        }
    }
}

@Composable
private fun ValueSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Text("$label: ${"%.1f".format(value)}")
    Slider(value = value, onValueChange = onValueChange, valueRange = range)
}

@Composable
fun AppearancePanel() {
    val state = LocalAvatarState.current
    PanelColumn {
        SectionTitle("APPEARANCE")
        Text("Gender")
        ChoiceRow(listOf("M", "F", "N", "O"), state.gender, state::letGender)
        Text("Skin tone")
        ChoiceRow(listOf("Fair", "Warm", "Tan", "Deep"), listOf("Fair", "Warm", "Tan", "Deep").indexOf(state.skinTone).coerceAtLeast(0)) { state.skinTone = listOf("Fair", "Warm", "Tan", "Deep")[it] }
        Text("Eye colour")
        ChoiceRow(listOf("Blue", "Green", "Amber", "Violet"), listOf("Blue", "Green", "Amber", "Violet").indexOf(state.eyeColor).coerceAtLeast(0)) { state.eyeColor = listOf("Blue", "Green", "Amber", "Violet")[it] }
        Text("Hair colour")
        ChoiceRow(listOf("Neon Pink", "Cyan", "White", "Black"), listOf("Neon Pink", "Cyan", "White", "Black").indexOf(state.hairColor).coerceAtLeast(0)) { state.hairColor = listOf("Neon Pink", "Cyan", "White", "Black")[it] }
        ValueSlider("Head shape", state.headShape, 1f..12f) { state.headShape = it }
        ValueSlider("Age", state.age, 18f..80f) { state.age = it }
    }
}

@Composable
fun AtmospherePanel() {
    val state = LocalAvatarState.current
    val values = listOf("Neon Cityscape", "Night Club", "Industrial", "Desert", "Space Station", "Studio")
    PanelColumn {
        SectionTitle("ATMOSPHERE")
        values.forEach { value ->
            Button(onClick = { state.atmosphere = value }, modifier = Modifier.fillMaxWidth()) { Text(value) }
        }
    }
}

@Composable
fun BodyPanel() {
    val state = LocalAvatarState.current
    PanelColumn {
        SectionTitle("BODY")
        ValueSlider("Height", state.height, 0f..100f) { state.height = it }
        ValueSlider("Build", state.build, 0f..100f) { state.build = it }
        ValueSlider("Jaw", state.jaw, 0f..100f) { state.jaw = it }
        ValueSlider("Cheek", state.cheek, 0f..100f) { state.cheek = it }
    }
}

@Composable
fun ClothingPanel() {
    val state = LocalAvatarState.current
    PanelColumn {
        SectionTitle("CLOTHING")
        ChoiceRow(listOf("Tech", "Street", "Armour", "Formal", "Tactical", "Minimal"), state.clothingIndex.coerceIn(-1, 5)) { state.clothingIndex = it }
    }
}

@Composable
fun HairPanel() {
    val state = LocalAvatarState.current
    PanelColumn {
        SectionTitle("HAIR")
        ChoiceRow(listOf("Long", "Short", "Bob", "Mohawk", "Braids", "Ponytail", "Undercut", "Spikes", "Bun"), state.hairStyleIndex.coerceIn(-1, 8)) { state.hairStyleIndex = it }
    }
}

@Composable
fun FacePanel() {
    val state = LocalAvatarState.current
    PanelColumn {
        SectionTitle("FACE")
        ValueSlider("Head shape", state.headShape, 1f..12f) { state.headShape = it }
        ValueSlider("Jaw", state.jaw, 0f..100f) { state.jaw = it }
        ValueSlider("Cheek", state.cheek, 0f..100f) { state.cheek = it }
    }
}

@Composable
fun EyesPanel() {
    val state = LocalAvatarState.current
    PanelColumn {
        SectionTitle("EYES")
        ChoiceRow(listOf("Almond", "Round", "Sharp", "Wide", "Narrow", "Cyber"), state.eyeShapeIndex.coerceIn(-1, 5)) { state.eyeShapeIndex = it }
        Text("Colour: ${state.eyeColor}")
    }
}

@Composable
fun AccessoriesPanel() {
    PanelColumn {
        SectionTitle("ACCESSORIES")
        Text("Select accessories for the avatar loadout.")
        Text("Earrings")
        Text("Neckwear")
        Text("Visor")
        Text("Utility belt")
        Text("Arm bands")
    }
}

@Composable
fun AugmentsPanel() {
    val state = LocalAvatarState.current
    PanelColumn {
        SectionTitle("AUGMENTS")
        ChoiceRow(listOf("None", "Eyes", "Arms", "Full cyber"), state.augmentsIndex.coerceIn(-1, 3)) { state.augmentsIndex = it }
    }
}

@Composable
fun TattoosPanel() {
    val state = LocalAvatarState.current
    PanelColumn {
        SectionTitle("TATTOOS")
        ChoiceRow(listOf("None", "Circuit", "Tribal", "Neon", "Face", "Sleeve", "Back", "Chest", "Full"), state.tattoosIndex.coerceIn(-1, 8)) { state.tattoosIndex = it }
    }
}

@Composable
fun AnimationsPanel() {
    val state = LocalAvatarState.current
    val animations = listOf("Idle", "Walk", "Run", "Dance", "Pose", "Wave")
    PanelColumn {
        SectionTitle("ANIMATIONS")
        animations.forEachIndexed { index, name ->
            Button(
                onClick = { state.avatarView?.playAnimation(index) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(name) }
        }
    }
}

private fun AvatarState.letGender(value: Int) {
    gender = value
}
