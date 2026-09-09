package com.imagetotext.converter.util

/**
 * Character / word counting that behaves reasonably for Bangla, English
 * and Arabic text mixed together. Bangla and Arabic both separate words
 * with plain spaces (like English), so a whitespace split is a simple and
 * reliable approach for all three scripts at once.
 */
object TextStats {

    data class Stats(val characters: Int, val words: Int)

    private val wordSplitRegex = Regex("\\s+")

    fun count(text: String): Stats {
        val characters = text.length
        val words = if (text.isBlank()) {
            0
        } else {
            text.trim().split(wordSplitRegex).count { it.isNotBlank() }
        }
        return Stats(characters, words)
    }
}
