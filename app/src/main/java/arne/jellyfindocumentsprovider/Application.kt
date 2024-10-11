package arne.jellyfindocumentsprovider

import android.app.Application
import arne.jellyfin.vfs.ObjectBox
import arne.provider.RandomAccessBucket
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        LogcatLogger.install(AndroidLogcatLogger(LogPriority.DEBUG))
        ObjectBox.init(this)
        RandomAccessBucket.init(applicationContext.cacheDir.toPath())
    }
}