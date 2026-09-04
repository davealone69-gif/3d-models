package com.aura.avatarstudio.api

import android.content.Context
import java.io.File

/** Holds the newest generated GLB so the renderer can hot-swap it safely. */
object GeneratedAvatarStore {
    @Volatile private var generatedFile: File? = null
    @Volatile private var generationId = 0L
    @Volatile private var consumedId = 0L
    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun publish(bytes: ByteArray): File {
        check(::appContext.isInitialized) { "GeneratedAvatarStore is not initialized" }
        val dir = File(appContext.filesDir, "generated_avatars").apply { mkdirs() }
        val file = File(dir, "avatar_${System.currentTimeMillis()}.glb")
        file.outputStream().use { it.write(bytes) }
        generatedFile?.delete()
        generatedFile = file
        generationId++
        return file
    }

    @Synchronized
    fun consumeNew(): File? {
        val file = generatedFile ?: return null
        if (consumedId == generationId) return null
        consumedId = generationId
        return file
    }
}
