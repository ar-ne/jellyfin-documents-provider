package arne.jellyfin.vfs

import arne.jellyfin.vfs.VirtualFile.Companion.toVirtualFile
import io.objectbox.kotlin.query
import io.objectbox.query.QueryBuilder
import kotlinx.coroutines.runBlocking
import logcat.logcat
import org.jellyfin.sdk.model.api.BaseItemDto

class DatabaseSync(
    private val accessor: JellyfinAccessor,
) {
    suspend fun sync(
        batchSize: Int = 1000,
        onProgress: (Int) -> Unit
    ) {
        logcat {
            "[${accessor.credential.name}] syncing database for ${accessor.credential.info}"
        }
        onProgress(-1)
        val libraryTotal = accessor.credential.library.keys.associateWith {
            accessor.queryAudioItems(it, limit = 0)?.totalRecordCount ?: 0
        }

        logcat {
            "[${accessor.credential.name}] total library to sync: ${libraryTotal.size}"
        }
        val total = libraryTotal.values.sum()
        var proceed = 0

        libraryTotal.forEach { (libId, libTotal) ->
            logcat {
                "[${accessor.credential.name}] syncing library: $libId"
            }
            with(ObjectBox.virtualFile) {
                ObjectBox.store.runInTx {
                    query {
                        equal(VirtualFile_.libId, libId, QueryBuilder.StringOrder.CASE_SENSITIVE)
                    }.remove()
                    runBlocking {
                        fetchItemsInBatches(
                            libId = libId,
                            batchSize = batchSize,
                            totalItems = libTotal,
                            onFetch = { items ->
                                put(items.map { it.toVirtualFile(accessor.credential) })

                                proceed += items.size
                                onProgress((100 * proceed / total))
                                logcat {
                                    "[${accessor.credential.name}] syncing library: $libId ... $proceed/$total"
                                }
                            }
                        )
                    }
                }
            }
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
}