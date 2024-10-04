package arne.jellyfindocumentsprovider

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import arne.jellyfindocumentsprovider.ui.main.Router
import arne.jellyfindocumentsprovider.ui.theme.JellyfinDocumentsProviderTheme

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
}
