package com.wallkraft.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.core.graphics.toColorInt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.domain.model.Category
import com.wallkraft.app.domain.model.Orientation
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.util.displayName

private val RoundedCornerShapeDp = RoundedCornerShape(KraftRadius.Standard)

/**
 * The search field + filter dropdown row shared by the Browse tab and the
 * Tag screen. Both screens need identical search/filter controls; this keeps
 * the UI in one place so behavior stays consistent.
 *
 * [query] is the current search text (hoisted by the caller so it survives
 * recomposition), [onSearch] fires on the search button / IME action, and
 * [onFiltersChange] fires whenever a filter dropdown changes.
 */
@Composable
fun SearchFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    filters: WallhavenFilters,
    onFiltersChange: (WallhavenFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current

    // Dropdown expansion state for filter buttons.
    var categoriesExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }
    var orientationExpanded by remember { mutableStateOf(false) }
    var colorExpanded by remember { mutableStateOf(false) }
    var atleastExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Search row: field (with magnifier + clear) + physical search button.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = KraftSpacing.Spacing16, vertical = KraftSpacing.Spacing8),
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboard?.hide()
                        onSearch(query)
                    },
                ),
                textStyle = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShapeDp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = KraftSpacing.Spacing12),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(KraftSpacing.Spacing8))
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (query.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_hint),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            innerTextField()
                        }
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { onQueryChange("") },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.search_clear),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                },
            )
            Spacer(Modifier.width(KraftSpacing.Spacing8))
            Button(
                onClick = {
                    keyboard?.hide()
                    onSearch(query)
                },
                modifier = Modifier.height(44.dp),
            ) {
                Text(stringResource(R.string.search_action))
            }
        }

        // Filter dropdown row: Categories, Sort, Orientation.
        Row(
            horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KraftSpacing.Spacing16, vertical = KraftSpacing.Spacing4),
        ) {
            FilterDropdownButton(
                label = categoriesLabel(filters.categories),
                expanded = categoriesExpanded,
                onExpandedChange = { categoriesExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                Category.entries.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat.displayName()) },
                        leadingIcon = {
                            if (cat in filters.categories) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            val current = filters.categories
                            val updated = if (cat in current) {
                                // Never allow deselecting the last category —
                                // an empty mask ("000") returns zero results.
                                if (current.size > 1) current - cat else current
                            } else {
                                current + cat
                            }
                            onFiltersChange(filters.copy(categories = updated))
                        },
                    )
                }
            }
            FilterDropdownButton(
                label = filters.sorting.displayName(),
                expanded = sortExpanded,
                onExpandedChange = { sortExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                Sorting.entries.forEach { s ->
                    DropdownMenuItem(
                        text = { Text(s.displayName()) },
                        leadingIcon = {
                            if (filters.sorting == s) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            onFiltersChange(filters.copy(sorting = s))
                            sortExpanded = false
                        },
                    )
                }
            }
            FilterDropdownButton(
                label = filters.orientation.displayName(),
                expanded = orientationExpanded,
                onExpandedChange = { orientationExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                Orientation.entries.forEach { o ->
                    DropdownMenuItem(
                        text = { Text(o.displayName()) },
                        leadingIcon = {
                            if (filters.orientation == o) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            onFiltersChange(filters.copy(orientation = o))
                            orientationExpanded = false
                        },
                    )
                }
            }
        }
        // Second filter row: Color + Min resolution (Wallhaven colors/atleast).
        Row(
            horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KraftSpacing.Spacing16, vertical = KraftSpacing.Spacing4),
        ) {
            FilterDropdownButton(
                label = colorLabel(filters.color),
                expanded = colorExpanded,
                onExpandedChange = { colorExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                // All + 12 palette colors matching Wallhaven's palette.
                ColorOption.entries.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c.displayName()) },
                        leadingIcon = {
                            if (filters.color == c.hex) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        },
                        trailingIcon = if (c.hex != null) {
                            {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            try {
                                                androidx.compose.ui.graphics.Color(
                                                    "#${c.hex}".toColorInt(),
                                                )
                                            } catch (_: Exception) {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            },
                                        ),
                                )
                            }
                        } else null,
                        onClick = {
                            onFiltersChange(filters.copy(color = c.hex))
                            colorExpanded = false
                        },
                    )
                }
            }
            FilterDropdownButton(
                label = atleastLabel(filters.atleast),
                expanded = atleastExpanded,
                onExpandedChange = { atleastExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                AtleastOption.entries.forEach { a ->
                    DropdownMenuItem(
                        text = { Text(a.displayName()) },
                        leadingIcon = {
                            if (filters.atleast == a.value) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            onFiltersChange(filters.copy(atleast = a.value))
                            atleastExpanded = false
                        },
                    )
                }
            }
        }

        // Hairline separator so the header reads as a distinct surface
        // above the scrolling grid.
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )
    }
}

/**
 * A compact dropdown button for the filter row: a label + chevron that opens a
 * [DropdownMenu] anchored beneath it. The label always shows the current value
 * so the user knows what filter is applied at a glance.
 */
@Composable
private fun FilterDropdownButton(
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KraftRadius.Standard))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { onExpandedChange(!expanded) }
                .padding(horizontal = KraftSpacing.Spacing12, vertical = KraftSpacing.Spacing8),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            content()
        }
    }
}

/** Compact label for the categories dropdown: "All", a single name, or "X +N". */
@Composable
private fun categoriesLabel(categories: Set<Category>): String = when {
    categories.size == Category.entries.size -> stringResource(R.string.filter_all)
    categories.size == 1 -> categories.first().displayName()
    else -> "${categories.first().displayName()} +${categories.size - 1}"
}

private enum class ColorOption(val hex: String?, val label: String) {
    All(null, "All colors"),
    Red("ff0000", "Red"),
    Orange("ff7f00", "Orange"),
    Yellow("ffff00", "Yellow"),
    Green("00ff00", "Green"),
    Cyan("00ffff", "Cyan"),
    Blue("0000ff", "Blue"),
    Purple("800080", "Purple"),
    Pink("ff69b4", "Pink"),
    Brown("a52a2a", "Brown"),
    Gray("808080", "Gray"),
    Black("000000", "Black"),
    White("ffffff", "White"),
    ;
    @Composable fun displayName(): String = label
}

private enum class AtleastOption(val value: String?, val label: String) {
    All(null, "Any res"),
    HD("1920x1080", "HD 1920×1080"),
    QHD("2560x1440", "QHD 2560×1440"),
    UHD("3840x2160", "4K 3840×2160"),
    UHDPlus("5120x2880", "5K 5120×2880"),
    ;
    @Composable fun displayName(): String = label
}

@Composable
private fun colorLabel(hex: String?): String = when (hex) {
    null -> "Color"
    else -> ColorOption.entries.firstOrNull { it.hex == hex }?.label ?: "#$hex"
}

@Composable
private fun atleastLabel(value: String?): String = when (value) {
    null -> "Resolution"
    else -> AtleastOption.entries.firstOrNull { it.value == value }?.label ?: value
}