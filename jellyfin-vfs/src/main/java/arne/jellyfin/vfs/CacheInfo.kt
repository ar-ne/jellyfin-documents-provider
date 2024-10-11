package arne.jellyfin.vfs

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.relation.ToOne

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
) {
    lateinit var virtualFile: ToOne<VirtualFile>
    val isComplete
        get() = isCompleted or chunks.noGapsIn(0 until virtualFile.target.size)
}
