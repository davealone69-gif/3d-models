package com.aura.avatarstudio.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.aura.avatarstudio.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class GenerationConfig(
    val responseFormat: ResponseFormat? = null,
    val temperature: Float? = null
)

@Serializable
data class ResponseFormat(
    val text: ResponseFormatText? = null
)

@Serializable
data class ResponseFormatText(
    val mimeType: String,
    val schema: JsonObject? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

suspend fun generateAvatarConfig(prompt: String): String = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    
    // Create JSON schema for structured output to configure avatar parameters
    val schemaJson = """
    {
      "type": "OBJECT",
      "properties": {
        "gender": { "type": "INTEGER", "description": "0=M, 1=F, 2=N, 3=O" },
        "headShape": { "type": "NUMBER", "description": "Float between 1.0 and 12.0" },
        "age": { "type": "NUMBER", "description": "Float between 18.0 and 80.0" },
        "hairStyleIndex": { "type": "INTEGER", "description": "0 to 8" },
        "clothingIndex": { "type": "INTEGER", "description": "0 to 5" },
        "eyeShapeIndex": { "type": "INTEGER", "description": "0 to 5" },
        "augmentsIndex": { "type": "INTEGER", "description": "0 to 3" },
        "tattoosIndex": { "type": "INTEGER", "description": "0 to 8" }
      }
    }
    """.trimIndent()
    val schemaElement = Json.parseToJsonElement(schemaJson) as JsonObject

    val request = GenerateContentRequest(
        contents = listOf(Content(parts = listOf(Part(text = prompt)))),
        generationConfig = GenerationConfig(
            temperature = 0.5f,
            responseFormat = ResponseFormat(
                text = ResponseFormatText(
                    mimeType = "application/json",
                    schema = schemaElement
                )
            )
        ),
        systemInstruction = Content(parts = listOf(Part(text = "You are a Cyberpunk Avatar configurator. The user gives a prompt, and you must output the exact matching indices for their avatar configuration in the requested JSON structure.")))
    )

    try {
        val response = RetrofitClient.service.generateContent(apiKey, request)
        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "{}"
    } catch (e: Exception) {
        e.printStackTrace()
        "{}" // fallback to empty json
    }
}

suspend fun chatWithAvatar(prompt: String): String = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    
    val request = GenerateContentRequest(
        contents = listOf(Content(parts = listOf(Part(text = prompt)))),
        systemInstruction = Content(parts = listOf(Part(text = "You are roleplaying as the user's custom Cyberpunk avatar. Respond in-character, using cyberpunk slang (chombatta, eddies, chrome, netrunner, etc.). Keep responses short, punchy, and immersive.")))
    )

    try {
        val response = RetrofitClient.service.generateContent(apiKey, request)
        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Connection lost. Neural link severed."
    } catch (e: Exception) {
        e.printStackTrace()
        "Error: Neural static. Retrying connection..."
    }
}
