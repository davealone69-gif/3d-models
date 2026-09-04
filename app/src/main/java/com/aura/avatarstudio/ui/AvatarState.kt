package com.aura.avatarstudio.ui

import androidx.compose.runtime.*

class AvatarState {
    var gender by mutableIntStateOf(0)
    var headShape by mutableFloatStateOf(4f)
    var age by mutableFloatStateOf(18f)
    var hairStyleIndex by mutableIntStateOf(-1)
    var height by mutableFloatStateOf(50f)
    var build by mutableFloatStateOf(50f)
    var jaw by mutableFloatStateOf(50f)
    var cheek by mutableFloatStateOf(50f)
    var clothingIndex by mutableIntStateOf(-1)
    var eyeShapeIndex by mutableIntStateOf(-1)
    var augmentsIndex by mutableIntStateOf(-1)
    var tattoosIndex by mutableIntStateOf(-1)
}

val LocalAvatarState = compositionLocalOf<AvatarState> { error("No AvatarState provided") }
