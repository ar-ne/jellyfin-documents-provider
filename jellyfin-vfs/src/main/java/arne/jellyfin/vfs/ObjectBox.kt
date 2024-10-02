package arne.jellyfin.vfs

import android.content.Context
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.query.QueryBuilder

object ObjectBox {
    private lateinit var store: BoxStore

    lateinit var credential: Box<JellyfinCredential>
        private set

    lateinit var virtualFile: Box<VirtualFile>

    fun init(context: Context) {
        store = MyObjectBox.builder()
            .androidContext(context.applicationContext)
            .build()
        credential = store.boxFor(JellyfinCredential::class.java)
        virtualFile = store.boxFor(VirtualFile::class.java)
    }

    fun Box<JellyfinCredential>.updateLibrarySel(
        userId: Long,
        lib: Map<String, String>
    ): JellyfinCredential {
        val credential = credential.get(userId)
        val copy = credential.copy(library = lib)
        ObjectBox.credential.put(copy)
        return copy
    }

    fun Box<VirtualFile>.findAllByLibId(libId: String): MutableMap<String, VirtualFile> {
        query().equal(VirtualFile_.libId, libId, QueryBuilder.StringOrder.CASE_SENSITIVE).build()
            .use {
                return it.find().associateBy { vf -> vf.documentId }.toMutableMap()
            }
    }
}