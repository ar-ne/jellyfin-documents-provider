package arne.jellyfindocumentsprovider

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import arne.jellyfindocumentsprovider.ui.main.App
import arne.jellyfindocumentsprovider.ui.theme.JellyfinDocumentsProviderTheme
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JellyfinDocumentsProviderTheme {
                Router()
            }
        }
    }

    @Composable
    fun Router() {

        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = Home
        ) {
            composable<Home> { App(navController) }
            composable<ServerSettings> {
                Text("Server Settings")
            }
        }
    }

    @Serializable
    object Home
    @Serializable
    object ServerSettings
}

