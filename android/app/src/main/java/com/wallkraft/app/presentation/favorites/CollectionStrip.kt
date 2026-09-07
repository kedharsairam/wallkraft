package com.wallkraft.app.presentation.favorites

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftIconSize
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.data.db.CollectionWithItems

/**
 * Horizontal collections strip for the Favorites tab.
 *
 * A "New" card first, then one card per collection (cover + name + count).
 * Tap selects (tap again to clear); long-press opens rename/delete.
 * Covers resolve from the favorites list in the caller.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectionStrip(
    collections: List<CollectionWithItems>,
    covers: Map<String, String>,
    activeId: Long?,
    onSelect: (Long?) -> Unit,
    onNew: () -> Unit,
    onLongPress: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.collections_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = KraftSpacing.Spacing16,
                bottom = KraftSpacing.Spacing8,
            ),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing12),
            contentPadding = PaddingValues(horizontal = KraftSpacing.Spacing16),
        ) {
            item(key = "new") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(KraftSpacing.CollectionCardSize)
                        .combinedClickable(onClick = onNew),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(KraftSpacing.CollectionCardSize)
                            .clip(RoundedCornerShape(KraftRadius.Standard))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.new_collection),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(KraftIconSize.Medium),
                        )
                    }
                    Text(
                        text = stringResource(R.string.new_collection),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = KraftSpacing.Spacing4),
                    )
                }
            }
            items(collections, key = { "collection-${it.collection.id}" }) { entry ->
                val active = entry.collection.id == activeId
                val cover = entry.items.lastOrNull()?.wallpaperId?.let { covers[it] }.orEmpty()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(KraftSpacing.CollectionCardSize)
                        .combinedClickable(
                            onClick = { onSelect(if (active) null else entry.collection.id) },
                            onLongClick = { onLongPress(entry.collection.id) },
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .size(KraftSpacing.CollectionCardSize)
                            .clip(RoundedCornerShape(KraftRadius.Standard))
                            .border(
                                width = if (active) 2.dp else 0.dp,
                                color = if (active) MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                                shape = RoundedCornerShape(KraftRadius.Standard),
                            ),
                    ) {
                        AsyncImage(
                            model = cover.ifEmpty { null },
                            contentDescription = entry.collection.name,
                            placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                            error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                    Text(
                        text = entry.collection.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = KraftSpacing.Spacing4),
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.collection_item_count,
                            entry.items.size,
                            entry.items.size,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
