package com.Neo.permissionauditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Neo.permissionauditor.ui.screens.AuditorScreen
import com.Neo.permissionauditor.viewmodel.AuditorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            // Instantiate the ViewModel built in Phase 1
            val viewModel: AuditorViewModel = viewModel()
            
            // Launch the main UI screen
            AuditorScreen(viewModel = viewModel)
        }
    }
}