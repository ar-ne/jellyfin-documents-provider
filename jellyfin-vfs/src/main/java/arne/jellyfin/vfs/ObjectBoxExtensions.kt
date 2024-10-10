package arne.jellyfin.vfs

import io.objectbox.Box
import io.objectbox.kotlin.query
import io.objectbox.query.QueryBuilder.StringOrder

fun Box<VirtualFile>.findAllByLibId(libId: String): List<VirtualFile> = query {
    equal(VirtualFile_.libId, libId, StringOrder.CASE_SENSITIVE)
}.find()

fun Box<VirtualFile>.countByServer(server: Long) = query {
    equal(VirtualFile_.serverId, server)
}.count()

fun Box<JellyfinServer>.findByUUID(uuid: String) = query {
    equal(JellyfinServer_.uuid, uuid, StringOrder.CASE_SENSITIVE)
}.findFirst() ?: throw RuntimeException("Server with UUID $uuid not found")

fun Box<JellyfinServer>.findByLibraryId(id: String) =
    all.find { it.library.containsKey(id) }
        ?: throw RuntimeException("Server with libraryId $id not found")

fun Box<VirtualFile>.findByDocumentId(documentId: String) = query {
    equal(VirtualFile_.documentId, documentId, StringOrder.CASE_SENSITIVE)
}.findFirst() ?: throw RuntimeException("File with documentId=$documentId not found")

fun Box<CacheInfo>.getOrCreate(vf: VirtualFile, path: String): CacheInfo {
    return query {
        equal(CacheInfo_.vfDocId, vf.documentId, StringOrder.CASE_SENSITIVE)
    }.findFirst() ?: CacheInfo(
        virtualFileId = vf.id, vfDocId = vf.documentId, localPath = path
    ).apply {
        put(this)
    }
}