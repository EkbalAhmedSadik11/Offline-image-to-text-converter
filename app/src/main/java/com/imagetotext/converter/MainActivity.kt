package com.imagetotext.converter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imagetotext.converter.ui.navigation.AppNavHost
import com.imagetotext.converter.ui.theme.ImageToTextTheme
import com.imagetotext.converter.viewmodel.OcrViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: OcrViewModel = viewModel()
            val darkModeOverride by viewModel.isDarkMode.collectAsState()

            ImageToTextTheme(darkModeOverride = darkModeOverride) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(viewModel = viewModel)
                }
            }
        }
    }
}
