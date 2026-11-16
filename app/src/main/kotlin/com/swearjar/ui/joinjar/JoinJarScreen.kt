@file:OptIn(ExperimentalMaterial3Api::class)

package com.swearjar.ui.joinjar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.swearjar.R
import com.swearjar.ui.theme.SwearjarTheme

@Composable
fun JoinJarScreen(
    onBack: () -> Unit,
    onJoined: (jarId: String) -> Unit,
    viewModel: JoinJarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.jarId) { state.jarId?.let(onJoined) }

    val context = LocalContext.current
    JoinJarContent(
        state = state,
        onBack = onBack,
        onCode = viewModel::setCode,
        onSubmit = viewModel::submit,
        onScan = {
            // Google's scanner screen: it handles the camera itself, so the app needs no
            // camera permission. Backing out of it is not an error.
            val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
            GmsBarcodeScanning.getClient(context, options).startScan()
                .addOnSuccessListener { viewModel.onScanned(it.rawValue) }
                .addOnFailureListener { viewModel.onScanned(null) }
        },
    )
}

@Composable
private fun JoinJarContent(
    state: JoinJarState,
    onBack: () -> Unit,
    onCode: (String) -> Unit,
    onSubmit: () -> Unit,
    onScan: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.join_title)) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.join_help), style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = state.code,
                onValueChange = onCode,
                label = { Text(stringResource(R.string.join_code)) },
                singleLine = true,
                isError = state.error != null,
                supportingText = state.error?.let { { Text(stringResource(it)) } },
                textStyle = MaterialTheme.typography.headlineSmall,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.joining) {
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            } else {
                Button(onClick = onSubmit, enabled = state.code.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.join_submit))
                }
                OutlinedButton(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.join_scan))
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 480)
@Composable
private fun JoinJarPreview() = SwearjarTheme {
    JoinJarContent(JoinJarState(code = "ABC23", error = R.string.join_invalid), {}, {}, {}, {})
}
