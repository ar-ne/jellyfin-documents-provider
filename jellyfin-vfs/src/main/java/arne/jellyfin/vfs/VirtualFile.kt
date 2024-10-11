package arne.jellyfin.vfs

import android.webkit.MimeTypeMap
import arne.jellyfin.vfs.MediaInfo.Companion.toMediaInfo
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.relation.ToOne
import org.jellyfin.sdk.model.api.BaseItemDto
import java.time.ZoneOffset

@Entity
data class VirtualFile(
    @Id var id: Long = 0,
    val name: String,
    @Index val documentId: String,

    // attributes
    val mimeType: String,
    val displayName: String,
    val lastModified: Long,
    val size: Long,

    // links
    @Index val libId: String,
    @Index val serverId: Long = 0,
    val mediaInfoId: Long = 0,
    val powerampExtId: Long = 0
) {
    lateinit var server: ToOne<JellyfinServer>
    lateinit var mediaInfo: ToOne<MediaInfo>
    val thumbnailQueryId
        get() = mediaInfo.target?.albumId

    companion object {
        fun BaseItemDto.toVirtualFile(credential: JellyfinServer): VirtualFile {
            val mediaSource = mediaSources?.first()!!
            return VirtualFile(
                name = name!!,
                documentId = id.toString(),
                mimeType = mediaSource.container.toMIMEType(),
                displayName = name!!,
                lastModified = 1000 * dateCreated?.toEpochSecond(ZoneOffset.UTC)!!,
                size = mediaSource.size ?: 0,
                libId = credential.library.keys.first()
            ).also {
                it.server.target = credential
                it.mediaInfo.target = toMediaInfo()
            }
        }

        private val mimeTypeCache = HashMap<String, String>()

        private fun String?.toMIMEType(): String {
            if (this == null) return "application/octet-stream"
            return mimeTypeCache.getOrPut(this) {
                // Get the MIME type for the extension using the MimeTypeMap class
                return@getOrPut MimeTypeMap.getSingleton().getMimeTypeFromExtension(this)
                    ?: "application/octet-stream"
            }
        }
    }
}
