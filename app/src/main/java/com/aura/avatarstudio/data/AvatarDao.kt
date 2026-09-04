package com.aura.avatarstudio.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AvatarDao {
    @Query("SELECT * FROM avatar_presets ORDER BY id DESC")
    fun getAllPresets(): Flow<List<AvatarPreset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: AvatarPreset)

    @Query("DELETE FROM avatar_presets WHERE id = :id")
    suspend fun deletePreset(id: Long)
}
