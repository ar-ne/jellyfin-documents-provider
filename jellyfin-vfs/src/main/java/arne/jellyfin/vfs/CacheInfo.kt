package arne.jellyfin.vfs

import arne.hacks.readable
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.Transient
import io.objectbox.relation.ToOne
import logcat.LogPriority
import logcat.logcat
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile

@Entity
data class CacheInfo(
    @Id var id: Long = 0,
    @Index val vfDocId: String,
    val localPath: String,
    val localLength: Long = 0,
    val bitrate: Int = -1,
    val isCompleted: Boolean = false,
    @Convert(converter = CacheChunksConverter::class, dbType = String::class)
    val chunks: CacheChunks = CacheChunks(),

    val virtualFileId: Long = 0,
) : Closeable {
    lateinit var virtualFile: ToOne<VirtualFile>
    val isComplete
        get() = isCompleted or chunks.noGapsIn(0 until virtualFile.target.size)

    @Transient
    private var _cacheFile = null as CacheFile?
    val cacheFile: CacheFile
        get() {
            if (_cacheFile == null) _cacheFile = CacheFile(File(localPath), inner = chunks)
            return _cacheFile!!
        }


    class CacheFile(
        file: File,
        inner: CacheChunks
    ) : CacheChunks(
        innerList = inner.toMutableList()
    ), Closeable {
        private val fileRA = RandomAccessFile(file, "rw")

        init {
            merge()
            logcat(LogPriority.VERBOSE) {
                "CacheFile: ${file.path} has ${this.size} chunks:" +
                        this.toList()
                            .joinToString(", ") { "${it.first.readable}..${it.last.readable}" }
            }
        }

        /**
         * read data from file at first .. last
         */
        @Synchronized
        fun read(offset: Long, size: Int, data: ByteArray): Int {
            fileRA.seek(offset)
            return fileRA.read(data, 0, size)
        }

        @Synchronized
        fun write(offset: Long, data: ByteArray) {
            fileRA.seek(offset)
            fileRA.write(data)
            add(offset..offset + data.size)
        }

        override fun close() {
            fileRA.close()
        }
    }

    override fun close() {
        _cacheFile?.close()
        ObjectBox.cacheInfo.put(this.copy(chunks = cacheFile))
    }
}
