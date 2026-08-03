package com.wallkraft.app.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wallkraft.app.AppContainer
import com.wallkraft.app.BuildConfig
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.domain.model.Order
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.ThemeMode
import com.wallkraft.app.util.displayName

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(container: AppContainer) {
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(container.settings) }
        },
    )
    val settings by viewModel.settings.collectAsState()
    val apiKeyText by viewModel.apiKeyText.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KraftSpacing.Spacing16, vertical = KraftSpacing.Spacing16),
        ) {
            SectionTitle(stringResource(R.string.api_key_title))
            Text(
                text = stringResource(R.string.api_key_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = KraftSpacing.Spacing8),
            )
            OutlinedTextField(
                value = apiKeyText,
                onValueChange = viewModel::setApiKey,
                placeholder = { Text(stringResource(R.string.api_key_hint)) },
                singleLine = true,
                // API keys are case-sensitive alphanumerics: autocorrect and
                // capitalization would silently mangle them (e.g. Gboard
                // inserting punctuation), so lock the input to plain ASCII.
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(KraftSpacing.Spacing24))
            SectionTitle(stringResource(R.string.appearance))
            FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(mode.displayName()) },
                    )
                }
            }

            Spacer(Modifier.height(KraftSpacing.Spacing24))
            SectionTitle(stringResource(R.string.default_sorting))
            FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                Sorting.entries.forEach { sorting ->
                    FilterChip(
                        selected = settings.sorting == sorting,
                        onClick = { viewModel.setSorting(sorting) },
                        label = { Text(sorting.displayName()) },
                    )
                }
            }

            Spacer(Modifier.height(KraftSpacing.Spacing24))
            SectionTitle(stringResource(R.string.default_order))
            FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                Order.entries.forEach { order ->
                    FilterChip(
                        selected = settings.order == order,
                        onClick = { viewModel.setOrder(order) },
                        label = { Text(order.displayName()) },
                    )
                }
            }

            Spacer(Modifier.height(KraftSpacing.Spacing32))
            Text(
                text = stringResource(R.string.version_format, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = KraftSpacing.Spacing8),
    )
}
