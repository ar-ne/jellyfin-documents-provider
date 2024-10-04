package arne.jellyfindocumentsprovider.ui.main

import android.content.Intent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import arne.jellyfindocumentsprovider.ServerWizardActivity
import arne.jellyfindocumentsprovider.common.LocalNavController
import arne.jellyfindocumentsprovider.common.LocalSnackbarHostState
import arne.jellyfindocumentsprovider.common.LocalSnackbarScope

private val selectedIcons = listOf(Icons.Filled.Home, Icons.Filled.Dashboard, Icons.Filled.Settings)
private val unselectedIcons =
    listOf(Icons.Outlined.Home, Icons.Outlined.Dashboard, Icons.Outlined.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun App() {
    var selectedItem by remember { mutableIntStateOf(0) }
    var slideDirection by remember { mutableIntStateOf(1) }
    val navController = rememberNavController()
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("Home")
                },
                actions = {
                    IconButton(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    context,
                                    ServerWizardActivity::class.java
                                )
                            )
                        },
                        content = { Icon(Icons.Filled.Add, contentDescription = "Add Server") }
                    )
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                NavigationBar {
                    listOf("Home", "Dashboard", "Settings").forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selectedItem == index) selectedIcons[index] else unselectedIcons[index],
                                    contentDescription = item
                                )
                            },
                            label = { Text(item) },
                            selected = selectedItem == index,
                            onClick = {
                                slideDirection = if (selectedItem < index) 1 else -1
                                selectedItem = index
                                navController.navigate(item.lowercase())
                            }
                        )
                    }
                }
            }
        }) { innerPadding ->

        AppContent(navController, innerPadding, slideDirection)
    }
}

@Composable
fun AppContent(navController: NavHostController, innerPadding: PaddingValues, slideDirection: Int) {
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }

    CompositionLocalProvider(
        LocalViewModelStoreOwner provides viewModelStoreOwner,
    ) {
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxHeight()
                .fillMaxWidth(),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { slideDirection * it },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -slideDirection * it },
                    animationSpec = tween(durationMillis = 300)
                )
            },
        ) {
            composable("home") { HomeScreen() }
            composable("dashboard") { DashboardScreen() }
            composable("settings") { SettingScreen() }
        }
    }
}

@Composable
fun Router() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    CompositionLocalProvider(
        LocalNavController provides navController,
        LocalSnackbarScope provides snackbarScope,
        LocalSnackbarHostState provides snackbarHostState
    ) {
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") { App() }
            composable(
                "server-setting/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) {
                it.arguments?.getLong("id")?.let { id ->
                    ServerSetting(id)
                } ?: Text(
                    "Server Settings/ID required",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

    }
}