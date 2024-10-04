package arne.jellyfindocumentsprovider.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import arne.hacks.logcat
import arne.jellyfin.vfs.JellyfinServer
import arne.jellyfindocumentsprovider.common.useNavController
import kotlin.math.roundToInt

@Composable
fun ServerItem(credential: JellyfinServer) {
    val navController = useNavController()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm)
        AlertDialog(onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(onClick = {}) {
                    Text("Confirm")
                }
            }, text = { Text("Delete server?") })

    ServerItemInternal(credential, sync = {}, delete = {
        showDeleteConfirm = true
    }, onClick = {
        navController {
            navigate("server-setting/${credential.id}")
        }
    })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
@Preview
private fun ServerItemInternal(
    @PreviewParameter(ServerItemProvider::class) credential: JellyfinServer,
    sync: () -> Unit = {},
    delete: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    val density = LocalDensity.current
    val anchor = with(density) {
        DraggableAnchors {
            DragValue.Start at 0.dp.toPx()
            DragValue.End at -128.dp.toPx()
        }
    }
    val dragState = remember {
        AnchoredDraggableState(
            initialValue = DragValue.Start,
            anchors = anchor,
            positionalThreshold = { 0.3f * it },
            velocityThreshold = { with(density) { Int.MAX_VALUE.dp.toPx() } },
            snapAnimationSpec = tween(),
            decayAnimationSpec = exponentialDecay(),
        )
    }
    var height by remember { mutableStateOf(0.dp) }

    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .fillMaxHeight()
                    .background(
                        color = MaterialTheme.colorScheme.inversePrimary
                    )
                    .clickable {
                        logcat { "request for sync ${credential.url}" }
                        sync()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Sync, "Sync")
            }
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .fillMaxHeight()
                    .background(
                        color = Color(211, 47, 47, 255)
                    )
                    .clickable {
                        logcat { "request for delete ${credential.url}" }
                        delete()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Delete, "Delete")
            }

        }


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .offset {
                    IntOffset(
                        x = dragState
                            .requireOffset()
                            .roundToInt(), y = 0
                    )
                }
                .anchoredDraggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                )
                .background(
                    color = MaterialTheme.colorScheme.background
                )
                .onGloballyPositioned { coordinates ->
                    height = with(density) {
                        coordinates.size.height.toDp()
                    }
                }
                .clickable {
                    onClick()
                }
        ) {
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Row {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            "${credential.serverName} (${credential.username})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(credential.url)
                        Text("Library: ${credential.library.entries.size}")
                    }
                }
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

    }
}


class ServerItemProvider : PreviewParameterProvider<JellyfinServer> {
    override val values = sequenceOf(
        JellyfinServer(
            url = "https://jellyfin.example.com",
            serverName = "Jellyfin Server",
            library = mapOf("id" to "id", "name" to "name"),
            uid = "uid",
            username = "username",
            token = "token"
        )
    )
}


private enum class DragValue { Start, End }
