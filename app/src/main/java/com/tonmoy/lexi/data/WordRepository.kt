package com.tonmoy.lexi.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class WordRepository(
    private val context: Context,
    private val dao: WordDao,
    private val api: DictionaryApi,
    private val onWordsChanged: suspend () -> Unit,
) {
    sealed interface SaveResult {
        val id: Long
        data class Added(override val id: Long) : SaveResult
        data class AlreadySaved(override val id: Long) : SaveResult
    }

    fun observeAll(): Flow<List<WordEntity>> = dao.observeAll()
    fun observe(id: Long): Flow<WordEntity?> = dao.observe(id)
    suspend fun random(): WordEntity? = dao.random()
    suspend fun count(): Int = dao.count()

    /** Saves right away, then fills in the definition in the background. */
    suspend fun save(raw: String): SaveResult {
        val display = clean(raw)
        val normalized = display.lowercase()
        dao.findByWord(normalized)?.let { return SaveResult.AlreadySaved(it.id) }
        val id = dao.insert(WordEntity(word = normalized, display = display))
        onWordsChanged()
        LookupWorker.enqueue(context, id)
        return SaveResult.Added(id)
    }

    /** Re-fetches one word's meaning, keeping what's already shown until new data arrives. */
    suspend fun resync(id: Long) {
        dao.get(id)?.let { dao.update(it.copy(lookupState = LookupState.PENDING)) }
        LookupWorker.enqueue(context, id, replace = true)
    }

    /** Re-fetches every word. Returns how many were queued. */
    suspend fun resyncAll(): Int {
        val words = dao.all()
        words.forEach {
            dao.update(it.copy(lookupState = LookupState.PENDING))
            LookupWorker.enqueue(context, it.id, replace = true)
        }
        return words.size
    }

    /**
     * Performs one lookup attempt for [LookupWorker]. Returns true when finished (found, not found,
     * or gave up on the final attempt) and false when the worker should retry later.
     */
    suspend fun lookup(id: Long, finalAttempt: Boolean): Boolean {
        val current = dao.get(id) ?: return true
        val hadMeaning = current.definition != null
        val result = api.lookup(current.word)

        // Re-read so a note or learned flag changed during the network call isn't overwritten.
        val latest = dao.get(id) ?: return true
        val (updated, done) = when (result) {
            is DictionaryApi.Result.Found -> {
                val first = result.senses.first()
                latest.copy(
                    phonetic = result.phonetic ?: latest.phonetic,
                    partOfSpeech = first.partOfSpeech,
                    definition = first.definitions.firstOrNull()?.text,
                    example = result.senses.flatMap { it.definitions }
                        .firstNotNullOfOrNull { it.example },
                    sensesJson = WordEntity.encodeSenses(result.senses),
                    lookupState = LookupState.FOUND,
                ) to true
            }
            // A resync that comes up empty shouldn't wipe a meaning we already have.
            DictionaryApi.Result.NotFound ->
                latest.copy(lookupState = if (hadMeaning) LookupState.FOUND else LookupState.NOT_FOUND) to true
            DictionaryApi.Result.Failed -> when {
                hadMeaning -> latest.copy(lookupState = LookupState.FOUND) to true
                finalAttempt -> latest.copy(lookupState = LookupState.FAILED) to true
                // Keep showing "looking up" while WorkManager retries.
                else -> latest.copy(lookupState = LookupState.PENDING) to false
            }
        }
        dao.update(updated)
        onWordsChanged()
        return done
    }

    /** Called on app start: queues anything that never got a meaning. Already-queued work is kept. */
    suspend fun resumePendingLookups() {
        dao.pendingLookups().forEach { LookupWorker.enqueue(context, it.id) }
    }

    suspend fun updateNote(id: Long, note: String) {
        dao.get(id)?.let { dao.update(it.copy(note = note)) }
    }

    suspend fun setLearned(id: Long, learned: Boolean) {
        dao.get(id)?.let { dao.update(it.copy(learned = learned)) }
    }

    suspend fun recordReview(id: Long, gotIt: Boolean) {
        dao.get(id)?.let {
            dao.update(it.copy(reviewCount = it.reviewCount + 1, learned = gotIt || it.learned))
        }
    }

    suspend fun delete(word: WordEntity) {
        dao.delete(word)
        onWordsChanged()
    }

    suspend fun restore(word: WordEntity) {
        dao.insert(word)
        onWordsChanged()
    }

    companion object {
        /** Trims whitespace and surrounding punctuation that comes along with copied text. */
        fun clean(raw: String): String =
            raw.trim()
                .trim { !it.isLetterOrDigit() && it != '\'' && it != '-' }
                .replace(Regex("\\s+"), " ")

        /** Whether clipboard text looks like a word or short phrase worth pre-filling. */
        fun looksLikeWord(text: String): Boolean {
            val c = clean(text)
            return c.isNotEmpty() && c.length <= 40 && c.split(' ').size <= 4 &&
                c.any { it.isLetter() } && !c.contains("://")
        }
    }
}
