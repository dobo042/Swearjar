package com.swearjar.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.swearjar.ui.jarhome.JarHomeScreen

/** Route names. Sign-in, jar list, ledger and settings are added as those screens are built. */
object Routes {
    const val JAR_HOME = "jarHome"
}

@Composable
fun SwearjarNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.JAR_HOME) {
        composable(Routes.JAR_HOME) { JarHomeScreen() }
    }
}
