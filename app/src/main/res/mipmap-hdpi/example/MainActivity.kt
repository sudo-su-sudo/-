package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.ChatViewModel
import com.example.ui.MainSandboxApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle cold-start share intents
        handleIncomingIntent(intent)

        setContent {
            MyApplicationTheme {
                MainSandboxApp(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handle running-start share intents
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        
        if (intent.action == Intent.ACTION_SEND) {
            val mimeType = intent.type
            if (mimeType != null) {
                if (mimeType.startsWith("text/")) {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (sharedText != null) {
                        viewModel.handleSharedData(sharedText, null, null)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val extraStreamUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    if (extraStreamUri != null) {
                        viewModel.handleSharedData(null, extraStreamUri.toString(), mimeType)
                    }
                }
            }
        }
    }
}
