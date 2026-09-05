package com.aura.avatarstudio.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object LocalLlamaService {
    private const val OLLAMA_BASE_URL = "http://192.168.1.50:11434"
    private const val MODEL_NAME = "llama3"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class ChatMessage(val role: String, val content: String)

    @Serializable
    data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val stream: Boolean = false
    )

    @Serializable
    data class ChatResponse(val message: Message)

    @Serializable
    data class Message(val role: String, val content: String)

    suspend fun chat(prompt: String, history: List<Pair<String, String>>): String =
        withContext(Dispatchers.IO) {
            try {
                val messages = mutableListOf(
                    ChatMessage(
                        "system",
                        "You are a helpful, slightly edgy AI avatar assistant."
                    )
                )
                history.forEach { (role, text) ->
                    messages.add(
                        ChatMessage(
                            if (role == "You") "user" else "assistant",
                            text
                        )
                    )
                }
                messages.add(ChatMessage("user", prompt))

                val requestBody = json.encodeToString(ChatRequest(MODEL_NAME, messages))
                    .toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$OLLAMA_BASE_URL/api/chat")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Ollama request failed: HTTP ${response.code}")
                    }
                    val responseBody = response.body?.string()
                        ?: throw Exception("Empty response from Llama")
                    val chatResponse = json.decodeFromString<ChatResponse>(responseBody)
                    chatResponse.message.content
                }
            } catch (e: Exception) {
                "Error connecting to local Llama AI. Is your PC running Ollama? (${e.message})"
            }
        }
}
