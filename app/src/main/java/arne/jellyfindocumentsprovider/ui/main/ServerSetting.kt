package arne.jellyfindocumentsprovider.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import arne.jellyfin.vfs.JellyfinServer
import arne.jellyfin.vfs.ObjectBox
import arne.jellyfindocumentsprovider.common.LocalNavController
import arne.jellyfindocumentsprovider.common.Query.Companion.useQuery
import arne.jellyfindocumentsprovider.common.useLocalSnackbar
import arne.jellyfindocumentsprovider.ui.serverWizard.LibraryItem
import arne.jellyfindocumentsprovider.ui.serverWizard.ServerWizardViewModel.Library.Companion.toLibrary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun ServerSetting(id: Long = 0) {
    val credential by remember { mutableStateOf<JellyfinServer>(ObjectBox.credential.get(id)) }
    val selection = remember { mutableStateMapOf<String, Boolean>() }
    val context = LocalContext.current
    val globalNav = LocalNavController.current

    val query = useQuery(
        onLoad = { lib ->
            lib?.associate { it.id to credential.library.containsKey(it.id) }
                ?.let { selection.putAll(it) }
        }
    ) {
        credential.asAccessor(context).libraries()?.map { it.toLibrary() }
    }

    val coroutineScope = rememberCoroutineScope()
    val snackbar = useLocalSnackbar()

    with(query) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Server Settings - ${credential.serverName}", fontSize = 22.sp)
                            Text("${credential.username}@${credential.url}", fontSize = 16.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { globalNav.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            enabled = state.isSuccess,
                            onClick = {
                                coroutineScope.launch {
                                    ObjectBox.credential.put(credential.copy(
                                        library = if (!data.isNullOrEmpty()) {
                                            data.associate { it.id to it.name }.filter {
                                                selection[it.key] ?: false
                                            }
                                        } else credential.library
                                    ))
                                    globalNav.popBackStack()
                                    snackbar {
                                        showSnackbar("Saved", duration = SnackbarDuration.Long)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = "Save")
                        }
                    })
            }
        ) { innerPadding ->
            if (state.isLoading)
                LinearProgressIndicator(
                    Modifier
                        .padding(innerPadding)
                        .fillMaxWidth()
                )
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {

                if (state.isSuccess) {
                    if (data.isNullOrEmpty())
                        Text("No libraries found")
                    else {
                        data.map {
                            LibraryItem(it.apply {
                                checked = selection[it.id] ?: false
                            }, onClick = {
                                selection[it.id] = !selection[it.id]!!
                            })
                        }
                    }
                }

                if (state.isError)
                    Text("Error loading library")
            }
        }
    }
}