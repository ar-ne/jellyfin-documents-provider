package arne.jellyfin.vfs

import android.database.MatrixCursor
import android.provider.MediaStore.Audio.AudioColumns
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import org.jellyfin.sdk.model.api.BaseItemDto

@Entity
data class MediaInfo(
    @Id var id: Long = 0,
    val duration: Long?,
    val year: Int?,
    val title: String?,
    val album: String?,
    val track: Int?,
    val artist: String?,
    val bitrate: Int?,
    val albumId: String?,
    val albumCoverTag: String?,
) {
    val hasThumbnail
        get() = albumCoverTag != null

    fun appendTo(row: MatrixCursor.RowBuilder) {
        if (year != -1) row.add(AudioColumns.YEAR, year)
        row.add(AudioColumns.DURATION, duration)
        row.add(AudioColumns.TITLE, title)
        row.add(AudioColumns.ALBUM, album)
        row.add(AudioColumns.TRACK, track)
        row.add(AudioColumns.ARTIST, artist)
        row.add(AudioColumns.BITRATE, bitrate)
    }

    companion object {
        fun BaseItemDto.toMediaInfo(): MediaInfo {
            return MediaInfo(
                duration = (runTimeTicks ?: 0) / 10000,
                year = productionYear ?: -1,
                title = name,
                album = album,
                track = indexNumber ?: 0,
                artist = artists?.joinToString(", ") ?: "",
                bitrate = mediaSources?.firstOrNull()?.bitrate ?: 0,
                albumId = albumId.toString(),
                albumCoverTag = albumPrimaryImageTag
            )
        }
    }
}
