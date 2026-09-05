package com.aura.avatarstudio.api

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.codeshipping.llamakotlin.LlamaModel
import java.io.File
import java.io.FileOutputStream

/** Completely on-device Llama inference through llama.cpp. */
object LocalLlamaService {
    private const val MODEL_DIRECTORY = "models"
    private const val MODEL_FILE_NAME = "llama3.gguf"
    private const val APP_EXTERNAL_MODEL_ROOT = "/sdcard/Android/data/com.aura.avatarstudio.rewrite/files"

    private var model: LlamaModel? = null
    private var loadedPath: String? = null

    fun modelFile(context: Context): File =
        File(context.getExternalFilesDir(null), "$MODEL_DIRECTORY/$MODEL_FILE_NAME")

    private fun defaultModelFile(): File =
        File(APP_EXTERNAL_MODEL_ROOT, "$MODEL_DIRECTORY/$MODEL_FILE_NAME")

    fun hasModel(context: Context): Boolean =
        modelFile(context).isFile && modelFile(context).length() > 0L

    fun hasModel(): Boolean = defaultModelFile().isFile && defaultModelFile().length() > 0L

    suspend fun installModel(context: Context, source: java.io.InputStream) = withContext(Dispatchers.IO) {
        val destination = modelFile(context)
        destination.parentFile?.mkdirs()
        source.use { input ->
            FileOutputStream(destination).use { output -> input.copyTo(output) }
        }
        close()
    }

    private suspend fun loadPath(path: String): Boolean = withContext(Dispatchers.IO) {
        if (!File(path).isFile) return@withContext false
        if (loadedPath == path && model?.isLoaded == true) return@withContext true

        close()
        model = LlamaModel.load(path) {
            contextSize = 4096
            batchSize = 512
            threads = maxOf(2, Runtime.getRuntime().availableProcessors() - 1)
            threadsBatch = maxOf(2, Runtime.getRuntime().availableProcessors() - 1)
            temperature = 0.7f
            topP = 0.9f
            topK = 40
            repeatPenalty = 1.1f
            maxTokens = 512
            useMmap = true
            useMlock = false
            gpuLayers = 0
        }
        loadedPath = path
        model?.isLoaded == true
    }

    suspend fun load(context: Context): Boolean =
        loadPath(modelFile(context).absolutePath)

    suspend fun chat(prompt: String, history: List<Pair<String, String>>): String =
        withContext(Dispatchers.IO) {
            try {
                if (!loadPath(defaultModelFile().absolutePath)) {
                    return@withContext "Local Llama model not installed. Put $MODEL_FILE_NAME in the app's local model storage."
                }
                generate(prompt, history)
            } catch (e: Exception) {
                "Local Llama error: ${e.message ?: "unknown inference error"}"
            }
        }

    suspend fun chat(
        context: Context,
        prompt: String,
        history: List<Pair<String, String>>
    ): String = withContext(Dispatchers.IO) {
        try {
            if (!load(context)) {
                return@withContext "Local Llama model not installed. Add $MODEL_FILE_NAME to local model storage first."
            }
            generate(prompt, history)
        } catch (e: Exception) {
            "Local Llama error: ${e.message ?: "unknown inference error"}"
        }
    }

    private suspend fun generate(prompt: String, history: List<Pair<String, String>>): String {
        val conversation = buildString {
            append("<|begin_of_text|>")
            append("<|start_header_id|>system<|end_header_id|>\n\n")
            append("You are a helpful, slightly edgy AI avatar assistant.")
            append("<|eot_id|>")
            history.forEach { (role, text) ->
                append("<|start_header_id|>")
                append(if (role == "You") "user" else "assistant")
                append("<|end_header_id|>\n\n")
                append(text)
                append("<|eot_id|>")
            }
            append("<|start_header_id|>user<|end_header_id|>\n\n")
            append(prompt)
            append("<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n")
        }
        return model?.generate(conversation) ?: "Local Llama engine is not loaded."
    }

    fun close() {
        model?.close()
        model = null
        loadedPath = null
    }
}
