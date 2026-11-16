package com.swearjar.ui.createjar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swearjar.data.auth.AuthRepository
import com.swearjar.data.jar.JarRepository
import com.swearjar.domain.model.GoalBehaviour
import com.swearjar.domain.model.JarDraft
import com.swearjar.domain.model.JarSettings
import com.swearjar.domain.model.LoggingPolicy
import com.swearjar.domain.model.PricingMode
import com.swearjar.domain.rules.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateJarState(
    val name: String = "",
    val currencies: List<String>,
    val currency: String,
    /** As typed, in major units. Blank means no goal. */
    val goalText: String = "",
    val settings: JarSettings = JarSettings(),
    /** Problems are only shown once the person has tried to submit. */
    val submitted: Boolean = false,
    val saving: Boolean = false,
    val failed: Boolean = false,
    /** Set when the jar has been created; the screen navigates to it. */
    val createdJarId: String? = null,
) {
    /** Null when the goal text is not a usable amount. */
    val goalMinor: Long?
        get() = if (goalText.isBlank()) 0 else Money.parse(goalText, currency)

    val draft: JarDraft? get() = goalMinor?.let { JarDraft(name, currency, it, settings) }

    val problems: Set<JarDraft.Problem> get() = JarDraft(name, currency, goalMinor ?: 0, settings).problems()

    val canSave: Boolean get() = goalMinor != null && problems.isEmpty()
}

@HiltViewModel
class CreateJarViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val jarRepository: JarRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<CreateJarState> = _state.asStateFlow()

    fun setName(name: String) = _state.update { it.copy(name = name.take(JarDraft.MAX_NAME_LENGTH + 20)) }
    fun setCurrency(currency: String) = _state.update { it.copy(currency = currency) }
    fun setGoal(text: String) = _state.update { it.copy(goalText = text.take(12)) }
    fun setLoggingPolicy(value: LoggingPolicy) = _state.update { it.copy(settings = it.settings.copy(loggingPolicy = value)) }
    fun setPricingMode(value: PricingMode) = _state.update { it.copy(settings = it.settings.copy(pricingMode = value)) }
    fun setGoalBehaviour(value: GoalBehaviour) = _state.update { it.copy(settings = it.settings.copy(goalBehaviour = value)) }

    fun submit() {
        val current = _state.value
        if (current.saving) return
        val draft = current.draft
        val user = authRepository.currentUser()
        if (draft == null || !current.canSave || user == null) {
            _state.update { it.copy(submitted = true) }
            return
        }
        _state.update { it.copy(submitted = true, saving = true, failed = false) }
        viewModelScope.launch {
            try {
                val id = jarRepository.createJar(user, draft)
                _state.update { it.copy(saving = false, createdJarId = id) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("CreateJarViewModel", "Could not create jar", e)
                _state.update { it.copy(saving = false, failed = true) }
            }
        }
    }

    private fun initialState(): CreateJarState {
        // Offer the phone's own currency first, then the common ones.
        val local = runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull()
        val currencies = (listOfNotNull(local) + listOf("GBP", "EUR", "USD")).distinct()
        return CreateJarState(currencies = currencies, currency = currencies.first())
    }
}
