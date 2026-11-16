@file:OptIn(ExperimentalMaterial3Api::class)

package com.swearjar.ui.createjar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swearjar.R
import com.swearjar.domain.model.GoalBehaviour
import com.swearjar.domain.model.JarDraft.Problem
import com.swearjar.domain.model.LoggingPolicy
import com.swearjar.domain.model.PricingMode
import com.swearjar.ui.common.OptionText
import com.swearjar.ui.common.text
import com.swearjar.ui.theme.SwearjarTheme

@Composable
fun CreateJarScreen(
    onBack: () -> Unit,
    onCreated: (jarId: String) -> Unit,
    viewModel: CreateJarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.createdJarId) { state.createdJarId?.let(onCreated) }
    CreateJarContent(
        state = state,
        onBack = onBack,
        onName = viewModel::setName,
        onCurrency = viewModel::setCurrency,
        onGoal = viewModel::setGoal,
        onLogging = viewModel::setLoggingPolicy,
        onPricing = viewModel::setPricingMode,
        onGoalBehaviour = viewModel::setGoalBehaviour,
        onSubmit = viewModel::submit,
    )
}

@Composable
private fun CreateJarContent(
    state: CreateJarState,
    onBack: () -> Unit,
    onName: (String) -> Unit,
    onCurrency: (String) -> Unit,
    onGoal: (String) -> Unit,
    onLogging: (LoggingPolicy) -> Unit,
    onPricing: (PricingMode) -> Unit,
    onGoalBehaviour: (GoalBehaviour) -> Unit,
    onSubmit: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_title)) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val nameError = when {
                !state.submitted -> null
                Problem.NAME_BLANK in state.problems -> R.string.create_name_blank
                Problem.NAME_TOO_LONG in state.problems -> R.string.create_name_long
                else -> null
            }
            OutlinedTextField(
                value = state.name,
                onValueChange = onName,
                label = { Text(stringResource(R.string.create_name)) },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(stringResource(it)) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle(R.string.create_currency)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (currency in state.currencies) {
                    FilterChip(
                        selected = currency == state.currency,
                        onClick = { onCurrency(currency) },
                        label = { Text(currency) },
                    )
                }
            }

            val goalError = when {
                !state.submitted -> null
                state.goalMinor == null -> R.string.create_goal_invalid
                Problem.GOAL_TOO_LARGE in state.problems -> R.string.create_goal_large
                else -> null
            }
            OutlinedTextField(
                value = state.goalText,
                onValueChange = onGoal,
                label = { Text(stringResource(R.string.create_goal)) },
                suffix = { Text(state.currency) },
                singleLine = true,
                isError = goalError != null,
                supportingText = { Text(stringResource(goalError ?: R.string.create_goal_help)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle(R.string.create_logging)
            OptionGroup(LoggingPolicy.entries, state.settings.loggingPolicy, { it.text }, onLogging)

            SectionTitle(R.string.create_pricing)
            OptionGroup(PricingMode.entries, state.settings.pricingMode, { it.text }, onPricing)

            SectionTitle(R.string.create_goal_behaviour)
            OptionGroup(GoalBehaviour.entries, state.settings.goalBehaviour, { it.text }, onGoalBehaviour)

            if (state.failed) {
                Text(stringResource(R.string.something_went_wrong), color = MaterialTheme.colorScheme.error)
            }
            if (state.saving) {
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            } else {
                Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.create_submit))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: Int) {
    Text(stringResource(text), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun <T> OptionGroup(options: List<T>, selected: T, text: (T) -> OptionText, onSelect: (T) -> Unit) {
    Column(Modifier.selectableGroup()) {
        for (option in options) {
            val label = text(option)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = option == selected, onClick = { onSelect(option) }, role = Role.RadioButton)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = option == selected, onClick = null)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(stringResource(label.title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(label.description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 1250)
@Composable
private fun CreateJarPreview() = SwearjarTheme {
    CreateJarContent(
        CreateJarState(name = "Flat 4", currencies = listOf("GBP", "EUR", "USD"), currency = "GBP", goalText = "20"),
        {}, {}, {}, {}, {}, {}, {}, {},
    )
}
