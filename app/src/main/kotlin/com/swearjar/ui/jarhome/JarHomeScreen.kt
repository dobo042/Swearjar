package com.swearjar.ui.jarhome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swearjar.R
import com.swearjar.domain.model.JarStatus
import com.swearjar.domain.model.Topic
import com.swearjar.domain.rules.Money
import com.swearjar.ui.jar.JarCanvas
import kotlinx.coroutines.delay

@Composable
fun JarHomeScreen(
    userName: String,
    onSignOut: () -> Unit,
    viewModel: JarHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    JarHomeContent(state, userName, onSignOut, onLog = viewModel::logOffence)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JarHomeContent(
    state: JarHomeState,
    userName: String,
    onSignOut: () -> Unit,
    onLog: (Topic) -> Unit,
) {
    // Show the celebration for a few seconds each time the goal is hit.
    var celebrating by remember { mutableStateOf(false) }
    LaunchedEffect(state.celebrations) {
        if (state.celebrations > 0) {
            celebrating = true
            delay(2600)
            celebrating = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.jarName) },
                actions = { TextButton(onClick = onSignOut) { Text(stringResource(R.string.sign_out)) } },
            )
        },
    ) { padding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.demo_banner),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.signed_in_as, userName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stringResource(R.string.jar_round, state.round), style = MaterialTheme.typography.labelLarge)

            JarCanvas(
                fillFraction = state.fillFraction,
                modifier = Modifier.fillMaxWidth(0.6f),
                dropKey = state.entryCount,
                celebrate = celebrating,
            )

            val total = Money.format(state.roundTotalMinor, state.currency)
            Text(
                text = if (state.goalMinor > 0) {
                    stringResource(R.string.jar_progress, total, Money.format(state.goalMinor, state.currency))
                } else {
                    stringResource(R.string.jar_no_goal, total)
                },
                style = MaterialTheme.typography.headlineSmall,
            )
            if (state.goalMinor > 0) {
                LinearProgressIndicator(
                    progress = { state.fillFraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            when {
                state.status == JarStatus.CLOSED ->
                    Text(stringResource(R.string.jar_closed), color = MaterialTheme.colorScheme.error)
                celebrating ->
                    Text(stringResource(R.string.jar_goal_reached), style = MaterialTheme.typography.titleMedium)
            }

            Text(
                stringResource(R.string.log_offence),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            for (topic in state.topics) {
                FilledTonalButton(
                    onClick = { onLog(topic) },
                    enabled = state.status == JarStatus.ACTIVE,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            R.string.topic_button,
                            topic.emoji,
                            topic.name,
                            Money.format(state.priceOf(topic), state.currency),
                        ),
                    )
                }
            }
        }
    }
}
