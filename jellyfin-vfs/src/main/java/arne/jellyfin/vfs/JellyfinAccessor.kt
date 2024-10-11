package arne.jellyfin.vfs

import android.content.Context
import io.ktor.utils.io.ByteReadChannel
import logcat.LogPriority
import logcat.logcat
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.Response
import org.jellyfin.sdk.api.client.extensions.audioApi
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.systemApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.AuthenticateUserByName
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest

class JellyfinAccessor(ctx: Context, val credential: JellyfinServer) {
    private val api: ApiClient = createJellyfin(ctx).createApi(
        baseUrl = credential.url,
        accessToken = credential.token,
    )

    /**
     * get all user libraries
     */
    suspend fun libraries() =
        api.userViewsApi.getUserViews().content.items

    companion object {
        @JvmStatic
        fun createJellyfin(ctx: Context) = createJellyfin {
            context = ctx
            clientInfo = ClientInfo("JellyfinDocumentsProvider", version = "in-dev")
        }
    }

    suspend fun queryAudioItems(
        parentId: String, startIndex: Int = 0, limit: Int = 100
    ): BaseItemDtoQueryResult? {
        return try {
            api.itemsApi.getItems(
                GetItemsRequest(
                    sortBy = listOf(ItemSortBy.SORT_NAME),
                    sortOrder = setOf(SortOrder.ASCENDING),
                    includeItemTypes = setOf(BaseItemKind.AUDIO),
                    recursive = true,
                    fields = setOf(
                        ItemFields.DATE_CREATED,
                        ItemFields.SORT_NAME,
                        ItemFields.MEDIA_STREAMS,
                        ItemFields.MEDIA_SOURCES,
                    ),
                    startIndex = startIndex,
                    imageTypeLimit = 1,
                    enableImageTypes = setOf(ImageType.PRIMARY),
                    limit = limit,
                    parentId = parentId.UUID()
                )
            ).content
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "Error querying Jellyfin API: ${e.stackTraceToString()}" }
            null
        }
    }

    suspend fun streamThumbnail(id: String, w: Int? = 250, h: Int? = 250) = api.imageApi.getItemImage(
        id.UUID(),
        ImageType.PRIMARY,
        fillWidth = w,
        fillHeight = h,
        quality = 96
    ).content


    suspend fun getStreamingAudioStreamFactory(
        id: DocId,
        bps: Int,
    ): FileStreamFactory =
        { start: Long, _: Long? ->
            api.audioApi.getAudioStream(
                id.UUID(),
                startTimeTicks = start,
                audioBitRate = bps
            ).toStream()
        }

    fun getAudioFileStreamFactory(id: DocId): FileStreamFactory =
        { _, _ ->
            api.libraryApi.getFile(id.UUID()).toStream()
        }


    data class ServerInfo(
        var url: String = "",
        var username: String = "",
        var password: String = ""
    ) {
        suspend fun login(
            ctx: Context
        ): JellyfinServer {
            if (url.isBlank())
                throw IllegalArgumentException("The baseUrl must not leave blank!")

            logcat {
                "try logging in to server: $this"
            }
            val api = createJellyfin(ctx).createApi(baseUrl = url)
            try {
                val serverPublicInfo by api.systemApi.getPublicSystemInfo()
                logcat {
                    "server info: $serverPublicInfo"
                }
                val authResult by api.userApi.authenticateUserByName(
                    AuthenticateUserByName(
                        username = username,
                        pw = password
                    )
                )
                logcat { "user info: ${authResult.user}" }

                return JellyfinServer(
                    url = url,
                    serverName = serverPublicInfo.serverName ?: "Unknown Server",
                    library = mapOf(),
                    token = authResult.accessToken!!,
                    username = authResult.user!!.name!!,
                    uuid = authResult.user!!.id.toString()
                )
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) {
                    "unable to login to server $url, error: ${e.stackTraceToString()}"
                }
            }
            throw IllegalArgumentException("unable to login to server $url")
        }
    }

    data class Stream(
        val channel: ByteReadChannel,
        val length: Long
    )

    private fun Response<ByteReadChannel>.toStream(): Stream {
        logcat(LogPriority.DEBUG) {
            "response status=${this.status}, headers: ${this.headers}"
        }
        val length = headers["content-length"]?.first()?.toLong() ?: -1
        return Stream(content, length)
    }
}

private fun DocId.UUID(): UUID = UUID.fromString(id)
private fun String.UUID(): UUID = UUID.fromString(this)
typealias FileStreamFactory = suspend (start: Long, end: Long?) -> JellyfinAccessor.Stream