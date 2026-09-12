package com.wallkraft.app.presentation.favorites

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
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
import com.wallkraft.app.domain.model.Collection

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
    collections: List<Collection>,
    covers: Map<String, String>,
    activeId: Long?,
    onSelect: (Long?) -> Unit,
    onNew: () -> Unit,
    onMenu: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.collections_title).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = com.wallkraft.app.core.design.KraftTypeScale.LabelSpacing),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = KraftSpacing.Spacing12,
                bottom = KraftSpacing.Spacing8,
            ),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing12),
            contentPadding = PaddingValues(horizontal = 0.dp),
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
            items(collections, key = { "collection-${it.id}" }) { entry ->
                val active = entry.id == activeId
                val cover = entry.items.lastOrNull()?.let { covers[it] }.orEmpty()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(KraftSpacing.CollectionCardSize)
                        .combinedClickable(
                            onClick = { onSelect(if (active) null else entry.id) },
                            onLongClick = { onMenu(entry.id) },
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
                            contentDescription = entry.name,
                            placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                            error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                        // Overflow affordance: long-press is undiscoverable on
                        // its own. Inner taps win over the parent card tap.
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(KraftSpacing.Spacing4)
                                .size(KraftSpacing.TouchTarget)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable { onMenu(entry.id) },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.collection_options),
                                tint = Color.White,
                                modifier = Modifier.size(KraftIconSize.Small),
                            )
                        }
                    }
                    Text(
                        text = entry.name,
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
