package arne.jellyfindocumentsprovider

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import arne.jellyfindocumentsprovider.ui.serverWizard.LibrarySelectionScreen
import arne.jellyfindocumentsprovider.ui.serverWizard.ServerInfoScreen
import arne.jellyfindocumentsprovider.ui.theme.JellyfinDocumentsProviderTheme
import kotlinx.serialization.Serializable

class ServerWizardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JellyfinDocumentsProviderTheme {
                Content()
            }
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content() {
        val navController = rememberNavController()
        var currentView by remember { mutableStateOf("server-info") }
        val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        }

        Scaffold(modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        when (currentView) {
                            "server-info" -> {
                                Text("Add Server - Server Info")
                            }

                            "library-selection" -> {
                                Text("Add Server - Library Selection")
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = ServerInfo,
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .fillMaxWidth(),
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it }
                    )
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it }
                    )
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it }
                    )
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it }
                    )
                }
            ) {
                composable<ServerInfo> {
                    CompositionLocalProvider(
                        LocalViewModelStoreOwner provides viewModelStoreOwner
                    ) {
                        ServerInfoScreen(next = {
                            navController.navigate(LibrarySelection)
                            currentView = "library-selection"
                        })
                    }
                }

                composable<LibrarySelection> {
                    CompositionLocalProvider(
                        LocalViewModelStoreOwner provides viewModelStoreOwner
                    ) {
                        LibrarySelectionScreen()
                    }
                }
            }
        }
    }

    @Serializable
    private object ServerInfo

    @Serializable
    private object LibrarySelection
}