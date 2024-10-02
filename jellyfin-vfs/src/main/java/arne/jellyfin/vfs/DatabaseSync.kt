package arne.jellyfin.vfs

import arne.hacks.logcat
import arne.jellyfin.vfs.ObjectBox.findAllByLibId
import arne.jellyfin.vfs.VirtualFile.Companion.toVirtualFile
import logcat.LogPriority
import org.jellyfin.sdk.model.api.BaseItemDto

class DatabaseSync(
    private val accessor: JellyfinAccessor,
) {
    suspend fun sync(
        batchSize: Int = 1000,
        onProgress: (Progress) -> Unit
    ) {
        val progress = Progress(0, -1, "", Progress.Step.PREPARE)
        progress.run(onProgress)
        val library = accessor.credential.library

        val libraryTotal = library.values.associateWith {
            accessor.queryAudioItems(it)?.totalRecordCount ?: 0
        }

        val total = libraryTotal.values.sum()
        var proceed = 0
        progress.also { it.total = total }.run(onProgress)

        libraryTotal.forEach { (libId, libTotal) ->
            val fetchIdSet = mutableSetOf<String>()
            fetchItemsInBatches(
                libId = libId,
                batchSize = batchSize,
                totalItems = libTotal,
                onFetch = { items ->
                    val virtualFiles = items.map { dto -> dto.toVirtualFile(accessor.credential) }
                    fetchIdSet.addAll(virtualFiles.map { it.documentId })

                    ObjectBox.virtualFile.put(virtualFiles)
                    proceed += items.size
                    progress.also { p ->
                        p.current = 100 * proceed / total
                        p.step = Progress.Step.FETCH
                    }.run(onProgress)
                }
            )

            progress.also {
                it.step = Progress.Step.CLEAN
            }
            val localIdSet = ObjectBox.virtualFile.findAllByLibId(libId)
            while (true) {
                val t = fetchIdSet.firstOrNull() ?: break
                fetchIdSet.remove(t)
                localIdSet.remove(t)
            }
            logcat(LogPriority.DEBUG) {
                "${localIdSet.size} to remove for $libId"
            }

            ObjectBox.virtualFile.remove(localIdSet.map { it.value })
        }
    }


    private suspend fun fetchItemsInBatches(
        batchSize: Int,
        totalItems: Int,
        libId: String,
        onFetch: (List<BaseItemDto>) -> Unit,
    ) {
        val numberOfBatches = (totalItems + batchSize - 1) / batchSize

        for (batch in 0 until numberOfBatches) {
            val startIndex = batch * batchSize
            val items = accessor.queryAudioItems(libId, startIndex, batchSize)?.items

            if (items != null) {
                onFetch(items)
            } else {
                break
            }
        }
    }

    data class Progress(
        var current: Int,
        var total: Int,
        var extra: String,
        var step: Step = Step.PREPARE
    ) {
        enum class Step {
            PREPARE,
            FETCH,
            CLEAN,
        }
    }

}