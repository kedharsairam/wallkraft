package com.wallkraft.app.presentation.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.core.design.KraftIconSize
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing

/**
 * Apple-polished collection sheets — matching the liquid-glass top/bottom bars.
 *
 * Old: centered Dialog + Surface + Checkbox + AlertDialog.
 * New: bottom sheets with grabber (2.5dp), Hero 22dp corners, 8px rhythm,
 * checkmark accessories (not boxes), SF Symbol style icons, search, haptics,
 * and the same 4-layer frost as GlassTabBar (Black 0.22/White 0.22/3A3A3C 0.45/2C2C2E 0.15).
 */

// ── Add to collection — bottom sheet ──────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToCollectionDialog(
    collections: List<com.wallkraft.app.domain.model.Collection>,
    selectedIds: Set<String>,
    onToggle: (collectionId: Long, member: Boolean) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var filter by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    val filtered = remember(collections, filter) {
        if (filter.isBlank()) collections
        else collections.filter { it.name.contains(filter, ignoreCase = true) }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = KraftRadius.Hero, topEnd = KraftRadius.Hero),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = KraftSpacing.Spacing12)
                    .size(width = 36.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        },
    ) {
        Column(modifier = Modifier.padding(KraftSpacing.Spacing20).navigationBarsPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.add_to_collection), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
                }
                Spacer(Modifier.height(KraftSpacing.Spacing12))
                BasicTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(KraftSpacing.SearchBarHeight)
                        .clip(RoundedCornerShape(KraftRadius.Pill))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = KraftSpacing.Spacing16),
                    decorationBox = { inner ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(KraftIconSize.Small))
                            Spacer(Modifier.width(KraftSpacing.Spacing8))
                            Box(Modifier.weight(1f)) {
                                if (filter.isEmpty()) Text(stringResource(R.string.search_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                inner()
                            }
                            if (filter.isNotEmpty()) {
                                IconButton(onClick = { filter = "" }, modifier = Modifier.size(KraftSpacing.TouchTarget)) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.search_clear), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    },
                )
                Spacer(Modifier.height(KraftSpacing.Spacing12))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = KraftConstants.DialogListMaxHeightDp.dp),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing2),
                ) {
                    items(filtered, key = { it.id }) { entry ->
                        val members = entry.items.toSet()
                        val allMembers = selectedIds.isNotEmpty() && selectedIds.all { it in members }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(KraftRadius.Standard))
                                .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onToggle(entry.id, !allMembers) }
                                .padding(horizontal = KraftSpacing.Spacing8, vertical = KraftSpacing.Spacing12),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(pluralStringResource(R.plurals.collection_item_count, entry.items.size, entry.items.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                imageVector = if (allMembers) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = if (allMembers) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(KraftIconSize.Medium),
                            )
                        }
                    }
                    if (filtered.isEmpty() && filter.isNotBlank()) {
                        item {
                            Text(stringResource(R.string.no_results_title), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = KraftSpacing.Spacing16, horizontal = KraftSpacing.Spacing8))
                        }
                    }
                }
                Spacer(Modifier.height(KraftSpacing.Spacing12))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(Modifier.height(KraftSpacing.Spacing12))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(KraftIconSize.Medium))
                    Spacer(Modifier.width(KraftSpacing.Spacing8))
                    BasicTextField(
                        value = newName,
                        onValueChange = { if (it.length <= KraftConstants.CollectionNameMaxLength) newName = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(KraftRadius.Pill))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(horizontal = KraftSpacing.Spacing12),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxWidth()) {
                                if (newName.isEmpty()) Text(stringResource(R.string.collection_name_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                inner()
                            }
                        },
                    )
                    Spacer(Modifier.width(KraftSpacing.Spacing8))
                    TextButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onCreate(newName.trim()); newName = "" }, enabled = newName.trim().isNotEmpty()) { Text(stringResource(R.string.create)) }
                }
            }
        }
}

