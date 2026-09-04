package com.aura.avatarstudio.util

import android.content.Context
import android.widget.Toast
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NetworkErrorHandler {
    suspend fun handleError(context: Context, exception: Throwable) = withContext(Dispatchers.Main) {
        val message = getErrorMessage(exception)
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun getErrorMessage(exception: Throwable): String {
        return when (exception) {
            is UnknownHostException -> "No internet connection. Please check your network."
            is SocketTimeoutException -> "Connection timed out. Please try again later."
            is IOException -> "Network error occurred. Please try again."
            is HttpException -> {
                when (exception.code()) {
                    400 -> "Bad Request. Please check your input."
                    401 -> "Unauthorized. Please check your API key."
                    403 -> "Forbidden. Access is denied."
                    404 -> "Resource not found."
                    500 -> "Internal server error. Please try again later."
                    503 -> "Service unavailable. Please try again later."
                    else -> "Unexpected network error: ${exception.code()}"
                }
            }
            else -> "An unexpected error occurred: ${exception.localizedMessage ?: exception.javaClass.simpleName}"
        }
    }
}
