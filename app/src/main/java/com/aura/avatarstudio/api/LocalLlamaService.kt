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

/** Local OpenAI-compatible Llama service. The endpoint is configurable so it can point at a PC. */
object LocalLlamaService {
    private const val DEFAULT_BASE_URL = "http://10.0.2.2:1234/v1"
    private const val DEFAULT_MODEL = "llama-3.2-3b-instruct"

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Volatile
    var baseUrl: String = DEFAULT_BASE_URL

    @Volatile
    var model: String = DEFAULT_MODEL

    @Serializable
    private data class Message(val role: String, val content: String)

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        val temperature: Double = 0.7,
        val max_tokens: Int = 512
    )

    @Serializable
    private data class ChatResponse(val choices: List<Choice> = emptyList())

    @Serializable
    private data class Choice(val message: Message)

    suspend fun chat(prompt: String, history: List<Pair<String, String>> = emptyList()): String =
        withContext(Dispatchers.IO) {
            require(prompt.isNotBlank()) { "Prompt cannot be blank" }

            val messages = buildList {
                add(
                    Message(
                        "system",
                        "You are the user's custom Cyberpunk avatar. Respond in-character, using cyberpunk slang. Keep responses short, punchy, and immersive."
                    )
                )
                history.forEach { (speaker, text) ->
                    add(Message(if (speaker.equals("You", true)) "user" else "assistant", text))
                }
                add(Message("user", prompt))
            }

            val requestBody = json.encodeToString(ChatRequest(model = model, messages = messages))
                .toRequestBody("application/json".toMediaType())
            val endpoint = baseUrl.trimEnd('/') + "/chat/completions"
            val request = Request.Builder()
                .url(endpoint)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException("Local Llama request failed: HTTP ${response.code} $body")
                }
                val parsed = json.decodeFromString<ChatResponse>(body)
                parsed.choices.firstOrNull()?.message?.content?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: throw IllegalStateException("Local Llama returned no response")
            }
        }
}
