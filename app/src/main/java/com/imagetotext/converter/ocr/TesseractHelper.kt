package com.imagetotext.converter.ocr

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Thin wrapper around Tesseract4Android that:
 *  - copies the bundled ben/eng/ara language files out of assets/ the
 *    first time the app runs (Tesseract needs a real file path, it cannot
 *    read straight out of the APK), and
 *  - runs OCR on a single Bitmap using all three languages at once, so
 *    the caller never has to pick a language.
 *
 * All work here is 100% local: no network call is made anywhere in this
 * class or in the underlying native library.
 */
class TesseractHelper(private val context: Context) {

    companion object {
        /** eng+ben+ara loaded together so mixed-language images are handled in one pass. */
        const val RECOGNITION_LANGUAGES = "eng+ben+ara"
        private val LANGUAGE_FILES = listOf("eng", "ben", "ara")
    }

    /** Parent folder that Tesseract expects; it looks for a "tessdata" sub-folder inside it. */
    private val tessDataParentDir: File
        get() = File(context.filesDir, "tesseract")

    /**
     * Copies every *.traineddata file from assets/tessdata into private
     * storage if it isn't already there. Safe to call many times - after
     * the first run this is just a handful of File.exists() checks.
     */
    suspend fun ensureLanguageDataReady() = withContext(Dispatchers.IO) {
        val tessDataDir = File(tessDataParentDir, "tessdata")
        if (!tessDataDir.exists()) {
            tessDataDir.mkdirs()
        }
        for (lang in LANGUAGE_FILES) {
            val outFile = File(tessDataDir, "$lang.traineddata")
            if (outFile.exists() && outFile.length() > 0L) continue
            context.assets.open("tessdata/$lang.traineddata").use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    /**
     * Runs OCR on [bitmap] and returns the recognized text. [onProgress] is
     * called from a background thread with values 0-100 while recognition
     * is running.
     */
    suspend fun recognize(bitmap: Bitmap, onProgress: (Int) -> Unit): String =
        withContext(Dispatchers.Default) {
            ensureLanguageDataReady()

            val api = TessBaseAPI { progressValues ->
                onProgress(progressValues.getPercent())
            }
            try {
                val initialized = api.init(
                    tessDataParentDir.absolutePath,
                    RECOGNITION_LANGUAGES,
                    TessBaseAPI.OEM_LSTM_ONLY
                )
                if (!initialized) {
                    throw IllegalStateException("Could not initialize the offline OCR engine.")
                }

                api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
                api.setImage(bitmap)

                // Triggers the actual recognition pass (with progress callbacks).
                api.getHOCRText(0)
                api.getUTF8Text() ?: ""
            } finally {
                api.clear()
                api.recycle()
            }
        }
}
