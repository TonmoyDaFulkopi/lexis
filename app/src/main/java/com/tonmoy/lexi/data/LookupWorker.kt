package com.tonmoy.lexi.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.tonmoy.lexi.container
import java.util.concurrent.TimeUnit

/**
 * Fetches a word's meaning. Runs through WorkManager so a lookup survives the quick-add sheet
 * closing or the app being killed, waits for a network connection, and retries with backoff.
 */
class LookupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_ID, -1)
        if (id < 0) return Result.failure()
        val done = applicationContext.container.repository
            .lookup(id, finalAttempt = runAttemptCount >= MAX_RUNS - 1)
        return if (done) Result.success() else Result.retry()
    }

    companion object {
        private const val KEY_ID = "word_id"
        private const val MAX_RUNS = 6

        fun enqueue(context: Context, id: Long, replace: Boolean = false) {
            val request = OneTimeWorkRequestBuilder<LookupWorker>()
                .setInputData(workDataOf(KEY_ID to id))
                .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "lookup-$id",
                if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}
