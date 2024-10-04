package arne.jellyfindocumentsprovider.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

val LocalNavController = compositionLocalOf<NavHostController> {
    error("No LocalNavController provided")
}

@Composable
fun useNavController(): (suspend NavHostController.() -> Unit) -> Unit {
    val nav = LocalNavController.current
    val scope = rememberCoroutineScope()
    return remember { { block -> scope.launch { block(nav) } } }
}