package arne.jellyfin.vfs

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import arne.hacks.fromMap
import arne.hacks.logcat
import arne.hacks.toMap
import kotlinx.coroutines.runBlocking
import logcat.LogPriority

class DatabaseSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val request = fromMap<SyncRequest>(inputData.keyValueMap)
        val idList = if (request.all) {
            ObjectBox.credential.all
        } else {
            val fromDB = ObjectBox.credential.get(request.id)
            if (fromDB.size == request.id.size) {
                return Result.failure()
            }
            fromDB
        }

        return runBlocking {
            sync(idList)
        }
    }


    private suspend fun sync(credential: MutableList<JellyfinCredential>): Result {
        if (credential.isEmpty()) {
            logcat(LogPriority.ERROR) {
                "some of the credential not found"
            }
            return Result.failure()
        }

        credential.forEachIndexed { index, c ->
            logcat {
                "syncing server: ${c.serverName} ${c.url}"
            }

            val sync = DatabaseSync(c.asAccessor(applicationContext))
            sync.sync { step ->
                val progress = Progress(
                    total = credential.size,
                    finished = index,
                    current = 100 * step.current / step.total,
                    id = c.id
                )
                setProgressAsync(
                    workDataOf(
                        *progress.toMap().toList().toTypedArray()
                    )
                )
            }
        }

        return Result.success()
    }

    data class Progress(
        val total: Int,
        val finished: Int,
        val current: Int,
        val id: Long,
    )

    data class SyncRequest(
        val all: Boolean = false,
        val id: List<Long>
    )
}