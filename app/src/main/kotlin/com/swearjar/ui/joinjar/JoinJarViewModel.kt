package com.swearjar.ui.joinjar

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swearjar.R
import com.swearjar.data.auth.AuthRepository
import com.swearjar.data.jar.JarRepository
import com.swearjar.data.jar.JoinResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JoinJarState(
    val code: String = "",
    val joining: Boolean = false,
    @StringRes val error: Int? = null,
    /** Set once in the jar (newly or already); the screen navigates to it. */
    val jarId: String? = null,
)

@HiltViewModel
class JoinJarViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val jarRepository: JarRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(JoinJarState())
    val state: StateFlow<JoinJarState> = _state.asStateFlow()

    fun setCode(code: String) = _state.update { it.copy(code = code.uppercase().take(12), error = null) }

    /** A scanned QR code goes straight to joining. */
    fun onScanned(value: String?) {
        if (value == null) {
            _state.update { it.copy(error = R.string.join_scan_failed) }
        } else {
            setCode(value)
            submit()
        }
    }

    fun submit() {
        val current = _state.value
        val user = authRepository.currentUser() ?: return
        if (current.joining) return
        _state.update { it.copy(joining = true, error = null) }
        viewModelScope.launch {
            val result = try {
                jarRepository.joinJar(user, current.code)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("JoinJarViewModel", "Could not join jar", e)
                null
            }
            _state.update {
                when (result) {
                    is JoinResult.Joined -> it.copy(joining = false, jarId = result.jarId)
                    is JoinResult.AlreadyMember -> it.copy(joining = false, jarId = result.jarId)
                    JoinResult.InvalidCode -> it.copy(joining = false, error = R.string.join_invalid)
                    JoinResult.NotFound -> it.copy(joining = false, error = R.string.join_not_found)
                    JoinResult.Refused -> it.copy(joining = false, error = R.string.join_refused)
                    null -> it.copy(joining = false, error = R.string.something_went_wrong)
                }
            }
        }
    }
}
