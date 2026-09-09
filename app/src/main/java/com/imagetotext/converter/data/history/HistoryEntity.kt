package com.imagetotext.converter.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One saved OCR result. Everything is stored only in the app's private
 * on-device SQLite database - there is no cloud sync, no account and no
 * network call involved anywhere in this file.
 */
@Entity(tableName = "ocr_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val text: String,
    val createdAt: Long,
    val detectedLanguages: String,
    val characterCount: Int,
    val wordCount: Int
)
