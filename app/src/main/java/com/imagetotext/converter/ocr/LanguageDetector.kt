package com.imagetotext.converter.ocr

/**
 * Best-effort, on-device detection of which of the three supported scripts
 * appear in a block of already-recognized text. This is only used to show
 * a friendly "Detected: Bangla, English" label and to decide reading
 * direction for the result editor - it never claims perfect accuracy and
 * it does not run a second OCR pass, it just inspects the Unicode ranges
 * of the characters Tesseract already produced.
 *
 * Ranges are written as explicit \\uXXXX escapes (rather than pasting raw
 * script characters into the source file) so they stay unambiguous and
 * safe to read/edit in any editor or encoding:
 *  - Bengali:                     U+0980-U+09FF
 *  - Arabic:                      U+0600-U+06FF
 *  - Arabic Supplement:           U+0750-U+077F
 *  - Arabic Extended-A:           U+08A0-U+08FF
 *  - Arabic Presentation Forms-A: U+FB50-U+FDFF
 *  - Arabic Presentation Forms-B: U+FE70-U+FEFF
 */
object LanguageDetector {

    private val bengaliRange = 'ঀ'..'৿'

    private val arabicRanges = listOf(
        '؀'..'ۿ',
        'ݐ'..'ݿ',
        'ࢠ'..'ࣿ',
        'ﭐ'..'﷿',
        'ﹰ'..'﻿'
    )

    private val latinRanges = listOf('A'..'Z', 'a'..'z')

    data class Detection(
        val languages: List<String>,
        val isArabicDominant: Boolean
    )

    fun detect(text: String): Detection {
        var bengaliCount = 0
        var arabicCount = 0
        var latinCount = 0

        for (ch in text) {
            when {
                ch in bengaliRange -> bengaliCount++
                arabicRanges.any { ch in it } -> arabicCount++
                latinRanges.any { ch in it } -> latinCount++
            }
        }

        val found = mutableListOf<String>()
        if (bengaliCount > 0) found.add("Bangla")
        if (latinCount > 0) found.add("English")
        if (arabicCount > 0) found.add("Arabic")

        val isArabicDominant = arabicCount > 0 && arabicCount >= bengaliCount && arabicCount >= latinCount

        return Detection(found, isArabicDominant)
    }
}
