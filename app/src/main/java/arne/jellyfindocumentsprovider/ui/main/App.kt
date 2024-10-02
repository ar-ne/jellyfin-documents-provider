package arne.jellyfindocumentsprovider.ui.main

import android.content.Intent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import arne.jellyfindocumentsprovider.ServerWizardActivity

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun App(globalNav: NavHostController? = null) {
    val selectedIcons = listOf(Icons.Filled.Home, Icons.Filled.Dashboard, Icons.Filled.Settings)
    val unselectedIcons =
        listOf(Icons.Outlined.Home, Icons.Outlined.Dashboard, Icons.Outlined.Settings)
    var selectedItem by remember { mutableIntStateOf(0) }
    var slideDirection by remember { mutableIntStateOf(1) }
    val navController = rememberNavController()
    val context = LocalContext.current
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
                    Button(
                        onClick = {
                            val intent = Intent(context, ServerWizardActivity::class.java)
                            context.startActivity(intent)
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
            composable("home") {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides viewModelStoreOwner
                ) {
                    HomeScreen(globalNav = globalNav, appViewModel = viewModel())
                }
            }
            composable("dashboard") {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides viewModelStoreOwner
                ) {
                    DashboardScreen()
                }
            }
            composable("settings") {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides viewModelStoreOwner
                ) {
                    SettingScreen()
                }
            }
        }
    }
}