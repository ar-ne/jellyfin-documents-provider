package arne.jellyfindocumentsprovider.ui.main

import androidx.lifecycle.ViewModel
import arne.jellyfin.vfs.JellyfinCredential
import arne.jellyfin.vfs.ObjectBox
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppViewModel : ViewModel() {
    private val _servers = MutableStateFlow<List<JellyfinCredential>>(mutableListOf())
    val servers: StateFlow<List<JellyfinCredential>>
        get() = _servers
    fun updateServerList() {
        _servers.value = ObjectBox.credential.all
    }

    fun deleteServer(credential: JellyfinCredential) {
        ObjectBox.credential.remove(credential.id)
        updateServerList()
    }

}