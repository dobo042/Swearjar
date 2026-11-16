package com.swearjar.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.swearjar.ui.createjar.CreateJarScreen
import com.swearjar.ui.jarhome.JarHomeScreen
import com.swearjar.ui.jarlist.JarListScreen
import com.swearjar.ui.joinjar.JoinJarScreen
import com.swearjar.ui.session.SessionState
import com.swearjar.ui.session.SessionViewModel
import com.swearjar.ui.signin.SignInScreen

/** Route names. Ledger and jar settings are added as those screens are built. */
object Routes {
    const val JARS = "jars"
    const val CREATE_JAR = "jars/create"
    const val JOIN_JAR = "jars/join"
    const val ARG_JAR_ID = "jarId"
    const val JAR = "jar/{$ARG_JAR_ID}"

    fun jar(jarId: String) = "jar/$jarId"
}

/** The whole app: sign-in when signed out, otherwise the signed-in navigation graph. */
@Composable
fun SwearjarNavHost(session: SessionViewModel = hiltViewModel()) {
    val state by session.state.collectAsStateWithLifecycle()
    when (val current = state) {
        SessionState.Loading -> Surface(Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        SessionState.SignedOut -> SignInScreen()
        is SessionState.SignedIn -> {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = Routes.JARS) {
                composable(Routes.JARS) {
                    JarListScreen(
                        userName = current.user.displayName,
                        onSignOut = session::signOut,
                        onCreate = { navController.navigate(Routes.CREATE_JAR) },
                        onJoin = { navController.navigate(Routes.JOIN_JAR) },
                        onOpen = { navController.navigate(Routes.jar(it)) },
                    )
                }
                composable(Routes.CREATE_JAR) {
                    CreateJarScreen(onBack = navController::popBackStack, onCreated = navController::openNewJar)
                }
                composable(Routes.JOIN_JAR) {
                    JoinJarScreen(onBack = navController::popBackStack, onJoined = navController::openNewJar)
                }
                composable(Routes.JAR) {
                    JarHomeScreen(onBack = navController::popBackStack)
                }
            }
        }
    }
}

/** Opens a jar just created or joined, leaving the jar list (not the form) behind it. */
private fun NavHostController.openNewJar(jarId: String) {
    navigate(Routes.jar(jarId)) { popUpTo(Routes.JARS) }
}
