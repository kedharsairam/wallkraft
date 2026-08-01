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
import androidx.compose.ui.unit.dp
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.domain.model.Category
import com.wallkraft.app.domain.model.Order
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.TopRange
import com.wallkraft.app.domain.model.WallhavenFilters

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
                text = "Filters",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(KraftSpacing.Spacing16))

            SectionLabel("Categories")
            FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { cat ->
                    FilterChip(
                        selected = cat in categories,
                        onClick = { categories = categories.toggle(cat) },
                        label = { Text(cat.value.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            Spacer(Modifier.height(KraftSpacing.Spacing16))
            SectionLabel("Sort by")
            FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                Sorting.entries.forEach { s ->
                    FilterChip(
                        selected = sorting == s,
                        onClick = { sorting = s },
                        label = { Text(s.displayName()) },
                    )
                }
            }

            if (sorting == Sorting.Toplist) {
                Spacer(Modifier.height(KraftSpacing.Spacing16))
                SectionLabel("Top of the day / week / ...")
                FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    TopRange.entries.forEach { r ->
                        FilterChip(
                            selected = topRange == r,
                            onClick = { topRange = if (topRange == r) null else r },
                            label = { Text(r.displayName()) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(KraftSpacing.Spacing16))
            SectionLabel("Order")
            FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                Order.entries.forEach { o ->
                    FilterChip(
                        selected = order == o,
                        onClick = { order = o },
                        label = { Text(o.value.replaceFirstChar { it.uppercase() }) },
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
                            topRange = topRange,
                            ratio = initial.ratio,
                            query = initial.query,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Apply filters")
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

private fun Sorting.displayName(): String = when (this) {
    Sorting.DateAdded -> "Date added"
    Sorting.Relevance -> "Relevance"
    Sorting.Random -> "Random"
    Sorting.Views -> "Views"
    Sorting.Favorites -> "Favorites"
    Sorting.Toplist -> "Toplist"
}

private fun TopRange.displayName(): String = when (this) {
    TopRange.Day1 -> "24h"
    TopRange.Days3 -> "3 days"
    TopRange.Week1 -> "1 week"
    TopRange.Month1 -> "1 month"
    TopRange.Months3 -> "3 months"
    TopRange.Months6 -> "6 months"
    TopRange.Year1 -> "1 year"
}
