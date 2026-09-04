package com.aura.avatarstudio.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "avatar_presets")
data class AvatarPreset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val gender: Int,
    val headShape: Float,
    val age: Float,
    val hairStyleIndex: Int,
    val height: Float,
    val build: Float,
    val jaw: Float,
    val cheek: Float,
    val clothingIndex: Int,
    val eyeShapeIndex: Int,
    val augmentsIndex: Int,
    val tattoosIndex: Int
)
