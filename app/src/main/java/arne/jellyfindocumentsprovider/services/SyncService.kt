package arne.jellyfindocumentsprovider.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import arne.jellyfin.vfs.DatabaseSync
import arne.jellyfindocumentsprovider.MainActivity
import arne.jellyfindocumentsprovider.R

class SyncService : Service() {

    companion object {
        private const val CHANNEL_ID = "sync_progress"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Database Sync Progress",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun sendProgressNotification(progress: DatabaseSync.Progress) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notificationId = 1 // Unique ID for the notification

        // Create an intent for the notification tap action (optional)
        val intent = Intent(this, MainActivity::class.java) // Replace with your activity
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle(progress.step.name)
                .setContentText(progress.extra)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your notification icon
                .setContentIntent(pendingIntent)
                .setProgress(progress.current, progress.current, false) // Set the progress bar
                .setPriority(NotificationCompat.PRIORITY_LOW).build()

        // Send the notification
        notificationManager.notify(notificationId, notification)
    }

    inner class LocalBinder : Binder() {
        fun getService(): SyncService = this@SyncService

        fun sendMessage(message: Map<String, Int>) {
            // Handle the received message here
            // For example, you could log it or process it as needed
            message.forEach { (key, value) ->
                // Process each key-value pair
                println("Key: $key, Value: $value") // Replace with your logic
            }
        }
    }
}