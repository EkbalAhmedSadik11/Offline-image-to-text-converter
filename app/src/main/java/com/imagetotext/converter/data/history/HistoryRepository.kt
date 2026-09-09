package com.imagetotext.converter.data.history

import android.content.Context
import com.imagetotext.converter.ocr.LanguageDetector
import com.imagetotext.converter.util.TextStats
import kotlinx.coroutines.flow.Flow

class HistoryRepository(context: Context) {

    private val dao = HistoryDatabase.getInstance(context).historyDao()

    fun observeHistory(): Flow<List<HistoryEntity>> = dao.observeAll()

    suspend fun saveResult(text: String): Long {
        val stats = TextStats.count(text)
        val languages = LanguageDetector.detect(text).languages
        val entity = HistoryEntity(
            text = text,
            createdAt = System.currentTimeMillis(),
            detectedLanguages = languages.joinToString(", "),
            characterCount = stats.characters,
            wordCount = stats.words
        )
        return dao.insert(entity)
    }

    suspend fun getById(id: Long): HistoryEntity? = dao.getById(id)

    suspend fun delete(entity: HistoryEntity) = dao.delete(entity)

    suspend fun deleteAll() = dao.deleteAll()
}
