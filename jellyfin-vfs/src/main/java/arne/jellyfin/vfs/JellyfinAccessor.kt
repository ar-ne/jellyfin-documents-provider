package arne.jellyfin.vfs

import android.content.Context
import arne.hacks.logcat
import logcat.LogPriority
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
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

class JellyfinAccessor(private val ctx: Context, val credential: JellyfinCredential) {
    private val api: ApiClient = createJellyfin(ctx).createApi(
        baseUrl = credential.url,
        accessToken = credential.token,
    )

    suspend fun userView() =
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
                    parentId = UUID.fromString(parentId)
                )
            ).content
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "Error querying Jellyfin API: ${e.stackTraceToString()}" }
            null
        }
    }


    data class ServerInfo(
        var url: String = "",
        var username: String = "",
        var password: String = ""
    ) {
        suspend fun login(
            ctx: Context
        ): JellyfinCredential {
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

                return JellyfinCredential(
                    url = url,
                    serverName = serverPublicInfo.serverName ?: "Unknown Server",
                    library = mapOf(),
                    uid = authResult.user!!.id.toString(),
                    token = authResult.accessToken!!,
                    username = authResult.user!!.name!!,
                )
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) {
                    "unable to login to server $url, error: ${e.stackTraceToString()}"
                }
            }
            throw IllegalArgumentException("unable to login to server $url")
        }
    }


}