// ── 3-dots collection menu — bottom sheet ─

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionMenuDialog(
    name: String,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = KraftRadius.Hero, topEnd = KraftRadius.Hero),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = KraftSpacing.Spacing12)
                    .size(width = 36.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        },
    ) {
        Column(modifier = Modifier.padding(vertical = KraftSpacing.Spacing8).navigationBarsPadding()) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = KraftSpacing.Spacing20, vertical = KraftSpacing.Spacing8),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                // Rename — centered to match Cancel
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onRename() }
                        .padding(horizontal = KraftSpacing.Spacing20, vertical = KraftSpacing.Spacing16),
                ) {
                    Icon(Icons.Outlined.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(KraftIconSize.Medium))
                    Spacer(Modifier.width(KraftSpacing.Spacing12))
                    Text(stringResource(R.string.rename), style = MaterialTheme.typography.bodyLarge)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onDelete() }
                        .padding(horizontal = KraftSpacing.Spacing20, vertical = KraftSpacing.Spacing16),
                ) {
                    Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(KraftIconSize.Medium))
                    Spacer(Modifier.width(KraftSpacing.Spacing12))
                    Text(stringResource(R.string.delete), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().clickable { onDismiss() }.padding(vertical = KraftSpacing.Spacing12),
                ) {
                    Text(stringResource(R.string.cancel), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
}

// ── Rename / New — bottom sheet ─

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionNameDialog(
    title: String,
    current: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    @StringRes confirmLabel: Int = R.string.rename,
) {
    var text by remember(current) { mutableStateOf(current) }
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = KraftRadius.Hero, topEnd = KraftRadius.Hero),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = KraftSpacing.Spacing12)
                    .size(width = 36.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        },
    ) {
        Column(modifier = Modifier.padding(KraftSpacing.Spacing20).navigationBarsPadding()) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(KraftSpacing.Spacing16))
                BasicTextField(
                    value = text,
                    onValueChange = { if (it.length <= KraftConstants.CollectionNameMaxLength) text = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(KraftRadius.Pill))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .padding(horizontal = KraftSpacing.Spacing16)
                        .focusRequester(focusRequester),
                    decorationBox = { inner ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Box(Modifier.weight(1f)) {
                                if (text.isEmpty()) Text(stringResource(R.string.collection_name_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                inner()
                            }
                            if (text.isNotEmpty()) {
                                IconButton(onClick = { text = "" }, modifier = Modifier.size(KraftSpacing.TouchTarget)) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.search_clear), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    },
                )
                Spacer(Modifier.height(KraftSpacing.Spacing4))
                Text(
                    text = "${text.trim().length}/${KraftConstants.CollectionNameMaxLength}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (text.length >= KraftConstants.CollectionNameMaxLength) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End),
                )
                Spacer(Modifier.height(KraftSpacing.Spacing12))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Spacer(Modifier.width(KraftSpacing.Spacing8))
                    TextButton(
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSave(text.trim()) },
                        enabled = text.trim().isNotEmpty(),
                    ) { Text(stringResource(confirmLabel)) }
                }
            }
        }
}

// Backward-compat alias — FavoritesScreen previously used RenameCollectionDialog.
// New code should call CollectionNameDialog directly.
@Composable
@Deprecated(
    "Renamed to CollectionNameDialog",
    ReplaceWith("CollectionNameDialog(title, current, onDismiss, onSave, confirmLabel)"),
)
fun RenameCollectionDialog(
    title: String,
    current: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    @StringRes confirmLabel: Int = R.string.rename,
) = CollectionNameDialog(title, current, onDismiss, onSave, confirmLabel)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteCollectionDialog(
    name: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = KraftRadius.Hero, topEnd = KraftRadius.Hero),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = KraftSpacing.Spacing12)
                    .size(width = 36.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        },
    ) {
        Column(modifier = Modifier.padding(KraftSpacing.Spacing20).navigationBarsPadding()) {
                Text(stringResource(R.string.delete_collection_title, name), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(KraftSpacing.Spacing12))
                Text(stringResource(R.string.delete_collection_message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(KraftSpacing.Spacing20))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Spacer(Modifier.width(KraftSpacing.Spacing8))
                    TextButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onConfirm() }) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
}
