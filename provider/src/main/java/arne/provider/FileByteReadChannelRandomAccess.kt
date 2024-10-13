package arne.provider

import arne.hacks.short
import arne.jellyfin.vfs.CacheChunks
import arne.jellyfin.vfs.FileStreamFactory
import arne.jellyfin.vfs.VirtualFile
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import logcat.logcat
import java.io.File
import java.io.RandomAccessFile
import kotlin.concurrent.Volatile
import kotlin.coroutines.cancellation.CancellationException

class FileByteReadChannelRandomAccess(
    file: File,
    virtualFile: VirtualFile,
    private val fileStreamFactory: FileStreamFactory,
) : RandomAccess() {
    override val length: Long = virtualFile.size
    private var currentJob: Job? = null
    private val cache = CacheFile(file)
    private val logId = virtualFile.documentId.short
    private var channel: FileByteReadChannel? = null

    override fun read(offset: Long, size: Int, data: ByteArray): Int {
        // check if already in cache
        val chunk = cache.offsetInChunks(offset)
        val cacheType = if (chunk == null) {
            ReadType.NO_CACHE
        } else if (chunk.last + 1 >= offset + size) {
            ReadType.ALL_CACHE
        } else {
            ReadType.PARTIAL_CACHE
        }

        logcat(LogPriority.VERBOSE) {
            "read $logId: offset=$offset, size=$size, cacheType=$cacheType, chunk=$chunk"
        }

        when (cacheType) {
            ReadType.NO_CACHE -> {
                return getOrCreateChannelFor(offset).read(offset..offset + size, data, 0)
            }

            ReadType.PARTIAL_CACHE -> {
                val fromDisk = ByteArray((chunk!!.last - offset + 1).toInt())
                val fromDiskSize = read(offset, fromDisk.size, fromDisk)
                fromDisk.copyInto(data, 0, 0, fromDiskSize)

                val restRange = offset + fromDiskSize..offset + size
                return getOrCreateChannelFor(restRange.first).read(restRange, data, fromDiskSize)
            }

            ReadType.ALL_CACHE -> {
                with(cache) {
                    return chunk!!.read(offset, size, data)
                }
            }
        }
    }

    private fun getOrCreateChannelFor(start: Long): FileByteReadChannel {
        synchronized(this) {
            if (channel != null && channel!!.availableForPosition(start)) {
                return channel!!
            } else {
                val stream = runBlocking { fileStreamFactory(start, null) }
                channel = FileByteReadChannel(stream.channel, cache, start..stream.length)
            }

            return channel!!
        }
    }

    fun requestData(offset: Long, size: Int) {
        logcat(LogPriority.VERBOSE) {
            "requestData $logId: offset=$offset, size=$size"
        }
    }

    override fun close() {
        currentJob?.cancel(cause = CancelCauseCloseException())
        coroutineContext.cancelChildren()
    }

    inner class CancelCauseNewRangeException : CancellationException()
    inner class CancelCauseCloseException : CancellationException()
}

private enum class ReadType {
    NO_CACHE, PARTIAL_CACHE, ALL_CACHE
}

class FileByteReadChannel(
    private val channel: ByteReadChannel,
    private val cacheFile: CacheFile,
    private val channelRange: LongRange,
) : ByteReadChannel by channel {
    @Volatile
    private var currentPosition = channelRange.first

    /**
     * @param range range of the actual file
     * @param data [RandomAccess.read]
     * @param dataOffset offset in [data]
     */
    fun read(range: LongRange, data: ByteArray, dataOffset: Int): Int {
        val size = (range.last - range.first).toInt()
        val buffer = ByteArray(size)
        runBlocking { channel.readFully(buffer, 0, size) }

        cacheFile.write(range.first, buffer)
        buffer.copyInto(data, dataOffset, 0, buffer.size)

        return buffer.size
    }

    fun availableForPosition(pos: Long): Boolean {
        return pos in (currentPosition..channelRange.last)
    }
}

class CacheFile(
    file: File,
    innerList: MutableList<LongRange> = mutableListOf()
) : CacheChunks(
    innerList = innerList
) {
    private val fileRA = RandomAccessFile(file, "rw")

    fun LongRange.read(offset: Long, size: Int, data: ByteArray): Int {
        synchronized(fileRA) {
            fileRA.seek(first + offset)
            return fileRA.read(data, 0, size)
        }
    }

    fun write(offset: Long, data: ByteArray) {
        synchronized(fileRA) {
            fileRA.seek(offset)
            fileRA.write(data)
            add(offset..offset + data.size)
        }
    }
}