package arne.provider

import arne.hacks.short
import arne.jellyfin.vfs.FileStreamFactory
import arne.jellyfin.vfs.VirtualFile
import io.ktor.utils.io.ByteReadChannel
import logcat.LogPriority
import logcat.logcat
import java.nio.file.Path

object RandomAccessBucket {
    fun init(tempFileRoot: Path) {
        this.tempFileRoot = tempFileRoot
    }

    private lateinit var tempFileRoot: Path
    private val mapper = HashMap<String, URLRandomAccess>()
    private val refCnt = HashMap<String, Int>()

    fun proxy(fsf: FileStreamFactory, vf: VirtualFile, bitrate: Int) =
        URLProxyFileDescriptorCallback(getRA(fsf, vf, bitrate)) {
            releaseRA(vf.documentId)
        }

    private fun getRA(fsf: FileStreamFactory, vf: VirtualFile, bitrate: Int): URLRandomAccess {
        val key = vf.documentId
        synchronized(this) {
            refCnt[key] = refCnt.getOrDefault(key, 0) + 1
            if (mapper.containsKey(key))
                return mapper[key]!!
            else mapper[key] = newBufferedRA(fsf, vf, bitrate)

            logcat(LogPriority.DEBUG) { "get(${key.short}): refCnt = ${refCnt[key]}" }
            return mapper[key]!!
        }
    }

    private fun releaseRA(key: String) {
        synchronized(this) {
            val after = refCnt.getOrDefault(key, 0) - 1
            if (after <= 0) {
                refCnt.remove(key)
                val remove = mapper.remove(key)
                remove?.close()
            } else {
                refCnt[key] = after
            }

            logcat(LogPriority.DEBUG) { "release(${key.short}): refCnt = $after" }
        }
    }

    private fun newBufferedRA(
        fsf: FileStreamFactory,
        vf: VirtualFile,
        bitrate: Int
    ) =
        BufferedURLRandomAccess(
            vf = vf,
            streamFactory = fsf,
            bufferSizeKB = 128,
            bitrate = bitrate,
            bufferFile = tempFileRoot.resolve(vf.documentId).toFile().apply {
                createNewFile()
            }
        )
}

