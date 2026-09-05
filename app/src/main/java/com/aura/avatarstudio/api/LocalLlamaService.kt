package com.aura.avatarstudio.api

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Local-only Llama client using an OpenAI-compatible Android loopback API.
 * Access tokens are stored only in Android app preferences, never in source.
 */
object LocalLlamaService {
    private const val PREFS = "local_llama"
    private const val ENDPOINT_KEY = "endpoint"
    private const val MODEL_KEY = "model"
    private const val TOKEN_KEY = "token"
    private const val DEFAULT_ENDPOINT = "http://127.0.0.1:8088"
    private const val DEFAULT_MODEL = "llama-3.2-3b-instruct"
    private const val CHAT_PATH = "/v1/chat/completions"
    private const val MODELS_PATH = "/v1/models"

    private var appContext: Context? = null
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun endpoint(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(ENDPOINT_KEY, DEFAULT_ENDPOINT) ?: DEFAULT_ENDPOINT

    fun model(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(MODEL_KEY, DEFAULT_MODEL) ?: DEFAULT_MODEL

    fun token(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(TOKEN_KEY, "") ?: ""

    fun saveSettings(context: Context, endpoint: String, model: String, token: String = "") {
        initialize(context)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(ENDPOINT_KEY, normalizeBaseUrl(endpoint))
            .putString(MODEL_KEY, model.trim().ifBlank { DEFAULT_MODEL })
            .putString(TOKEN_KEY, token.trim())
            .apply()
    }

    fun resetSettings(context: Context) {
        initialize(context)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    /** Existing ChatMode entry point. It uses the saved local Android settings. */
    suspend fun chat(prompt: String, history: List<Pair<String, String>>): String {
        val context = appContext
        if (context == null) {
            return "Local AI is not initialized yet. Restart Avatar Studio and retry."
        }
        return chat(context, prompt, history)
    }

    suspend fun testConnection(context: Context): Result<String> = withContext(Dispatchers.IO) {
        initialize(context)
        runCatching {
            val base = normalizeBaseUrl(endpoint(context))
            val request = requestBuilder(base + MODELS_PATH, token(context)).get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("Local AI returned HTTP ${response.code}: ${body.take(300)}")
                }
                firstModelId(body) ?: model(context)
            }
        }
    }

    suspend fun chat(
        context: Context,
        prompt: String,
        history: List<Pair<String, String>>
    ): String = withContext(Dispatchers.IO) {
        initialize(context)
        try {
            val base = normalizeBaseUrl(endpoint(context))
            val configuredModel = model(context)
            val token = token(context)
            val selectedModel = discoverModel(base, configuredModel, token)
            val messages = buildMessages(prompt, history)
            val payload = buildString {
                append("{\"model\":")
                append(json.encodeToString(kotlinx.serialization.serializer<String>(), selectedModel))
                append(",\"messages\":[")
                messages.forEachIndexed { index, message ->
                    if (index > 0) append(',')
                    append("{\"role\":")
                    append(json.encodeToString(kotlinx.serialization.serializer<String>(), message.first))
                    append(",\"content\":")
                    append(json.encodeToString(kotlinx.serialization.serializer<String>(), message.second))
                    append('}')
                }
                append("],\"stream\":false,\"temperature\":0.7,\"max_tokens\":512}")
            }

            val request = requestBuilder(base + CHAT_PATH, token)
                .post(payload.toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("Local AI returned HTTP ${response.code}: ${body.take(300)}")
                }
                val root = json.parseToJsonElement(body).jsonObject
                root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
                    ?: error("Local AI returned no message content")
            }
        } catch (e: Exception) {
            "Local AI unavailable at ${normalizeBaseUrl(endpoint(context))}. Start the Android Llama runner and load Llama 3.2, then retry. (${e.message ?: "connection error"})"
        }
    }

    private fun buildMessages(prompt: String, history: List<Pair<String, String>>): List<Pair<String, String>> =
        buildList {
            add("system" to "You are a helpful AI avatar assistant. Keep answers concise and useful for avatar design, styling, scenes and creative direction.")
            history.forEach { (role, text) ->
                add((if (role.equals("You", ignoreCase = true)) "user" else "assistant") to text)
            }
            add("user" to prompt)
        }

    private fun discoverModel(base: String, configuredModel: String, token: String): String =
        runCatching {
            val request = requestBuilder(base + MODELS_PATH, token).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching configuredModel
                firstModelId(response.body?.string().orEmpty()) ?: configuredModel
            }
        }.getOrDefault(configuredModel)

    private fun requestBuilder(url: String, token: String): Request.Builder {
        val builder = Request.Builder().url(url).header("Accept", "application/json")
        if (token.isNotBlank()) builder.header("Authorization", "Bearer $token")
        return builder
    }

    private fun firstModelId(body: String): String? = runCatching {
        json.parseToJsonElement(body).jsonObject["data"]
            ?.jsonArray?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content
    }.getOrNull()

    private fun normalizeBaseUrl(value: String): String {
        var base = value.trim().trimEnd('/')
        if (base.endsWith("/v1")) base = base.removeSuffix("/v1")
        if (base.endsWith(CHAT_PATH)) base = base.removeSuffix(CHAT_PATH)
        if (base.endsWith(MODELS_PATH)) base = base.removeSuffix(MODELS_PATH)
        return base.ifBlank { DEFAULT_ENDPOINT }
    }
}
