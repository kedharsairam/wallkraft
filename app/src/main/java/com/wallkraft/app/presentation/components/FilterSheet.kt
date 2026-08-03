package com.wallkraft.app.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.domain.model.Category
import com.wallkraft.app.domain.model.Order
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.TopRange
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.util.displayName

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    initial: WallhavenFilters,
    onApply: (WallhavenFilters) -> Unit,
    onDismiss: () -> Unit,
) {
    var categories by remember { mutableStateOf(initial.categories) }
    var sorting by remember { mutableStateOf(initial.sorting) }
    var order by remember { mutableStateOf(initial.order) }
    var topRange by remember { mutableStateOf(initial.topRange) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = KraftSpacing.Spacing16)
                .padding(bottom = KraftSpacing.Spacing24),
        ) {
            Text(
                text = stringResource(R.string.filter_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(KraftSpacing.Spacing16))

            SectionLabel(stringResource(R.string.filter_categories))
            FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(KraftSpacing.Spacing8)) {
                Category.entries.forEach { cat ->
                    FilterChip(
                        selected = cat in categories,
                        // Never allow deselecting the last category — an empty
                        // mask ("000") silently returns zero results.
                        onClick = { if (cat !in categories || categories.size > 1) categories = categories.toggle(cat) },
                        label = { Text(cat.displayName()) },
                    )
                }
            }

            Spacer(Modifier.height(KraftSpacing.Spacing16))
            SectionLabel(stringResource(R.string.filter_sort_by))
            FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(KraftSpacing.Spacing8)) {
                Sorting.entries.forEach { s ->
                    FilterChip(
                        selected = sorting == s,
                        // topRange only applies to Toplist; drop it when switching
                        // so a stale range isn't sent with other sorts.
                        onClick = {
                            sorting = s
                            if (s != Sorting.Toplist) topRange = null
                        },
                        label = { Text(s.displayName()) },
                    )
                }
            }

            if (sorting == Sorting.Toplist) {
                Spacer(Modifier.height(KraftSpacing.Spacing16))
                SectionLabel(stringResource(R.string.filter_time_range))
                FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(KraftSpacing.Spacing8)) {
                    TopRange.entries.forEach { r ->
                        FilterChip(
                            selected = topRange == r,
                            // Tapping an active range clears it (back to all-time).
                            onClick = { topRange = if (topRange == r) null else r },
                            label = { Text(r.displayName()) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(KraftSpacing.Spacing16))
            SectionLabel(stringResource(R.string.filter_order))
            FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(KraftSpacing.Spacing8)) {
                Order.entries.forEach { o ->
                    FilterChip(
                        selected = order == o,
                        onClick = { order = o },
                        label = { Text(o.displayName()) },
                    )
                }
            }

            Spacer(Modifier.height(KraftSpacing.Spacing24))
            Button(
                onClick = {
                    onApply(
                        WallhavenFilters(
                            categories = categories,
                            sorting = sorting,
                            order = order,
                            topRange = if (sorting == Sorting.Toplist) topRange else null,
                            ratio = initial.ratio,
                            query = initial.query,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.filter_apply))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = KraftSpacing.Spacing8),
    )
}

private fun <T> Set<T>.toggle(value: T): Set<T> =
    if (value in this) this - value else this + value
