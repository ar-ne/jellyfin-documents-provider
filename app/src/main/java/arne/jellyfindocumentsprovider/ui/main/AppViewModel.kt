package arne.jellyfindocumentsprovider.ui.main

import androidx.lifecycle.ViewModel
import arne.jellyfin.vfs.JellyfinServer
import arne.jellyfin.vfs.ObjectBox
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppViewModel : ViewModel() {
    private val _servers = MutableStateFlow<List<JellyfinServer>>(mutableListOf())
    val servers: StateFlow<List<JellyfinServer>>
        get() = _servers
    fun updateServerList() {
        _servers.value = ObjectBox.credential.all
    }

    fun deleteServer(credential: JellyfinServer) {
        ObjectBox.credential.remove(credential.id)
        updateServerList()
    }

}