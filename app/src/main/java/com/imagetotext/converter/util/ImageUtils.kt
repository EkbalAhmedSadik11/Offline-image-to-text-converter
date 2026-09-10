package com.imagetotext.converter.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File

/**
 * All bitmap loading / editing helpers used by the app.
 *
 * Large photos from a modern phone camera (12+ megapixels) are downsampled
 * to [MAX_DIMENSION] on load so the app never tries to hold an enormous
 * bitmap in memory - this is what keeps large-image OCR from crashing the
 * app with an OutOfMemoryError. (The manifest also opts into a larger app
 * heap as a safety net.)
 */
object ImageUtils {

    private const val MAX_DIMENSION = 2600

    /** Loads, downsamples and EXIF-rotates an image the user picked from the gallery. */
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        } ?: throw IllegalArgumentException("Unable to decode the selected image.")

        val rotated = try {
            resolver.openInputStream(uri)?.use { input ->
                applyExifRotation(decoded, ExifInterface(input))
            } ?: decoded
        } catch (e: Exception) {
            decoded
        }

        return ensureMaxDimension(rotated, MAX_DIMENSION)
    }

    /** Loads, downsamples and EXIF-rotates a photo captured by the in-app camera. */
    fun loadBitmapFromFile(filePath: String): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, bounds)

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeFile(filePath, decodeOptions)
            ?: throw IllegalArgumentException("Unable to decode the captured photo.")

        val rotated = try {
            applyExifRotation(decoded, ExifInterface(filePath))
        } catch (e: Exception) {
            decoded
        }

        return ensureMaxDimension(rotated, MAX_DIMENSION)
    }

    /** A brand-new, empty file the camera app can write the captured photo into. */
    fun createImageCaptureFile(context: Context): File {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        return File(dir, "capture_${System.currentTimeMillis()}.jpg")
    }

    /** Content:// URI for [file], handed to the system camera app which cannot see our real file paths. */
    fun getUriForCaptureFile(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360f == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Renders [source] with grayscale / brightness / contrast applied.
     * [brightness] is in the range -100..100, [contrast] in 0.5..2.0 (1.0 = unchanged).
     * The source bitmap is never modified, so adjustments can always be
     * recomputed from the original without compounding quality loss.
     */
    fun applyAdjustments(source: Bitmap, grayscale: Boolean, brightness: Float, contrast: Float): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val matrix = ColorMatrix()
        if (grayscale) {
            matrix.setSaturation(0f)
        }
        val contrastAndBrightness = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        matrix.postConcat(contrastAndBrightness)

        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    /**
     * Corrects a decoded bitmap using its EXIF orientation tag. Camera
     * photos are frequently stored "sideways" (sensor orientation) with an
     * EXIF tag describing how to display them correctly; a photo left in
     * the wrong orientation looks fine to a human (phones auto-rotate the
     * preview) but is disastrous for OCR, which expects roughly upright
     * text. This handles all 8 possible EXIF orientation values, including
     * the mirrored/transposed ones some camera apps use, not just the 3
     * plain rotations.
     */
    private fun applyExifRotation(bitmap: Bitmap, exif: ExifInterface): Bitmap {
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun ensureMaxDimension(bitmap: Bitmap, maxDim: Int): Bitmap {
        val largestSide = maxOf(bitmap.width, bitmap.height)
        if (largestSide <= maxDim) return bitmap
        val scale = maxDim.toFloat() / largestSide
        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sampleSize = 1
        var w = width
        var h = height
        while (w / 2 >= maxDim || h / 2 >= maxDim) {
            sampleSize *= 2
            w /= 2
            h /= 2
        }
        return sampleSize
    }
}
