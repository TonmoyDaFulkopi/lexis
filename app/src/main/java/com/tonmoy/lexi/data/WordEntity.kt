package com.tonmoy.lexi.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class LookupState { PENDING, FOUND, NOT_FOUND, FAILED }

@Entity(tableName = "words", indices = [Index(value = ["word"], unique = true)])
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Stored lowercase-trimmed for duplicate detection; [display] keeps the user's casing. */
    val word: String,
    val display: String,
    val phonetic: String? = null,
    val partOfSpeech: String? = null,
    val definition: String? = null,
    val example: String? = null,
    /** JSON-encoded List<Sense>, the full lookup result for the detail screen. */
    val sensesJson: String? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val learned: Boolean = false,
    val reviewCount: Int = 0,
    val lookupState: LookupState = LookupState.PENDING,
) {
    val senses: List<Sense>
        get() = sensesJson?.let { runCatching { json.decodeFromString<List<Sense>>(it) }.getOrNull() }
            ?: emptyList()

    companion object {
        val json = Json { ignoreUnknownKeys = true }
        fun encodeSenses(senses: List<Sense>) = json.encodeToString(senses)
    }
}

@Serializable
data class Sense(
    val partOfSpeech: String,
    val definitions: List<Definition>,
    val synonyms: List<String> = emptyList(),
)

@Serializable
data class Definition(val text: String, val example: String? = null)
