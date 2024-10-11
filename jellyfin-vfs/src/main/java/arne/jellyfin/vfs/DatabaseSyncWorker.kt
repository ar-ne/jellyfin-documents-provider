package arne.jellyfin.vfs

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.impl.utils.futures.SettableFuture
import androidx.work.workDataOf
import arne.hacks.fromMap
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import logcat.logcat

class DatabaseSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "DatabaseSyncWorker"
    }

    @SuppressLint("RestrictedApi")
    override fun getForegroundInfoAsync(): ListenableFuture<ForegroundInfo> {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Sync Database",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(arne.jfdp.hacks.R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentTitle("Jellyfin VFS")
            .setLocalOnly(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setContentText("Updating widget")
            .build()
        return SettableFuture.create<ForegroundInfo?>().apply {
            set(ForegroundInfo(1, notification))
        }
    }

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
