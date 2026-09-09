package com.imagetotext.converter.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/**
 * Saving, sharing and clipboard helpers for the extracted text. Saving
 * always writes a plain UTF-8 .txt file locally on the device - nothing is
 * ever uploaded anywhere.
 */
object FileSaver {

    private const val FILE_NAME = "extracted-text.txt"

    /** Saves [text] as extracted-text.txt into the device's Downloads folder (or app folder on old Android). */
    fun saveTextFile(context: Context, text: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, text)
        } else {
            saveViaLegacyFile(context, text)
        }
    }

    private fun saveViaMediaStore(context: Context, text: String): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { output ->
            output.write(text.toByteArray(Charsets.UTF_8))
        }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    private fun saveViaLegacyFile(context: Context, text: String): Uri {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, FILE_NAME)
        file.writeText(text, Charsets.UTF_8)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun buildShareIntent(text: String): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        return Intent.createChooser(sendIntent, "Share extracted text")
    }
}
