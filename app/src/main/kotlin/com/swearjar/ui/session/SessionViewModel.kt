package com.swearjar.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swearjar.data.auth.AuthRepository
import com.swearjar.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SessionState {
    /** Firebase has not yet said whether anyone is signed in. */
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val user: UserProfile) : SessionState
}

/** Decides whether the app shows the sign-in screen or the signed-in screens. */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val state: StateFlow<SessionState> = authRepository.user
        .map { user -> if (user == null) SessionState.SignedOut else SessionState.SignedIn(user) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionState.Loading)

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
