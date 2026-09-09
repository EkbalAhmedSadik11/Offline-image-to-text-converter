package com.imagetotext.converter

import android.app.Application
import com.imagetotext.converter.ocr.TesseractHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point.
 *
 * On startup we kick off a background copy of the bundled Tesseract
 * language files (the .traineddata files under assets/tessdata) into the
 * app's private storage, since Tesseract can only read them from a real
 * file path, not directly from the APK. This never touches the network -
 * the files are already inside the installed APK.
 */
class ImageToTextApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            TesseractHelper(this@ImageToTextApp).ensureLanguageDataReady()
        }
    }
}
