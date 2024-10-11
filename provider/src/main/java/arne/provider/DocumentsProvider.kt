package arne.provider

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.os.CancellationSignal
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.os.StrictMode
import android.os.storage.StorageManager
import android.preference.PreferenceManager
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import arne.hacks.short
import arne.jellyfin.vfs.BitrateLimitType
import arne.jellyfin.vfs.BitrateLimits
import arne.jellyfin.vfs.FSProvider
import arne.jellyfin.vfs.FSProvider.getAudioStreamFactory
import arne.jellyfin.vfs.FSProvider.streamThumbnail
import arne.jellyfin.vfs.ObjectBox
import arne.jellyfin.vfs.PrefKeys
import arne.jellyfin.vfs.WaveType
import arne.jellyfin.vfs.asAndroidMatrixCursor
import arne.jellyfin.vfs.getEnum
import arne.jellyfin.vfs.toDocId
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat
import java.io.FileNotFoundException


class DocumentsProvider() : android.provider.DocumentsProvider() {
    private val providerContext: Context by lazy { context!! }
    private val storageManager: StorageManager by lazy { providerContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager }
    private val preference: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(
            providerContext
        )
    }

    private val waveType
        get() = preference.getEnum<WaveType>(PrefKeys.WAVE_TYPE)
    private val bitrateLimits
        get() = preference.getEnum<BitrateLimits>(PrefKeys.BITRATE_LIMIT)
    private val bitrateLimitType
        get() = preference.getEnum<BitrateLimitType>(PrefKeys.BITRATE_LIMIT_TYPE)

    override fun onCreate(): Boolean {
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects().detectLeakedClosableObjects()
                .penaltyLog().build()
        )
        return true
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        logcat { "queryRoots(): projection = $projection" }
        return if (ObjectBox.server.all.isEmpty()) MatrixCursor(
            projection ?: arrayOf(
                Root.COLUMN_ROOT_ID,
                Root.COLUMN_MIME_TYPES,
                Root.COLUMN_FLAGS,
                Root.COLUMN_ICON,
                Root.COLUMN_TITLE,
                Root.COLUMN_SUMMARY,
                Root.COLUMN_DOCUMENT_ID,
                Root.COLUMN_AVAILABLE_BYTES
            )
        )
        else FSProvider.getRoots().asAndroidMatrixCursor()
    }


    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor {
        logcat { "queryDocument: id=$documentId, projection=$projection" }
        return FSProvider.getOne(documentId!!.toDocId()).asAndroidMatrixCursor()
    }

    override fun queryChildDocuments(
        parentDocumentId: String?, projection: Array<out String>?, sortOrder: String?
    ): Cursor {
        logcat(LogPriority.INFO) { "queryChildDocuments: parent=$parentDocumentId, projection=$projection, sort=$sortOrder" }
        return if (parentDocumentId.isNullOrBlank()) {
            logcat(LogPriority.WARN) {
                "queryChildDocuments: parent id is null or blank"
            }
            MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        } else {
            FSProvider.getChildren(parentDocumentId.toDocId()).asAndroidMatrixCursor()
        }
    }

    override fun isChildDocument(parentDocumentId: String?, documentId: String?): Boolean {
        logcat { "isChildDocument(): parentDocumentId = $parentDocumentId, documentId = $documentId" }
        val parent = parentDocumentId?.toDocId()
        val document = documentId?.toDocId()
        if (parent == null || document == null) return false
        return FSProvider.isChildDocument(parent, document)
    }

    override fun openDocumentThumbnail(
        documentId: String,
        sizeHint: Point?,
        signal: CancellationSignal?
    ): AssetFileDescriptor? {
        logcat { "openDocumentThumbnail(${documentId.short}): sizeHint = $sizeHint" }
        return providerContext.streamThumbnail(documentId.toDocId(), sizeHint)?.let { stream ->
            val total = stream.length
            val (read, write) = ParcelFileDescriptor.createPipe()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ParcelFileDescriptor.AutoCloseOutputStream(write).use { output ->
                        stream.channel.copyTo(output)
                    }
                } catch (e: Exception) {
                    // Handle any exceptions that occur while downloading the thumbnail
                    logcat(LogPriority.ERROR) { "openDocumentThumbnail: failed to get thumbnail file=${documentId.short} \n${e.stackTraceToString()}" }
                }
            }
            AssetFileDescriptor(read, 0, total)
        }
    }

    @Throws(FileNotFoundException::class)
    override fun openDocument(
        documentId: String, mode: String?, signal: CancellationSignal?
    ): ParcelFileDescriptor? {
        logcat { "openDocument(): documentId = $documentId, mode = $mode" }
        return providerContext.getAudioStreamFactory(
            documentId.toDocId(), when (bitrateLimitType) {
                BitrateLimitType.NONE -> null
                BitrateLimitType.CELL,
                BitrateLimitType.ALL -> bitrateLimits.bps
            }
        )?.let { (fsf, vf, bps) ->
            RandomAccessBucket.proxy(fsf, vf, bps).let { proxy ->
                storageManager.openProxyFileDescriptor(
                    ParcelFileDescriptor.parseMode(mode),
                    proxy,
                    Handler(HandlerThread("fdProxyHandler-${documentId.short}").apply { start() }.looper)
                )
            }
        }
    }

    private fun addVirtualDirRow(
        cursor: MatrixCursor, id: String, name: String
    ) {
        val row = cursor.newRow()
        row.add(Document.COLUMN_DOCUMENT_ID, id)
        row.add(Document.COLUMN_DISPLAY_NAME, name)
        row.add(Document.COLUMN_SIZE, 0)
        row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR)
        row.add(Document.COLUMN_LAST_MODIFIED, 0)
        row.add(Document.COLUMN_FLAGS, 0)
    }


    private fun extractUniqueId(documentId: String) = documentId.toDocId().id


    private fun getDocTypeByDocId(documentId: String) = documentId.toDocId().type

    companion object {
        private val DEFAULT_DOCUMENT_PROJECTION: Array<String> = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
        )
    }

}