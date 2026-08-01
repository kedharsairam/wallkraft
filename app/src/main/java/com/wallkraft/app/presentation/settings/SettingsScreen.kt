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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wallkraft.app.AppContainer
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.domain.model.Order
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(container: AppContainer) {
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(container.settings) }
        },
    )
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KraftSpacing.Spacing16, vertical = KraftSpacing.Spacing16),
        ) {
            SectionTitle("Wallhaven API key")
            Text(
                text = "Optional. Unlocks higher rate limits and exclusive content with your account key.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = KraftSpacing.Spacing8),
            )
            OutlinedTextField(
                value = settings.apiKey,
                onValueChange = viewModel::setApiKey,
                placeholder = { Text("Paste your API key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(KraftSpacing.Spacing24))
            SectionTitle("Appearance")
            FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(mode.name) },
                    )
                }
            }

            Spacer(Modifier.height(KraftSpacing.Spacing24))
            SectionTitle("Default sorting")
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
            SectionTitle("Default order")
            FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                Order.entries.forEach { order ->
                    FilterChip(
                        selected = settings.order == order,
                        onClick = { viewModel.setOrder(order) },
                        label = { Text(order.name.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            Spacer(Modifier.height(KraftSpacing.Spacing32))
            Text(
                text = "WallKraft v1.0.0 — Kotlin + Jetpack Compose",
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

private fun Sorting.displayName(): String = when (this) {
    Sorting.DateAdded -> "Date added"
    Sorting.Relevance -> "Relevance"
    Sorting.Random -> "Random"
    Sorting.Views -> "Views"
    Sorting.Favorites -> "Favorites"
    Sorting.Toplist -> "Toplist"
}
