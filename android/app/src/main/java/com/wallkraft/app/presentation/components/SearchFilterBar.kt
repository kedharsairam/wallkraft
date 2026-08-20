package com.wallkraft.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.core.graphics.toColorInt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
 * The search field + filter sheet shared by Browse and Tag screens.
 * Single-row chrome (search + Filters button) saves 252px vs the old 2-row
 * dropdowns — gallery-first, not control-panel. Filters live in a
 * ModalBottomSheet grouped list (Apple HIG sheet) so every filter stays
 * accessible without crowding the header.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val activeCount = remember(filters) {
        var c = 0
        if (filters.categories.size != Category.entries.size) c++
        if (filters.sorting != Sorting.DateAdded) c++
        if (filters.orientation != Orientation.Both) c++
        if (filters.color != null) c++
        if (filters.atleast != null) c++
        c
    }

    Column(modifier = modifier) {
        // Search row: field (with magnifier + clear) + Search button + Filters button.
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
                textStyle = MaterialTheme.typography.bodyLarge.copy(
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
                            .fillMaxWidth()
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
                                    style = MaterialTheme.typography.bodyLarge,
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
            Spacer(Modifier.width(KraftSpacing.Spacing8))
            Button(
                onClick = { showSheet = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeCount > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (activeCount > 0) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.height(44.dp),
            ) {
                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (activeCount > 0) "Filters • $activeCount" else "Filters")
            }
        }

        // Hairline separator so the header reads as a distinct surface above the grid.
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = KraftRadius.Hero, topEnd = KraftRadius.Hero),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = KraftSpacing.Spacing16, vertical = KraftSpacing.Spacing8)
                    .padding(bottom = KraftSpacing.Spacing24),
                verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing16),
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = KraftSpacing.Spacing4),
                )

                // Categories — multi-select chips (Apple home app style, not list rows)
                FilterSectionLabel("Categories")
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Category.entries.forEach { cat ->
                        val checked = cat in filters.categories
                        androidx.compose.material3.FilterChip(
                            selected = checked,
                            onClick = {
                                val current = filters.categories
                                val updated = if (cat in current) {
                                    if (current.size > 1) current - cat else current
                                } else current + cat
                                onFiltersChange(filters.copy(categories = updated))
                            },
                            label = { Text(cat.displayName()) },
                            leadingIcon = if (checked) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Sorting — FlowRow single-select chips
                FilterSectionLabel("Sort by")
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Sorting.entries.forEach { s ->
                        androidx.compose.material3.FilterChip(
                            selected = filters.sorting == s,
                            onClick = { onFiltersChange(filters.copy(sorting = s)) },
                            label = { Text(s.displayName()) },
                            leadingIcon = if (filters.sorting == s) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Orientation — FlowRow chips
                FilterSectionLabel("Orientation")
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Orientation.entries.forEach { o ->
                        androidx.compose.material3.FilterChip(
                            selected = filters.orientation == o,
                            onClick = { onFiltersChange(filters.copy(orientation = o)) },
                            label = { Text(o.displayName()) },
                            leadingIcon = if (filters.orientation == o) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Color — palette circles in FlowRow, not list rows
                FilterSectionLabel("Color")
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ColorOption.entries.forEach { c ->
                        val checked = filters.color == c.hex
                        androidx.compose.material3.FilterChip(
                            selected = checked,
                            onClick = { onFiltersChange(filters.copy(color = c.hex)) },
                            label = { Text(c.displayName()) },
                            leadingIcon = if (c.hex != null) {
                                {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(
                                                try {
                                                    androidx.compose.ui.graphics.Color("#${c.hex}".toColorInt())
                                                } catch (_: Exception) {
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                },
                                            ),
                                    )
                                }
                            } else null,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Resolution — chips FlowRow
                FilterSectionLabel("Minimum resolution")
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AtleastOption.entries.forEach { a ->
                        androidx.compose.material3.FilterChip(
                            selected = filters.atleast == a.value,
                            onClick = { onFiltersChange(filters.copy(atleast = a.value)) },
                            label = { Text(a.displayName()) },
                            leadingIcon = if (filters.atleast == a.value) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = KraftSpacing.Spacing8),
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing12),
                ) {
                    TextButton(
                        onClick = {
                            onFiltersChange(WallhavenFilters(query = filters.query))
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Reset") }
                    Button(
                        onClick = { showSheet = false },
                        modifier = Modifier.weight(1f),
                    ) { Text("Done") }
                }
                Spacer(Modifier.height(KraftSpacing.Spacing16))
            }
        }
    }
}

@Composable
private fun FilterSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = KraftSpacing.Spacing4),
    )
}

@Composable
private fun FilterSheetItem(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KraftRadius.Standard))
            .background(
                if (checked) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow,
            )
            .clickable { onClick() }
            .padding(horizontal = KraftSpacing.Spacing12, vertical = KraftSpacing.Spacing12),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Spacer(Modifier.width(KraftSpacing.Spacing8))
            trailing()
            Spacer(Modifier.width(KraftSpacing.Spacing8))
        }
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * A compact dropdown button for the filter row: a label + chevron that opens a
 * [DropdownMenu] anchored beneath it. Kept for legacy previews; sheet is primary.
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
