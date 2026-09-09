package com.imagetotext.converter.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.imagetotext.converter.data.history.HistoryEntity
import com.imagetotext.converter.data.history.HistoryRepository
import com.imagetotext.converter.data.settings.ThemePreferences
import com.imagetotext.converter.ocr.LanguageDetector
import com.imagetotext.converter.ocr.TesseractHelper
import com.imagetotext.converter.util.FileSaver
import com.imagetotext.converter.util.ImageUtils
import com.imagetotext.converter.util.TextStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Drives the whole "select/capture -> edit -> extract -> result" flow. */
sealed class OcrState {
    data object Idle : OcrState()
    data class Preparing(val message: String) : OcrState()
    data class Recognizing(val percent: Int) : OcrState()
    data class Success(val text: String) : OcrState()
    data class Error(val message: String) : OcrState()
}

class OcrViewModel(application: Application) : AndroidViewModel(application) {

    private val tesseractHelper = TesseractHelper(application)
    private val historyRepository = HistoryRepository(application)
    private val themePreferences = ThemePreferences(application)

    // ----- Image state -----

    var originalBitmap by mutableStateOf<Bitmap?>(null)
        private set
    var editedBitmap by mutableStateOf<Bitmap?>(null)
        private set

    var isGrayscale by mutableStateOf(false)
        private set
    var brightness by mutableStateOf(0f)
        private set
    var contrast by mutableStateOf(1f)
        private set
    var rotationDegrees by mutableStateOf(0f)
        private set

    var imageError by mutableStateOf<String?>(null)
        private set

    // ----- OCR state -----

    var ocrState by mutableStateOf<OcrState>(OcrState.Idle)
        private set

    // ----- Result state -----

    var extractedText by mutableStateOf("")
    var lastDetectedLanguages by mutableStateOf<List<String>>(emptyList())
        private set
    var isArabicDominant by mutableStateOf(false)
        private set

    // ----- Theme (persisted locally with DataStore) -----

    val isDarkMode: StateFlow<Boolean?> = themePreferences.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setDarkMode(enabled) }
    }

    // ----- History (persisted locally with Room) -----

    val history: StateFlow<List<HistoryEntity>> = historyRepository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ----- Image loading -----

    fun loadImageFromGallery(uri: Uri) {
        loadImageInternal { ImageUtils.loadBitmapFromUri(getApplication(), uri) }
    }

    fun loadImageFromCameraFile(filePath: String) {
        loadImageInternal { ImageUtils.loadBitmapFromFile(filePath) }
    }

    private fun loadImageInternal(decode: () -> Bitmap) {
        viewModelScope.launch {
            imageError = null
            try {
                val bitmap = withContext(Dispatchers.IO) { decode() }
                originalBitmap = bitmap
                resetEdits()
                ocrState = OcrState.Idle
            } catch (e: Exception) {
                imageError = "Please select a supported image format."
            }
        }
    }

    fun clearImage() {
        originalBitmap = null
        editedBitmap = null
        imageError = null
        ocrState = OcrState.Idle
        resetEdits()
    }

    // ----- Preprocessing -----
    // originalBitmap is never modified in place; every adjustment is
    // recomputed from it so repeated edits never compound image quality
    // loss and Reset always returns to a clean image.

    fun toggleGrayscale() {
        isGrayscale = !isGrayscale
        recomputeEditedBitmap()
    }

    fun setBrightness(value: Float) {
        brightness = value
        recomputeEditedBitmap()
    }

    fun setContrast(value: Float) {
        contrast = value
        recomputeEditedBitmap()
    }

    fun rotateLeft() {
        rotationDegrees = (rotationDegrees - 90f).mod(360f)
        recomputeEditedBitmap()
    }

    fun rotateRight() {
        rotationDegrees = (rotationDegrees + 90f).mod(360f)
        recomputeEditedBitmap()
    }

    fun resetEdits() {
        isGrayscale = false
        brightness = 0f
        contrast = 1f
        rotationDegrees = 0f
        recomputeEditedBitmap()
    }

    /** [rect] must already be in the source bitmap's own pixel coordinates. */
    fun applyCrop(rect: Rect) {
        val source = editedBitmap ?: return
        val cropped = ImageUtils.cropBitmap(source, rect)
        originalBitmap = cropped
        resetEdits()
    }

    private fun recomputeEditedBitmap() {
        val source = originalBitmap
        if (source == null) {
            editedBitmap = null
            return
        }
        val adjusted = ImageUtils.applyAdjustments(source, isGrayscale, brightness, contrast)
        editedBitmap = if (rotationDegrees != 0f) {
            ImageUtils.rotateBitmap(adjusted, rotationDegrees)
        } else {
            adjusted
        }
    }

    // ----- OCR -----

    fun runOcr() {
        // Guard against starting a second OCR pass while one is already running.
        if (ocrState is OcrState.Preparing || ocrState is OcrState.Recognizing) return

        val bitmap = editedBitmap ?: originalBitmap
        if (bitmap == null) {
            ocrState = OcrState.Error("Please select or capture an image first.")
            return
        }

        viewModelScope.launch {
            ocrState = OcrState.Preparing("Preparing OCR engine...")
            try {
                val text = tesseractHelper.recognize(bitmap) { percent ->
                    ocrState = OcrState.Recognizing(percent)
                }
                if (text.isBlank()) {
                    ocrState = OcrState.Error("No readable text was detected in this image.")
                    return@launch
                }
                val detection = LanguageDetector.detect(text)
                lastDetectedLanguages = detection.languages
                isArabicDominant = detection.isArabicDominant
                extractedText = text
                ocrState = OcrState.Success(text)
                historyRepository.saveResult(text)
            } catch (e: Exception) {
                ocrState = OcrState.Error("Text extraction failed. Please try another image.")
            }
        }
    }

    fun resetOcrState() {
        ocrState = OcrState.Idle
    }

    // ----- Result actions -----

    fun updateExtractedText(newText: String) {
        extractedText = newText
    }

    fun clearExtractedText() {
        extractedText = ""
        lastDetectedLanguages = emptyList()
    }

    fun textStats() = TextStats.count(extractedText)

    fun saveExtractedText(): Uri? = FileSaver.saveTextFile(getApplication(), extractedText)

    fun buildShareIntent() = FileSaver.buildShareIntent(extractedText)

    // ----- History actions -----

    fun loadFromHistory(entity: HistoryEntity) {
        extractedText = entity.text
        val detection = LanguageDetector.detect(entity.text)
        lastDetectedLanguages = detection.languages
        isArabicDominant = detection.isArabicDominant
    }

    fun deleteHistoryItem(entity: HistoryEntity) {
        viewModelScope.launch { historyRepository.delete(entity) }
    }

    fun clearAllHistory() {
        viewModelScope.launch { historyRepository.deleteAll() }
    }
}
