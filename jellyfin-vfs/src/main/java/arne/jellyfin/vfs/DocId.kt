package arne.jellyfin.vfs

import arne.jellyfin.vfs.DocId.DocType

data class DocId(
    val type: DocType,
    val id: String
) {
    override fun toString() = "${type.name}=$id"
    fun toTypedId() = "${type.name}=${id}"

    @Suppress("EnumEntryName")
    enum class DocType {
        root, lib, album, file;
    }
}

fun String.toTypedId(type: DocId.DocType) = DocId(type, this)
fun String.toDocId(): DocId {
    val (type, value) = split("=")
    return DocId(
        DocType.valueOf(type), value
    )
}