package arne.jellyfin.vfs

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import arne.hacks.fromMap
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import logcat.logcat

class DatabaseSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val request = fromMap<SyncRequest>(inputData.keyValueMap)
        val idList = if (request.all) {
            ObjectBox.server.all
        } else {
            val fromDB = ObjectBox.server.get(request.id.toList())
            if (fromDB.size != request.id.size) {
                return Result.failure(
                    workDataOf(
                        "reason" to "Some of the server info not found"
                    )
                )
            }
            fromDB
        }

        return runBlocking {
            sync(idList)
        }
    }


    private suspend fun sync(credential: MutableList<JellyfinServer>): Result {
        if (credential.isEmpty()) {
            logcat(LogPriority.ERROR) {
                "some of the credential not found"
            }
            return Result.failure()
        }

        /**
         * -1 means pending
         * [0, 100) means progress
         * 100 means done
         */
        setProgressAsync(
            workDataOf(
                *credential.associate { it.uuid to -1 }.toList().toTypedArray()
            )
        )
        credential.forEach { c ->
            logcat {
                "syncing server: ${c.info}"
            }

            val sync = DatabaseSync(c.asAccessor(applicationContext))
            sync.sync {
                setProgressAsync(
                    workDataOf(c.uuid to it)
                )
            }
        }

        return Result.success()
    }

    @Suppress("ArrayInDataClass")
    data class SyncRequest(
        val id: Array<Long> = emptyArray(),
        val all: Boolean = false
    )
}
