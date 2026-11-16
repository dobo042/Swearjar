package com.swearjar.ui.signin

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swearjar.R
import com.swearjar.data.auth.AuthRepository
import com.swearjar.data.auth.SignInResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SignInState(
    val inProgress: Boolean = false,
    @StringRes val error: Int? = null,
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()

    /** @param activityContext passed through, never stored: the Google sheet needs an Activity. */
    fun signIn(activityContext: Context) {
        if (_state.value.inProgress) return
        _state.value = SignInState(inProgress = true)
        viewModelScope.launch {
            // On success the session state flips and this screen is replaced.
            val error = when (authRepository.signInWithGoogle(activityContext)) {
                SignInResult.Success, SignInResult.Cancelled -> null
                SignInResult.NoAccount -> R.string.sign_in_no_account
                SignInResult.Failed -> R.string.sign_in_failed
            }
            _state.value = SignInState(error = error)
        }
    }
}
