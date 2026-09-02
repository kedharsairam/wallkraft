package com.wallkraft.app.presentation.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.core.design.KraftIconSize
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.core.design.KraftTypeScale
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.util.formatCount

/**
 * The bottom-panel content: drag handle, uploader row, stat pills, and tags.
 *
 * `collapsed = true` shows just the drag handle and uploader row — the compact
 * bar. `collapsed = false` adds the divider, stat pills, and all tag chips.
 * `clickable = false` disables chip taps for the invisible measurement pass.
 * `loadAvatar = false` skips the uploader avatar network fetch in that same
 * pass (the fixed-size placeholder keeps the measured height identical).
 * `tagsScrollable` attaches a vertical scroll to the tag chips — only used
 * when the expanded content would overflow the panel's max height.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailPanelContent(
    wallpaper: Wallpaper,
    onTagClick: (String) -> Unit,
    onUploaderClick: (String) -> Unit,
    isUploaderDeleted: Boolean,
    clickable: Boolean = true,
    collapsed: Boolean = false,
    loadAvatar: Boolean = true,
    tagsScrollable: Boolean = false,
    pullHintVisible: Boolean = true,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = KraftSpacing.Spacing16)
            .padding(top = KraftSpacing.Spacing16, bottom = KraftSpacing.Spacing16 + bottomPadding),
    ) {
        // Drag handle — 40x5, glass treatment for consistent chrome.
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = KraftSpacing.Spacing40, height = KraftSpacing.Spacing4)
                .shadow(KraftSpacing.Spacing4, RoundedCornerShape(KraftRadius.DragHandle), clip = false)
                .clip(RoundedCornerShape(KraftRadius.DragHandle))
                .background(KraftColors.GlassDark),
        )
        Spacer(Modifier.height(KraftSpacing.Spacing16))

        // Uploader row. While the preview is still loading (no uploader data yet) a
        // pulsing skeleton occupies the slot; once the real detail loads it
        // crossfades to the uploader row (or the account-deleted state). All
        // three states are the same height, so the panel never changes size —
        // the load reads as content arriving, not a layout jump. This is the
        // collapsed bar's content — the divider, stats, and tags only appear
        // expanded.
        val uploaderState = when {
            wallpaper.uploaderName.isNotBlank() -> UploaderState.Loaded
            isUploaderDeleted -> UploaderState.Deleted
            else -> UploaderState.Loading
        }
        AnimatedContent(
            targetState = uploaderState,
            label = "uploaderState",
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = fadeIn(animationSpec = SharedElementSpringFloat),
                    initialContentExit = fadeOut(animationSpec = SharedElementSpringFloat),
                )
            },
            modifier = Modifier,
        ) { state ->
            when (state) {
                UploaderState.Loaded -> UploaderRow(
                    name = wallpaper.uploaderName,
                    avatarUrl = wallpaper.uploaderAvatarUrl,
                    createdAt = wallpaper.createdAt,
                    loadAvatar = loadAvatar,
                    onClick = { onUploaderClick(wallpaper.uploaderName) },
                    clickable = clickable,
                )
                UploaderState.Deleted -> DeletedUploaderRow()
                UploaderState.Loading -> UploaderRowPlaceholder()
            }
        }

        // Pull hint — "More details" — tells first-time users the bar swipes
        // up. It's placed between the uploader and the divider and rendered in
        // both collapsed and expanded states, so the collapsed panel's measured
        // height includes it and it's visible without any drag. It grows/shrinks
        // in place so the divider and stats below slide smoothly, and fades
        // away entirely once the user starts dragging (expanded) — it would be
        // pointless while the panel is visibly moving.
        Spacer(Modifier.height(KraftSpacing.Spacing4))
        AnimatedVisibility(
            visible = pullHintVisible,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(animationSpec = SharedElementSpringFloat),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(animationSpec = SharedElementSpringFloat),
            modifier = Modifier,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.more_details),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = KraftConstants.OverlayHintAlpha),
                        letterSpacing = KraftTypeScale.LabelSpacing,
                    ),
                )
                Spacer(Modifier.width(KraftSpacing.Spacing4))
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = KraftConstants.OverlayHintAlpha),
                    modifier = Modifier.size(KraftIconSize.Tiny),
                )
            }
        }
        Spacer(Modifier.height(KraftSpacing.Spacing8))

        // Expanded content: stat pills and tags. The live panel always renders
        // this (clipped to its current height), so the reveal is purely the
        // Box clip following the finger — no switching, no fade.
        if (!collapsed) {
            Column {
                Spacer(Modifier.height(KraftSpacing.Spacing16))

                // Stat pills: resolution, file size, views, favorites — category
                // is implied by tags and adds no decision value.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                ) {
                    StatPill(wallpaper.resolution)
                    StatPill(wallpaper.fileSizeFormatted())
                    if (wallpaper.views > 0) {
                        StatPill(stringResource(R.string.stat_views, formatCount(wallpaper.views)))
                    }
                    if (wallpaper.favorites > 0) {
                        StatPill(stringResource(R.string.stat_favorites, formatCount(wallpaper.favorites)))
                    }
                }

                if (wallpaper.tags.isNotEmpty()) {
                    Spacer(Modifier.height(KraftSpacing.Spacing16))
                    Text(
                        text = stringResource(R.string.tags_heading),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = KraftConstants.OverlayHintAlpha),
                            letterSpacing = KraftTypeScale.LabelSpacing,
                        ),
                    )
        Spacer(Modifier.height(KraftSpacing.Spacing4))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                        verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                        modifier = if (tagsScrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier,
                    ) {
                        wallpaper.tags.forEach { tag ->
                            DetailTagChip(name = tag.name, onClick = { onTagClick(tag.name) }, clickable = clickable)
                        }
                    }
                }
            }
        }
    }
}
