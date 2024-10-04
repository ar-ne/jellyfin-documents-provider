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
    @Index val name: String,
    @Index val documentId: String,

    // attributes
    val mimeType: String,
    val displayName: String,
    val lastModified: Long,
    val size: Long,

    // links
    @Index val libId: String,
    val credentialId: Long = 0,
    val mediaInfoId: Long = 0,
) {
    lateinit var credential: ToOne<JellyfinServer>
    lateinit var mediaInfo: ToOne<MediaInfo>

    companion object {
        fun BaseItemDto.toVirtualFile(credential: JellyfinServer): VirtualFile {
            val mediaSource = mediaSources?.first()!!
            return VirtualFile(
                name = name!!,
                documentId = id.toString(),
                mimeType = getMimeTypeFromExtension(mediaSource.container!!)!!,
                displayName = name!!,
                lastModified = 1000 * dateCreated?.toEpochSecond(ZoneOffset.UTC)!!,
                size = mediaSource.size ?: 0,
                libId = credential.library.keys.first()
            ).also {
                it.credential.target = credential
                it.mediaInfo.target = toMediaInfo()
            }
        }

        private val mimeTypeCache = HashMap<String, String>()
        private fun getMimeTypeFromExtension(extension: String): String? {
            return mimeTypeCache.getOrPut(extension) {
                // Get the MIME type for the extension using the MimeTypeMap class
                val mimeTypeMap = MimeTypeMap.getSingleton()
                return mimeTypeMap.getMimeTypeFromExtension(extension)
            }
        }
    }
}
