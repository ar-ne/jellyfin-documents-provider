package arne.jellyfindocumentsprovider.ui.serverWizard

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import arne.hacks.logcat
import arne.jellyfin.vfs.JellyfinAccessor
import arne.jellyfin.vfs.JellyfinCredential
import arne.jellyfin.vfs.ObjectBox
import arne.jellyfindocumentsprovider.ServerWizardActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import logcat.LogPriority
import org.jellyfin.sdk.model.api.BaseItemDto

class ServerWizardViewModel(application: Application) : AndroidViewModel(application) {
    val url = MutableStateFlow("")
    val username = MutableStateFlow("")
    val password = MutableStateFlow("")

    private val _state = MutableStateFlow(State.INVALID_SERVER)
    val state: StateFlow<State>
        get() = _state

    private val _libraries = MutableStateFlow<MutableList<Library>>(mutableListOf())
    val libraries: StateFlow<List<Library>>
        get() = _libraries

    fun toggleLibraryChecked(id: String) {
        _libraries.value = _libraries.value.map {
            if (it.id == id) {
                it.copy(checked = !it.checked)
            } else {
                it
            }
        }.toMutableList()
    }

    private var credential: JellyfinCredential? = null

    fun markServerInvalid() {
        _state.value = State.INVALID_SERVER
    }

    suspend fun testServer() {
        _state.value = State.VALIDATING_SERVER
        withContext(Dispatchers.IO) {
            try {
                credential =
                    JellyfinAccessor.ServerInfo(url.value, username.value, password.value)
                        .login(getApplication())
                _state.value = State.VALID_SERVER
            } catch (e: Exception) {
                _state.value = State.INVALID_SERVER
            }
        }
    }

    suspend fun loadLibraries() {
        _state.value = State.LOADING_LIBRARY
        withContext(Dispatchers.IO) {
            try {
                val libraries = credential!!.asAccessor(getApplication()).userView()
                _libraries.value = (libraries?.map {
                    Library(it.name ?: "Unknown", it.id.toString(), dto = it)
                } ?: emptyList()).toMutableList()
                _state.value =
                    (if (_libraries.value.isEmpty()) State.EMPTY_LIBRARY else State.LOADED_LIBRARY)
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) {
                    "Error loading libraries: ${e.stackTraceToString()}"
                }
                _state.value = State.INVALID_LIBRARY
            }
        }
    }

    fun save(libraries: List<Library>, activityCtx: Context) {
        val cred = credential!!.copy(
            library = libraries.filter { it.checked }.associate { it.id to it.name }
        )
        ObjectBox.credential.put(cred)
        (activityCtx as? ServerWizardActivity)?.finish()
    }

    data class Library(
        val name: String,
        val id: String,
        var checked: Boolean = false,
        val dto: BaseItemDto? = null
    )

    enum class State {
        INVALID_SERVER,
        VALIDATING_SERVER,
        VALID_SERVER,
        LOADING_LIBRARY,
        EMPTY_LIBRARY,
        LOADED_LIBRARY,
        INVALID_LIBRARY
    }

}