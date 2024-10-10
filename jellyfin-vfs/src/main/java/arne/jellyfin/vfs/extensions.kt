package arne.jellyfin.vfs

import android.database.MatrixCursor

fun MatrixCursor.addRow(row: Map<String, Any>): MatrixCursor.RowBuilder? {
    val newRow = newRow()
    row.forEach { (key, value) -> newRow.add(key, value) }
    return newRow
}