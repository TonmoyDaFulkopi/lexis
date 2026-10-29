package com.tonmoy.lexi.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE id = :id")
    fun observe(id: Long): Flow<WordEntity?>

    @Query("SELECT * FROM words")
    suspend fun all(): List<WordEntity>

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun get(id: Long): WordEntity?

    @Query("SELECT * FROM words WHERE word = :normalized LIMIT 1")
    suspend fun findByWord(normalized: String): WordEntity?

    @Query("SELECT * FROM words ORDER BY RANDOM() LIMIT 1")
    suspend fun random(): WordEntity?

    @Query("SELECT COUNT(*) FROM words")
    suspend fun count(): Int

    @Query("SELECT * FROM words WHERE lookupState IN ('PENDING', 'FAILED')")
    suspend fun pendingLookups(): List<WordEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(word: WordEntity): Long

    @Update
    suspend fun update(word: WordEntity)

    @Delete
    suspend fun delete(word: WordEntity)
}
