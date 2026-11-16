@file:OptIn(ExperimentalMaterial3Api::class)

package com.swearjar.ui.jarhome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swearjar.R
import com.swearjar.domain.model.Jar
import com.swearjar.domain.model.JarSettings
import com.swearjar.domain.model.JarStatus
import com.swearjar.domain.model.Member
import com.swearjar.domain.model.Role
import com.swearjar.domain.rules.GoalRules
import com.swearjar.domain.rules.Money
import com.swearjar.ui.common.QrCode
import com.swearjar.ui.common.label
import com.swearjar.ui.jar.JarCanvas
import com.swearjar.ui.theme.SwearjarTheme

@Composable
fun JarHomeScreen(onBack: () -> Unit, viewModel: JarHomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    JarHomeContent(state, onBack)
}

@Composable
private fun JarHomeContent(state: JarHomeState, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text((state as? JarHomeState.Loaded)?.jar?.name.orEmpty()) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        when (state) {
            JarHomeState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            JarHomeState.Missing -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.jar_missing))
            }
            is JarHomeState.Loaded -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                JarSummary(state.jar)
                InviteCard(state.jar)
                MembersCard(state.jar, state.members)
            }
        }
    }
}

@Composable
private fun JarSummary(jar: Jar) {
    val fraction = GoalRules.fillFraction(jar.roundTotalMinor, jar.goalMinor)
    JarCanvas(fillFraction = fraction, modifier = Modifier.fillMaxWidth(0.55f))

    val total = Money.format(jar.roundTotalMinor, jar.currency)
    Text(
        text = if (jar.goalMinor > 0) {
            stringResource(R.string.jar_progress, total, Money.format(jar.goalMinor, jar.currency))
        } else {
            stringResource(R.string.jar_no_goal, total)
        },
        style = MaterialTheme.typography.headlineSmall,
    )
    if (jar.goalMinor > 0) {
        LinearProgressIndicator(progress = { fraction.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
    }
    if (jar.status == JarStatus.CLOSED) {
        Text(stringResource(R.string.jar_closed), color = MaterialTheme.colorScheme.error)
    }
    Text(
        stringResource(R.string.jar_no_topics),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun InviteCard(jar: Jar) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.jar_invite_title), style = MaterialTheme.typography.titleMedium)
            Text(
                jar.inviteCode,
                style = MaterialTheme.typography.displaySmall,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp,
            )
            QrCode(
                content = jar.inviteCode,
                description = stringResource(R.string.jar_invite_qr, jar.inviteCode),
                modifier = Modifier.fillMaxWidth(0.6f),
            )
            Text(
                stringResource(R.string.jar_invite_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MembersCard(jar: Jar, members: List<Member>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.jar_members_title), style = MaterialTheme.typography.titleMedium)
            for (member in members) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(member.displayName, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(member.role.label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(Money.format(member.outstandingMinor, jar.currency), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 1100)
@Composable
private fun JarHomePreview() = SwearjarTheme {
    JarHomeContent(
        JarHomeState.Loaded(
            Jar("1", "Flat 4", "u1", "GBP", "ABC234", JarSettings(), "r1", 2000, 1250, memberIds = listOf("u1", "u2")),
            listOf(
                Member("u1", "Alice", null, Role.OWNER, owedMinor = 750, paidMinor = 500),
                Member("u2", "Bob", null, Role.MEMBER, owedMinor = 500),
            ),
        ),
        onBack = {},
    )
}
