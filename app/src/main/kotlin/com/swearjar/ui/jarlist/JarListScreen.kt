@file:OptIn(ExperimentalMaterial3Api::class)

package com.swearjar.ui.jarlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swearjar.R
import com.swearjar.domain.model.Jar
import com.swearjar.domain.model.JarSettings
import com.swearjar.domain.rules.GoalRules
import com.swearjar.domain.rules.Money
import com.swearjar.ui.jar.JarCanvas
import com.swearjar.ui.theme.SwearjarTheme

@Composable
fun JarListScreen(
    userName: String,
    onSignOut: () -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onOpen: (jarId: String) -> Unit,
    viewModel: JarListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    JarListContent(state, userName, onSignOut, onCreate, onJoin, onOpen)
}

@Composable
private fun JarListContent(
    state: JarListState,
    userName: String,
    onSignOut: () -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onOpen: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.jars_title)) },
                actions = { TextButton(onClick = onSignOut) { Text(stringResource(R.string.sign_out)) } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.signed_in_as, userName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when (state) {
                JarListState.Loading -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                JarListState.Failed -> item {
                    Text(stringResource(R.string.something_went_wrong), color = MaterialTheme.colorScheme.error)
                }
                is JarListState.Loaded -> {
                    if (state.jars.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.jars_empty),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                    items(state.jars, key = { it.id }) { jar -> JarRow(jar, onClick = { onOpen(jar.id) }) }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onCreate, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.jars_create))
                    }
                    OutlinedButton(onClick = onJoin, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.jars_join))
                    }
                }
            }
        }
    }
}

@Composable
private fun JarRow(jar: Jar, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            JarCanvas(GoalRules.fillFraction(jar.roundTotalMinor, jar.goalMinor), Modifier.width(44.dp))
            Column(Modifier.padding(start = 16.dp).weight(1f)) {
                Text(jar.name, style = MaterialTheme.typography.titleMedium)
                val total = Money.format(jar.roundTotalMinor, jar.currency)
                Text(
                    if (jar.goalMinor > 0) {
                        stringResource(R.string.jar_progress, total, Money.format(jar.goalMinor, jar.currency))
                    } else {
                        stringResource(R.string.jar_no_goal, total)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    if (jar.memberIds.size <= 1) {
                        stringResource(R.string.jars_one_member)
                    } else {
                        stringResource(R.string.jars_members, jar.memberIds.size)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun previewJar(id: String, name: String, total: Long, goal: Long, members: Int) = Jar(
    id, name, "u1", "GBP", "ABC234", JarSettings(), "r1", goal, total, memberIds = List(members) { "u$it" },
)

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun JarListPreview() = SwearjarTheme {
    JarListContent(
        JarListState.Loaded(
            listOf(
                previewJar("1", "Flat 4", 1250, 2000, 4),
                previewJar("2", "Office", 300, 5000, 12),
                previewJar("3", "Just me", 450, 0, 1),
            ),
        ),
        "Alice", {}, {}, {}, {},
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun JarListEmptyPreview() = SwearjarTheme {
    JarListContent(JarListState.Loaded(emptyList()), "Alice", {}, {}, {}, {})
}
