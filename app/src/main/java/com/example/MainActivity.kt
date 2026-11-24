package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.navigation.BabyAiApp
import com.example.viewmodel.BabyAiViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BabyAiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.handleLaunchIntent(intent)

        setContent {
            BabyAiApp(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissionStatus()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleLaunchIntent(intent)
    }
}
