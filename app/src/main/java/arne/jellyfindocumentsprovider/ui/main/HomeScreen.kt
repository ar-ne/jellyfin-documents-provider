package arne.jellyfindocumentsprovider.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import arne.jellyfindocumentsprovider.MainActivity
import arne.jellyfindocumentsprovider.ui.components.ServerItem

@Composable
fun HomeScreen(
    appViewModel: AppViewModel = viewModel(),
    globalNav: NavHostController? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    val servers by appViewModel.servers.collectAsState()

    LaunchedEffect(lifecycleState) {
        when (lifecycleState) {
            Lifecycle.State.RESUMED -> {
                appViewModel.updateServerList()
            }

            else -> {}
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        servers.map {
            ServerItem(
                credential = it,
                sync = {

                },
                delete = {
                    appViewModel.deleteServer(it)
                },
                onClick = {
                    globalNav?.navigate(MainActivity.ServerSettings)
                }
            )
        }
        if (servers.isEmpty()) Text("No servers found")
    }
}
