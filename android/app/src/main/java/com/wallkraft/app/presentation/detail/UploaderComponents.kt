package com.wallkraft.app.presentation.detail

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.core.design.KraftIconSize
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing

/** The uploader row's three visual states, crossfaded as the detail loads. */
internal enum class UploaderState { Loading, Loaded, Deleted }

/**
 * The clickable uploader row: avatar + username. Tapping it opens the
 * uploader's wallpapers (search query `@username`), mirroring tag behavior.
 * Only the avatar + name are clickable — the row wraps its content instead of
 * filling the panel width, so the tap target (and its ripple) stays compact
 * rather than covering the whole strip.
 */
@Composable
internal fun UploaderRow(
    name: String,
    avatarUrl: String,
    createdAt: String = "",
    loadAvatar: Boolean,
    onClick: () -> Unit,
    clickable: Boolean,
) {
    val viewByDesc = stringResource(R.string.view_by_uploader, name)
    val timeAgo = remember(createdAt) { relativeTime(createdAt) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = viewByDesc }
            .padding(vertical = KraftSpacing.Spacing2),
    ) {
        UploaderAvatar(avatarUrl = avatarUrl, name = name, loadAvatar = loadAvatar)
        Spacer(Modifier.width(KraftSpacing.Spacing12))
        Text(
            text = if (timeAgo != null) "$name • $timeAgo" else name,
            style = MaterialTheme.typography.titleSmall.copy(
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // Cap the width so a very long username ellipsizes instead of
            // overflowing the panel; the tap target stays compact.
            modifier = Modifier.widthIn(max = 260.dp),
        )
    }
}

/**
 * A pulsing skeleton shown in the uploader slot while the real uploader
 * details load. Same dimensions as the real row (32dp avatar + name), so the
 * panel never changes size when the data arrives — it reads as content
 * loading, not a layout jump.
 */
@Composable
internal fun UploaderRowPlaceholder() {
    val pulse by rememberInfiniteTransition(label = "uploaderPlaceholder").animateFloat(
        initialValue = KraftConstants.SkeletonAlphaMin,
        targetValue = KraftConstants.SkeletonAlphaMax,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "uploaderPlaceholderAlpha",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = KraftSpacing.Spacing2),
    ) {
        Box(
            modifier = Modifier
                .size(KraftSpacing.AvatarSize)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = pulse)),
        )
        Spacer(Modifier.width(KraftSpacing.Spacing12))
        Box(
            modifier = Modifier
                .width(KraftSpacing.Spacing40 * 3)
                .height(KraftSpacing.Spacing4)
                .clip(RoundedCornerShape(KraftRadius.Small))
                .background(Color.White.copy(alpha = pulse)),
        )
    }
}

/**
 * The uploader's avatar: the real image when available, otherwise the
 * username's initial on a deterministic accent color. `loadAvatar = false`
 * (measurement pass) renders the placeholder directly — same size, no network.
 */
@Composable
internal fun UploaderAvatar(
    avatarUrl: String,
    name: String,
    loadAvatar: Boolean,
    modifier: Modifier = Modifier,
) {
    val initial = name.firstOrNull()?.uppercase() ?: "?"
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(KraftSpacing.AvatarSize)
            .clip(CircleShape)
            .background(avatarColor(name)),
    ) {
        if (loadAvatar && avatarUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { InitialLetter(initial) },
                error = { InitialLetter(initial) },
            )
        } else {
            InitialLetter(initial)
        }
    }
}

@Composable
internal fun InitialLetter(initial: String) {
    Text(
        text = initial,
        style = MaterialTheme.typography.labelMedium.copy(
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        ),
    )
}

/**
 * Shown when the loaded detail has no uploader — the account was deleted but
 * the wallpaper remains. Not clickable: there is no user to browse.
 */
@Composable
internal fun DeletedUploaderRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = KraftSpacing.Spacing2),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(KraftSpacing.AvatarSize)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = KraftConstants.DeletedUploaderBgAlpha)),
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = Color.White.copy(alpha = KraftConstants.DeletedUploaderTextAlpha),
                modifier = Modifier.size(KraftIconSize.Small),
            )
        }
        Spacer(Modifier.width(KraftSpacing.Spacing12))
        Text(
            text = stringResource(R.string.account_deleted),
            style = MaterialTheme.typography.titleSmall.copy(
                color = Color.White.copy(alpha = KraftConstants.DeletedUploaderTextAlpha),
            ),
        )
    }
}

/** Deterministic avatar color per username, picked from the Aurora palette. */
private val avatarPalette = listOf(
    KraftColors.AccentBlue,
    KraftColors.AccentBlue,
    KraftColors.AccentOrange,
    KraftColors.AccentGreen,
    KraftColors.AccentRed,
)

internal fun avatarColor(name: String): Color =
    avatarPalette[(name.hashCode() and Int.MAX_VALUE) % avatarPalette.size]

/** Parses Wallhaven `created_at` (yyyy-MM-dd HH:mm:ss UTC) to a short relative time. */
internal fun relativeTime(createdAt: String): String? {
    if (createdAt.isBlank()) return null
    return try {
        val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val ldt = java.time.LocalDateTime.parse(createdAt, fmt)
        val instant = ldt.atZone(java.time.ZoneId.of("UTC")).toInstant()
        val now = java.time.Instant.now()
        val d = java.time.Duration.between(instant, now)
        if (d.isNegative) return null
        val mins = d.toMinutes()
        val hours = d.toHours()
        val days = d.toDays()
        when {
            mins < 1 -> "just now"
            mins < 60 -> "${mins}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            days < 30 -> "${days / 7}w ago"
            days < 365 -> "${days / 30}mo ago"
            else -> "${days / 365}y ago"
        }
    } catch (_: Exception) {
        null
    }
}
