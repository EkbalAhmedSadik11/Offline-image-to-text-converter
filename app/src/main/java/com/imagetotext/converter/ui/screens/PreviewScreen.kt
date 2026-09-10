package com.imagetotext.converter.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.imagetotext.converter.viewmodel.OcrState
import com.imagetotext.converter.viewmodel.OcrViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    viewModel: OcrViewModel,
    onChangeImage: () -> Unit,
    onTextExtracted: () -> Unit
) {
    val bitmap = viewModel.editedBitmap ?: viewModel.originalBitmap
    val snackbarHostState = remember { SnackbarHostState() }

    // Makes the phone's physical/gesture back button do exactly what the
    // top bar's back arrow does, rather than relying on default back-stack
    // handling.
    BackHandler(enabled = true) { onChangeImage() }

    LaunchedEffect(viewModel.ocrState) {
        when (val state = viewModel.ocrState) {
            is OcrState.Success -> onTextExtracted()
            is OcrState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetOcrState()
            }
            else -> Unit
        }
    }

    if (bitmap == null) {
        LaunchedEffect(Unit) { onChangeImage() }
        return
    }

    val isBusy = viewModel.ocrState is OcrState.Preparing || viewModel.ocrState is OcrState.Recognizing

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview Image") },
                navigationIcon = {
                    IconButton(onClick = onChangeImage) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Change image")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { data ->
            Snackbar(snackbarData = data)
        } }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.03f))
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Selected image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(onClick = onChangeImage, modifier = Modifier.fillMaxWidth()) {
                Text("Change Image")
            }

            Spacer(Modifier.height(18.dp))

            // ---- Preprocessing tools ----
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Adjust Image", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Grayscale and contrast are enhanced automatically for the most " +
                            "accurate text recognition.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(onClick = { viewModel.rotateLeft() }) {
                            Icon(Icons.Filled.RotateLeft, contentDescription = "Rotate left")
                        }
                        IconButton(onClick = { viewModel.rotateRight() }) {
                            Icon(Icons.Filled.RotateRight, contentDescription = "Rotate right")
                        }
                        TextButton(onClick = { viewModel.resetEdits() }) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Reset")
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { viewModel.runOcr() },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.DocumentScanner, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Extract Text", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (isBusy) {
        OcrProgressDialog(state = viewModel.ocrState)
    }
}

@Composable
private fun OcrProgressDialog(state: OcrState) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Extracting Text...", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                when (state) {
                    is OcrState.Recognizing -> {
                        LinearProgressIndicator(
                            progress = state.percent / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Processing ${state.percent}%", style = MaterialTheme.typography.bodyMedium)
                    }
                    is OcrState.Preparing -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text(state.message, style = MaterialTheme.typography.bodyMedium)
                    }
                    else -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
