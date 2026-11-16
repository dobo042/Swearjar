package com.swearjar.ui.jarlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swearjar.data.auth.AuthRepository
import com.swearjar.data.jar.JarRepository
import com.swearjar.domain.model.Jar
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface JarListState {
    data object Loading : JarListState
    data object Failed : JarListState
    data class Loaded(val jars: List<Jar>) : JarListState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class JarListViewModel @Inject constructor(
    authRepository: AuthRepository,
    jarRepository: JarRepository,
) : ViewModel() {

    val state: StateFlow<JarListState> = authRepository.user
        .flatMapLatest { user ->
            if (user == null) {
                flowOf<JarListState>(JarListState.Loaded(emptyList()))
            } else {
                jarRepository.observeMyJars(user.uid)
                    .map<List<Jar>, JarListState> { JarListState.Loaded(it) }
                    .catch { e ->
                        Log.w("JarListViewModel", "Could not load jars", e)
                        emit(JarListState.Failed)
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JarListState.Loading)
}
