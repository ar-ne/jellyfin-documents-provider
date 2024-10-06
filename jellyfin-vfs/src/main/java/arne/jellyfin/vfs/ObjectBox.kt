package arne.jellyfin.vfs

import android.content.Context
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.kotlin.query
import io.objectbox.query.QueryBuilder
import kotlinx.coroutines.runBlocking

object ObjectBox {
    private lateinit var store: BoxStore

    lateinit var server: Box<JellyfinServer>
        private set

    lateinit var virtualFile: Box<VirtualFile>

    fun init(context: Context) {
        store = MyObjectBox.builder()
            .androidContext(context.applicationContext)
            .build()
        server = store.boxFor(JellyfinServer::class.java)
        virtualFile = store.boxFor(VirtualFile::class.java)
    }

    fun Box<VirtualFile>.update(libId: String, adder: suspend (Box<VirtualFile>) -> Unit) {
        store.runInTx {
            query {
                equal(VirtualFile_.libId, libId, QueryBuilder.StringOrder.CASE_SENSITIVE)
            }.remove()
            runBlocking { adder(this@update) }
        }
    }

    fun Box<VirtualFile>.findAllByLibId(libId: String): MutableMap<String, VirtualFile> {
        query().equal(VirtualFile_.libId, libId, QueryBuilder.StringOrder.CASE_SENSITIVE).build()
            .use {
                return it.find().associateBy { vf -> vf.documentId }.toMutableMap()
            }
    }

    fun Box<VirtualFile>.countByServer(server: Long) =
        query().equal(VirtualFile_.serverId, server).build().count()
}