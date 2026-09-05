package com.aura.avatarstudio.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aura.avatarstudio.api.LocalLlamaService
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun LocalAiSetupDialog(
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var endpoint by remember { mutableStateOf("http://127.0.0.1:8088") }
    var model by remember { mutableStateOf("llama-3.2-3b-instruct") }
    var token by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var testing by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        endpoint = LocalLlamaService.endpoint(context)
        model = LocalLlamaService.model(context)
        token = LocalLlamaService.token(context)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("LOCAL AI") },
        text = {
            Column {
                Text("Android-local Llama runner. Nothing is sent to a cloud AI service.", modifier = Modifier.padding(bottom = 12.dp))
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    label = { Text("Endpoint") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Access token") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (status.isNotBlank()) {
                    Text(status, modifier = Modifier.padding(top = 10.dp))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("LATER") }
        },
        confirmButton = {
            Button(
                enabled = !testing,
                onClick = {
                    scope.launch {
                        testing = true
                        status = "Testing local runner..."
                        LocalLlamaService.saveSettings(context, endpoint, model, token)
                        LocalLlamaService.testConnection(context).fold(
                            onSuccess = { detectedModel ->
                                status = "CONNECTED: $detectedModel"
                                testing = false
                                onSaved()
                            },
                            onFailure = { error ->
                                status = "FAILED: ${error.message ?: "connection error"}"
                                testing = false
                            }
                        )
                    }
                }
            ) { Text(if (testing) "TESTING..." else "SAVE & TEST") }
        }
    )
}
