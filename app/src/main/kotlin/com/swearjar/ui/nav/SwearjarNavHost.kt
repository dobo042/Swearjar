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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.swearjar.ui.jarhome.JarHomeScreen
import com.swearjar.ui.session.SessionState
import com.swearjar.ui.session.SessionViewModel
import com.swearjar.ui.signin.SignInScreen

/** Route names. Jar list, ledger and settings are added as those screens are built. */
object Routes {
    const val JAR_HOME = "jarHome"
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
            NavHost(navController = navController, startDestination = Routes.JAR_HOME) {
                composable(Routes.JAR_HOME) {
                    JarHomeScreen(userName = current.user.displayName, onSignOut = session::signOut)
                }
            }
        }
    }
}
