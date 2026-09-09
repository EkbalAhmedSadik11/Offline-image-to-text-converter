package com.imagetotext.converter.data.history

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM ocr_history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insert(entity: HistoryEntity): Long

    @Query("SELECT * FROM ocr_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): HistoryEntity?

    @Delete
    suspend fun delete(entity: HistoryEntity)

    @Query("DELETE FROM ocr_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM ocr_history")
    suspend fun deleteAll()
}
