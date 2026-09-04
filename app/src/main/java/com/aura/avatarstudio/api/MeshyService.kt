@file:Suppress("MaxLineLength", "MagicNumber", "ThrowsCount")

package com.aura.avatarstudio.api

import com.aura.avatarstudio.BuildConfig
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Real AI 3D generation: Meshy image-to-3D or text-to-3D, returning a GLB to the renderer. */
object MeshyService {
    private const val BASE_URL = "https://api.meshy.ai"
    private const val POLL_MS = 5000L
    private const val MAX_POLLS = 72
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder().build()

    suspend fun generateAndPublish(prompt: String, base64Image: String?): Boolean = withContext(Dispatchers.IO) {
        val key = BuildConfig.MESHY_API_KEY.trim()
        if (key.isBlank() || key.startsWith("YOUR_")) return@withContext false
        val taskId = if (!base64Image.isNullOrBlank()) {
            createImageTask(key, base64Image)
        } else {
            createTextTask(key, prompt.ifBlank { "high-detail cyberpunk humanoid avatar, neutral A-pose" })
        }
        val completed = poll(
            key,
            taskId,
            if (base64Image.isNullOrBlank()) "/openapi/v2/text-to-3d/" else "/openapi/v1/image-to-3d/"
        )
        val glbUrl = completed["model_urls"]?.jsonObject?.get("glb")?.jsonPrimitive?.content
            ?: throw IOException("Meshy completed without a GLB URL")
        val request = Request.Builder().url(glbUrl).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("GLB download failed: HTTP ${response.code}")
            val bytes = response.body?.bytes() ?: throw IOException("Meshy returned an empty GLB")
            require(bytes.size >= 20 && bytes[0] == 'g'.code.toByte() && bytes[1] == 'l'.code.toByte() && bytes[2] == 'T'.code.toByte() && bytes[3] == 'F'.code.toByte()) {
                "Meshy returned invalid GLB data"
            }
            GeneratedAvatarStore.publish(bytes)
        }
        true
    }

    private fun createImageTask(key: String, base64Image: String): String {
        val body = """{"image_url":"data:image/jpeg;base64,$base64Image","model_type":"standard","ai_model":"latest","should_texture":true,"enable_pbr":true,"pose_mode":"a-pose","target_formats":["glb"]}"""
        return post(key, "/openapi/v1/image-to-3d", body)
    }

    private fun createTextTask(key: String, prompt: String): String {
        val safePrompt = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").take(800)
        val body = """{"mode":"preview","prompt":"$safePrompt","model_type":"standard","ai_model":"latest","should_remesh":false,"target_formats":["glb"]}"""
        return post(key, "/openapi/v2/text-to-3d", body)
    }

    private fun post(key: String, path: String, body: String): String {
        val request = Request.Builder()
            .url(BASE_URL + path)
            .header("Authorization", "Bearer $key")
            .header("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Meshy request failed: HTTP ${response.code} $text")
            return json.parseToJsonElement(text).jsonObject["result"]?.jsonPrimitive?.content
                ?: throw IOException("Meshy request returned no task id")
        }
    }

    private suspend fun poll(key: String, taskId: String, pathPrefix: String): JsonObject {
        repeat(MAX_POLLS) {
            val request = Request.Builder()
                .url(BASE_URL + pathPrefix + taskId)
                .header("Authorization", "Bearer $key")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw IOException("Meshy poll failed: HTTP ${response.code} $text")
                val obj = json.parseToJsonElement(text).jsonObject
                when (obj["status"]?.jsonPrimitive?.content) {
                    "SUCCEEDED" -> return obj
                    "FAILED", "CANCELED" -> throw IOException(
                        obj["task_error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                            ?: "Meshy generation failed"
                    )
                    else -> Unit
                }
            }
            delay(POLL_MS)
        }
        throw IOException("Meshy generation timed out")
    }
}
