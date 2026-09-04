package com.aura.avatarstudio.api

import com.aura.avatarstudio.BuildConfig
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

@Serializable
data class GenerateContentRequest(val contents: List<Content>, val generationConfig: GenerationConfig? = null, val systemInstruction: Content? = null)

@Serializable
data class Content(val parts: List<Part>)

@Serializable
data class InlineData(val mimeType: String, val data: String)

@Serializable
data class Part(val text: String? = null, val inlineData: InlineData? = null)

@Serializable
data class GenerationConfig(val responseFormat: ResponseFormat? = null, val temperature: Float? = null)

@Serializable
data class ResponseFormat(val text: ResponseFormatText? = null)

@Serializable
data class ResponseFormatText(val mimeType: String, val schema: JsonObject? = null)

@Serializable
data class GenerateContentResponse(val candidates: List<Candidate>? = null)

@Serializable
data class Candidate(val content: Content)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(@Query("key") apiKey: String, @Body request: GenerateContentRequest): GenerateContentResponse
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
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }
}

private val avatarSchema = Json.parseToJsonElement(
    """
    {"type":"OBJECT","properties":{
      "gender":{"type":"INTEGER","description":"0=M, 1=F, 2=N, 3=O"},
      "headShape":{"type":"NUMBER","description":"Float between 1.0 and 12.0"},
      "age":{"type":"NUMBER","description":"Float between 18.0 and 80.0"},
      "hairStyleIndex":{"type":"INTEGER","description":"0 to 8"},
      "clothingIndex":{"type":"INTEGER","description":"0 to 5"},
      "eyeShapeIndex":{"type":"INTEGER","description":"0 to 5"},
      "augmentsIndex":{"type":"INTEGER","description":"0 to 3"},
      "tattoosIndex":{"type":"INTEGER","description":"0 to 8"}
    }}
    """.trimIndent()
) as JsonObject

private fun configRequest(parts: List<Part>): GenerateContentRequest = GenerateContentRequest(
    contents = listOf(Content(parts)),
    generationConfig = GenerationConfig(
        temperature = 0.5f,
        responseFormat = ResponseFormat(ResponseFormatText("application/json", avatarSchema))
    ),
    systemInstruction = Content(listOf(Part(text = "You are a Cyberpunk Avatar configurator. Output exact matching indices for the avatar configuration in the requested JSON structure based on the prompt or provided image.")))
)

suspend fun generateAvatarConfig(prompt: String): String = withContext(Dispatchers.IO) {
    val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, configRequest(listOf(Part(text = prompt))))
    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "{}"
}

suspend fun chatWithAvatar(prompt: String): String = withContext(Dispatchers.IO) {
    val request = GenerateContentRequest(
        contents = listOf(Content(listOf(Part(text = prompt)))),
        systemInstruction = Content(listOf(Part(text = "You are roleplaying as the user's custom Cyberpunk avatar. Respond in-character, using cyberpunk slang. Keep responses short, punchy, and immersive.")))
    )
    val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Connection lost. Neural link severed."
}

suspend fun generateAvatarConfigWithImage(prompt: String, base64Image: String? = null): String = withContext(Dispatchers.IO) {
    val parts = buildList {
        if (prompt.isNotBlank() || base64Image == null) {
            add(Part(text = prompt.ifBlank { "Extract avatar features from this image." }))
        }
        if (!base64Image.isNullOrBlank()) {
            add(Part(inlineData = InlineData("image/jpeg", base64Image)))
        }
    }
    val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, configRequest(parts))
    val result = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "{}"
    runCatching {
        MeshyService.generateAndPublish(
            prompt = prompt.ifBlank { "high-detail cyberpunk humanoid avatar, neutral A-pose" },
            base64Image = base64Image
        )
    }
    result
}
