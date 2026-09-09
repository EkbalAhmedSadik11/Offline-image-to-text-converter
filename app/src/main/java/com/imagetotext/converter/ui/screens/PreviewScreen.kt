package com.imagetotext.converter.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.imagetotext.converter.viewmodel.OcrState
import com.imagetotext.converter.viewmodel.OcrViewModel
import kotlin.math.max
import kotlin.math.min
import androidx.compose.foundation.Image
import android.graphics.Rect as AndroidRect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    viewModel: OcrViewModel,
    onChangeImage: () -> Unit,
    onTextExtracted: () -> Unit
) {
    val bitmap = viewModel.editedBitmap ?: viewModel.originalBitmap
    val snackbarHostState = remember { SnackbarHostState() }

    var isCropping by remember { mutableStateOf(false) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }
    var containerSizePx by remember { mutableStateOf(IntSize.Zero) }

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
            // ---- Image + optional crop overlay ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                    .clipToRoundedCard()
                    .onSizeChanged { containerSizePx = it }
                    .pointerInput(isCropping) {
                        if (isCropping) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    dragStart = offset
                                    dragEnd = offset
                                },
                                onDrag = { change, _ ->
                                    dragEnd = change.position
                                }
                            )
                        }
                    }
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Selected image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                val start = dragStart
                val end = dragEnd
                if (isCropping && start != null && end != null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val left = min(start.x, end.x)
                        val top = min(start.y, end.y)
                        val right = max(start.x, end.x)
                        val bottom = max(start.y, end.y)
                        val dim = Color.Black.copy(alpha = 0.5f)

                        // Dim everything outside the selected rectangle using four
                        // plain strips (no blend-mode tricks, so it always renders
                        // correctly regardless of the surrounding compositing layer).
                        drawRect(color = dim, topLeft = Offset(0f, 0f), size = Size(size.width, top))
                        drawRect(color = dim, topLeft = Offset(0f, bottom), size = Size(size.width, size.height - bottom))
                        drawRect(color = dim, topLeft = Offset(0f, top), size = Size(left, bottom - top))
                        drawRect(color = dim, topLeft = Offset(right, top), size = Size(size.width - right, bottom - top))

                        drawRect(
                            color = Color.White,
                            topLeft = Offset(left, top),
                            size = Size(right - left, bottom - top),
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }

            if (isCropping) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val start = dragStart
                            val end = dragEnd
                            if (start != null && end != null && containerSizePx.width > 0) {
                                val scaleX = bitmap.width.toFloat() / containerSizePx.width
                                val scaleY = bitmap.height.toFloat() / containerSizePx.height
                                val rect = AndroidRect(
                                    (min(start.x, end.x) * scaleX).toInt(),
                                    (min(start.y, end.y) * scaleY).toInt(),
                                    (max(start.x, end.x) * scaleX).toInt(),
                                    (max(start.y, end.y) * scaleY).toInt()
                                )
                                if (rect.width() > 10 && rect.height() > 10) {
                                    viewModel.applyCrop(rect)
                                }
                            }
                            isCropping = false
                            dragStart = null
                            dragEnd = null
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Apply Crop") }
                    OutlinedButton(
                        onClick = {
                            isCropping = false
                            dragStart = null
                            dragEnd = null
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(onClick = onChangeImage, modifier = Modifier.weight(1f)) {
                    Text("Change Image")
                }
                OutlinedButton(
                    onClick = {
                        viewModel.clearImage()
                        onChangeImage()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.width(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Remove")
                }
            }

            Spacer(Modifier.height(18.dp))

            // ---- Preprocessing tools ----
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Adjust Image", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = viewModel.isGrayscale,
                            onClick = { viewModel.toggleGrayscale() },
                            label = { Text("Grayscale") }
                        )
                        FilterChip(
                            selected = isCropping,
                            onClick = { isCropping = !isCropping },
                            label = { Text("Crop") },
                            leadingIcon = { Icon(Icons.Filled.Crop, contentDescription = null) }
                        )
                        IconButton(onClick = { viewModel.rotateLeft() }) {
                            Icon(Icons.Filled.RotateLeft, contentDescription = "Rotate left")
                        }
                        IconButton(onClick = { viewModel.rotateRight() }) {
                            Icon(Icons.Filled.RotateRight, contentDescription = "Rotate right")
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Brightness", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = viewModel.brightness,
                        onValueChange = { viewModel.setBrightness(it) },
                        valueRange = -100f..100f
                    )

                    Text("Contrast", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = viewModel.contrast,
                        onValueChange = { viewModel.setContrast(it) },
                        valueRange = 0.5f..2f
                    )

                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { viewModel.resetEdits() }) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Reset")
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

private fun Modifier.clipToRoundedCard(): Modifier = this
    .clip(RoundedCornerShape(16.dp))
    .background(Color.Black.copy(alpha = 0.03f))
