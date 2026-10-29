package com.tonmoy.lexi.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Looks words up in https://dictionaryapi.dev and fills gaps (missing pronunciation, or no
 * entry at all) from https://www.datamuse.com/api. Both are free and need no key.
 */
class DictionaryApi {

    sealed interface Result {
        data class Found(val phonetic: String?, val senses: List<Sense>) : Result
        data object NotFound : Result
        /** Network trouble; worth retrying later. */
        data object Failed : Result
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun lookup(word: String): Result {
        val primary = lookupFreeDictionary(word)
        if (primary is Result.Found && primary.phonetic != null) return primary

        val fallback = lookupDatamuse(word)
        return when {
            primary is Result.Found ->
                primary.copy(phonetic = (fallback as? Result.Found)?.phonetic)
            fallback is Result.Found && fallback.senses.isNotEmpty() -> fallback
            primary is Result.NotFound && fallback !is Result.Failed -> Result.NotFound
            else -> Result.Failed
        }
    }

    private suspend fun lookupFreeDictionary(word: String): Result {
        val response = get("https://api.dictionaryapi.dev/api/v2/entries/en/${encode(word)}")
            ?: return Result.Failed
        if (response.code == 404) return Result.NotFound
        return runCatching { parseFreeDictionary(json.decodeFromString<List<EntryDto>>(response.body)) }
            .getOrDefault(Result.Failed)
    }

    private suspend fun lookupDatamuse(word: String): Result {
        val response = get("https://api.datamuse.com/words?sp=${encode(word)}&md=dr&ipa=1&max=1")
            ?: return Result.Failed
        val match = runCatching { json.decodeFromString<List<DatamuseDto>>(response.body) }
            .getOrNull()
            ?.firstOrNull { it.word.equals(word.trim(), ignoreCase = true) }
            ?: return Result.NotFound

        val phonetic = match.tags.firstOrNull { it.startsWith("ipa_pron:") }
            ?.removePrefix("ipa_pron:")?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { "/$it/" }
        val senses = match.defs
            .mapNotNull { d ->
                val parts = d.split('\t', limit = 2)
                if (parts.size == 2) posName(parts[0]) to parts[1].trim() else null
            }
            .groupBy({ it.first }, { it.second })
            .map { (pos, defs) -> Sense(pos, defs.take(5).map { Definition(it) }) }
        return Result.Found(phonetic, senses)
    }

    private class Response(val code: Int, val body: String)

    /**
     * GET with retries. Returns null when the network failed every attempt, or a response for
     * 2xx/404. Rate limits (429), server errors and dropped connections are retried with backoff.
     */
    private suspend fun get(url: String): Response? = withContext(Dispatchers.IO) {
        repeat(MAX_ATTEMPTS) { attempt ->
            if (attempt > 0) delay(RETRY_DELAYS_MS[attempt - 1])
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("User-Agent", "Lexi/1.0 (Android)")
                // Pooled keep-alive sockets go stale while the app sits in the background, which
                // was the source of the "failed" lookups even with a working connection.
                conn.setRequestProperty("Connection", "close")
                val code = conn.responseCode
                when {
                    code in 200..299 ->
                        return@withContext Response(code, conn.inputStream.bufferedReader().use { it.readText() })
                    code == 404 -> return@withContext Response(code, "")
                    // 429 / 5xx: fall through and retry
                }
            } catch (_: IOException) {
                // Timeout, DNS hiccup, connection reset: retry.
            } finally {
                conn.disconnect()
            }
        }
        null
    }

    private fun encode(word: String) = URLEncoder.encode(word.trim(), "UTF-8").replace("+", "%20")

    private fun posName(code: String) = when (code) {
        "n" -> "noun"
        "v" -> "verb"
        "adj" -> "adjective"
        "adv" -> "adverb"
        else -> code
    }

    private fun parseFreeDictionary(entries: List<EntryDto>): Result {
        if (entries.isEmpty()) return Result.NotFound
        val phonetic = entries.firstNotNullOfOrNull { e ->
            e.phonetic?.takeIf { it.isNotBlank() }
                ?: e.phonetics.firstNotNullOfOrNull { it.text?.takeIf { t -> t.isNotBlank() } }
        }
        // Merge senses that share a part of speech across entries.
        val senses = entries.flatMap { it.meanings }
            .groupBy { it.partOfSpeech }
            .map { (pos, meanings) ->
                Sense(
                    partOfSpeech = pos,
                    definitions = meanings.flatMap { it.definitions }
                        .map { Definition(it.definition, it.example) }
                        .take(5),
                    synonyms = meanings.flatMap { m -> m.synonyms + m.definitions.flatMap { it.synonyms } }
                        .distinct()
                        .take(8),
                )
            }
        return if (senses.isEmpty()) Result.NotFound else Result.Found(phonetic, senses)
    }

    @Serializable
    private data class EntryDto(
        val word: String = "",
        val phonetic: String? = null,
        val phonetics: List<PhoneticDto> = emptyList(),
        val meanings: List<MeaningDto> = emptyList(),
    )

    @Serializable
    private data class PhoneticDto(val text: String? = null)

    @Serializable
    private data class MeaningDto(
        val partOfSpeech: String = "",
        val definitions: List<DefinitionDto> = emptyList(),
        val synonyms: List<String> = emptyList(),
    )

    @Serializable
    private data class DefinitionDto(
        val definition: String = "",
        val example: String? = null,
        val synonyms: List<String> = emptyList(),
    )

    @Serializable
    private data class DatamuseDto(
        val word: String = "",
        val tags: List<String> = emptyList(),
        val defs: List<String> = emptyList(),
    )

    private companion object {
        const val MAX_ATTEMPTS = 3
        val RETRY_DELAYS_MS = longArrayOf(800, 2_000)
    }
}
