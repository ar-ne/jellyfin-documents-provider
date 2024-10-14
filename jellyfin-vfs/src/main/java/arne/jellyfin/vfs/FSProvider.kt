package arne.jellyfin.vfs

import android.content.Context
import android.database.MatrixCursor
import android.graphics.Point
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.MediaStore.Audio.AudioColumns
import arne.jellyfin.vfs.DocId.DocType
import com.maxmpz.poweramp.player.TrackProviderConsts
import com.maxmpz.poweramp.player.TrackProviderHelper
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import logcat.logcat

object FSProvider {
    fun getRoots(): List<Map<String, Any>> {
        val servers = ObjectBox.server.all
        logcat {
            "FSProvider.getRoots(): amount of servers = $servers"
        }
        val roots = servers.map {
            val idInProvider = DocId(DocType.root, it.uuid).toTypedId()
            return@map mapOf(
                Root.COLUMN_ROOT_ID to idInProvider,
                Root.COLUMN_DOCUMENT_ID to idInProvider,
                Root.COLUMN_SUMMARY to it.username,
                Root.COLUMN_TITLE to it.serverName,

                // this provider only support "IS_CHILD" query.
                Root.COLUMN_FLAGS to Root.FLAG_SUPPORTS_IS_CHILD,

                // The child MIME types are used to filter the roots and only present to the user roots
                // that contain the desired type somewhere in their file hierarchy.
                Root.COLUMN_MIME_TYPES to setOf(Document.MIME_TYPE_DIR, Root.MIME_TYPE_ITEM, "*/*"),


                Root.COLUMN_AVAILABLE_BYTES to 0,
                Root.COLUMN_ICON to arne.jfdp.hacks.R.drawable.ic_launcher_foreground
            )
        }
        return roots
    }

    fun isChildDocument(parent: DocId, document: DocId): Boolean {
        return when (document.type) {
            DocType.lib -> if (parent.type == DocType.root) {
                ObjectBox.server.findByUUID(parent.id).library.containsKey(document.id)
            } else {
                false
            }

            DocType.file -> {
                if (parent.type == DocType.file) {
                    return false
                }
                if (parent.type == DocType.root) {
                    // if it checking from root directory
                    isChildDocument(parent, DocId(DocType.lib, document.id))
                } else {
                    val vf = ObjectBox.virtualFile.findByDocumentId(document.id)
                    vf.libId == parent.id
                }

            }

            else -> false
        }
    }

    fun getChildren(parent: DocId): List<Map<String, Any>> {
        logcat(LogPriority.INFO) {
            "FSProvider.queryChildren(parent = $parent)"
        }
        return with(ObjectBox) {
            when (parent.type) {
                DocType.root -> server.all.map { it.asProjection() }
                DocType.server -> server.findByUUID(parent.id).getLibrariesAsProjection()

                DocType.lib -> virtualFile.findAllByLibId(parent.id).map {
                    it.asProjection() + it.mediaInfo.target.asProjection()
                }

                else -> throw RuntimeException("no impl yet.")
            }
        }
    }

    fun getOne(id: DocId) = with(ObjectBox) {
        when (id.type) {
            DocType.root -> server.all.map { it.asProjection() }
            DocType.lib -> server.findByLibraryId(id.id).getLibrariesAsProjection()
            DocType.file -> listOf(virtualFile.findByDocumentId(id.id).asProjection())
            else -> TODO("Not yet implemented")
        }
    }

    fun Context.streamThumbnail(id: DocId, sizeHint: Point?): JellyfinAccessor.Stream? {
        return with(ObjectBox) {
            when (id.type) {
                DocType.file -> {
                    val vf = virtualFile.findByDocumentId(id.id)
                    val server = vf.server.target.asAccessor(this@streamThumbnail)
                    runBlocking { server.streamThumbnail(vf.documentId, sizeHint?.x, sizeHint?.y) }
                }

                else -> null
            }
        }
    }

    fun Context.getAudioStreamFactory(
        id: DocId,
        bps: Int?
    ): Triple<FileStreamFactory, VirtualFile, Int>? {
        return with(ObjectBox) {
            if (id.type == DocType.file) {
                val vf = virtualFile.findByDocumentId(id.id)
                val server = vf.server.target.asAccessor(this@getAudioStreamFactory)
                val fsf = runBlocking { server.getAudioFileStreamFactory(id) }
                return Triple(fsf, vf, bps ?: -1)
            } else null
        }
    }
}

fun MediaInfo.asProjection() = listOfNotNull(
    duration?.let { AudioColumns.DURATION to it },
    title?.let { AudioColumns.TITLE to it },
    album?.let { AudioColumns.ALBUM to it },
    track?.let { AudioColumns.TRACK to it },
    artist?.let { AudioColumns.ARTIST to it },
    bitrate?.let { AudioColumns.BITRATE to it },
    year?.let { AudioColumns.YEAR to it },
).toMap()

fun JellyfinServer.asProjection() = mapOf(
    Document.COLUMN_DOCUMENT_ID to uuid.toTypedId(DocType.server),
    Document.COLUMN_DISPLAY_NAME to serverName,
    Document.COLUMN_MIME_TYPE to Document.MIME_TYPE_DIR,
    Document.COLUMN_SIZE to 0,
    Document.COLUMN_LAST_MODIFIED to 0,
    Document.COLUMN_FLAGS to 0
)

fun VirtualFile.asProjection(): Map<String, Any> {
    val flag = if (mediaInfo.target.hasThumbnail)
        Document.FLAG_SUPPORTS_THUMBNAIL else 0
    return mapOf(
        Document.COLUMN_DOCUMENT_ID to documentId.toTypedId(DocType.file),
        Document.COLUMN_DISPLAY_NAME to name,
        Document.COLUMN_SIZE to size,
        Document.COLUMN_MIME_TYPE to mimeType,
        Document.COLUMN_LAST_MODIFIED to lastModified,
        Document.COLUMN_FLAGS to flag,
        Document.COLUMN_SIZE to size,
    )
}

fun JellyfinServer.getLibrariesAsProjection() = library.entries
    .map { (id, name) ->
        mapOf(
            Document.COLUMN_DOCUMENT_ID to id.toTypedId(DocType.lib),
            Document.COLUMN_DISPLAY_NAME to name,
            Document.COLUMN_MIME_TYPE to Document.MIME_TYPE_DIR,
            Document.COLUMN_SIZE to 0,
            Document.COLUMN_LAST_MODIFIED to 0,
            Document.COLUMN_FLAGS to 0
        )
    }

fun PowerampExtraInfo.asProjection(waveType: WaveType) = listOfNotNull(
    lyrics?.let { TrackProviderConsts.COLUMN_FLAGS to TrackProviderConsts.FLAG_HAS_LYRICS },
    lyrics?.let { TrackProviderConsts.COLUMN_TRACK_LYRICS_SYNCED to it },
    when (waveType) {
        WaveType.NONE -> TrackProviderConsts.COLUMN_TRACK_WAVE to byteArrayOf()
        WaveType.FAKE -> TrackProviderConsts.COLUMN_TRACK_WAVE to TrackProviderHelper.floatsToBytes(
            getFakeWave()
        )

        WaveType.REAL -> {}
    }
)

fun List<Map<String, Any>>.asAndroidMatrixCursor() = MatrixCursor(
    flatMap { it.keys }.toSet().toTypedArray()
).also { c -> forEach { c.addRow(it) } }
