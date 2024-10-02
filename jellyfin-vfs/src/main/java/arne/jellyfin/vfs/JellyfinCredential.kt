package arne.jellyfin.vfs

import android.content.Context
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class JellyfinCredential(
    @Id var id: Long = 0,
    val url: String,
    val serverName: String,
    val library: Map<String, String>,

    val uid: String,
    val username: String,
    val token: String,
) {
    fun asAccessor(ctx: Context) = JellyfinAccessor(ctx, this)
}


