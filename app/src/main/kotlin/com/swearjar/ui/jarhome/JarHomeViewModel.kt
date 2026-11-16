package com.swearjar.ui.jarhome

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swearjar.data.jar.JarRepository
import com.swearjar.domain.model.Jar
import com.swearjar.domain.model.Member
import com.swearjar.ui.nav.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface JarHomeState {
    data object Loading : JarHomeState

    /** The jar does not exist, or this user cannot see it. */
    data object Missing : JarHomeState
    data class Loaded(val jar: Jar, val members: List<Member>) : JarHomeState
}

@HiltViewModel
class JarHomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    jarRepository: JarRepository,
) : ViewModel() {

    private val jarId: String = checkNotNull(savedStateHandle[Routes.ARG_JAR_ID])

    val state: StateFlow<JarHomeState> =
        combine(jarRepository.observeJar(jarId), jarRepository.observeMembers(jarId)) { jar, members ->
            if (jar == null) JarHomeState.Missing else JarHomeState.Loaded(jar, members)
        }
            .catch { e ->
                Log.w("JarHomeViewModel", "Could not load jar $jarId", e)
                emit(JarHomeState.Missing)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JarHomeState.Loading)
}
