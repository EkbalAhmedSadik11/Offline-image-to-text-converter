package com.imagetotext.converter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.imagetotext.converter.viewmodel.OcrViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    viewModel: OcrViewModel,
    onBackToHome: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showClearConfirm by remember { mutableStateOf(false) }

    val stats = viewModel.textStats()
    val hasText = viewModel.extractedText.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extracted Text") },
                navigationIcon = {
                    IconButton(onClick = onBackToHome) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back to home")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {

            if (viewModel.lastDetectedLanguages.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Detected: ${viewModel.lastDetectedLanguages.joinToString(", ")}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (viewModel.isArabicDominant) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                    OutlinedTextField(
                        value = viewModel.extractedText,
                        onValueChange = { viewModel.updateExtractedText(it) },
                        modifier = Modifier.fillMaxSize(),
                        label = { Text("Extracted Text") },
                        placeholder = { Text("No text yet") }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Characters: ${stats.characters}", style = MaterialTheme.typography.bodyMedium)
                Text("Words: ${stats.words}", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp)
            ) {
                item {
                    ActionButton(
                        text = "Copy",
                        icon = Icons.Filled.ContentCopy,
                        enabled = hasText
                    ) {
                        clipboardManager.setText(AnnotatedString(viewModel.extractedText))
                        scope.launch { snackbarHostState.showSnackbar("Text copied successfully") }
                    }
                }
                item {
                    ActionButton(
                        text = "Save",
                        icon = Icons.Filled.Save,
                        enabled = hasText
                    ) {
                        val uri = viewModel.saveExtractedText()
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (uri != null) "Saved as extracted-text.txt" else "Could not save the file."
                            )
                        }
                    }
                }
                item {
                    ActionButton(
                        text = "Share",
                        icon = Icons.Filled.Share,
                        enabled = hasText
                    ) {
                        context.startActivity(viewModel.buildShareIntent())
                    }
                }
                item {
                    ActionButton(
                        text = "Clear",
                        icon = Icons.Filled.Clear,
                        enabled = hasText
                    ) {
                        showClearConfirm = true
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(onClick = onBackToHome, modifier = Modifier.fillMaxWidth()) {
                Text("New Image")
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear extracted text?") },
            text = { Text("This will remove the text from the editor. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearExtractedText()
                    showClearConfirm = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}
