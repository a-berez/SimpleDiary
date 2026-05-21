package com.example.simplediary.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.example.simplediary.ui.feed.FeedViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.simplediary.ui.navigation.SimpleDiaryDestination
import com.example.simplediary.ui.navigation.SimpleDiaryNavHost
import com.example.simplediary.ui.navigation.bottomNavDestinations

@Composable
fun SimpleDiaryApp(
    feedViewModel: FeedViewModel,
    pendingShortcutRoute: String? = null,
    onShortcutConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(pendingShortcutRoute) {
        val route = pendingShortcutRoute ?: return@LaunchedEffect
        navController.navigate(route) {
            launchSingleTop = true
        }
        onShortcutConsumed()
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavDestinations.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.destination.route,
                        onClick = {
                            navController.navigate(item.destination.route) {
                                popUpTo(SimpleDiaryDestination.Feed.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(imageVector = item.icon, contentDescription = item.destination.title) },
                        label = { Text(item.destination.title) },
                    )
                }
            }
        },
    ) { contentPadding ->
        SimpleDiaryNavHost(
            navController = navController,
            contentPadding = contentPadding,
            feedViewModel = feedViewModel,
        )
    }
}
