package com.aura.avatarstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.aura.avatarstudio.api.LocalLlamaService
import com.aura.avatarstudio.ui.AppUI
import com.aura.avatarstudio.ui.theme.AvatarStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalLlamaService.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            AvatarStudioTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppUI(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
