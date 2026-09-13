package com.wallkraft.app.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftIconSize
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import kotlinx.coroutines.launch

/**
 * Apple-grade welcome for first launch — 3 pages, useful, not janky.
 * Page 1: Crafted — browse Wallhaven privately
 * Page 2: Organize — favorites + collections + rotation
 * Page 3: Private — no trackers, local only, go
 * Full-screen, not a Dialog — so it feels like onboarding, not a popup.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(KraftSpacing.Spacing24),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { page ->
                when (page) {
                    0 -> WelcomePage(
                        icon = Icons.Outlined.Explore,
                        title = stringResource(R.string.welcome_title_browse),
                        body = stringResource(R.string.welcome_body_browse),
                    )
                    1 -> WelcomePage(
                        icon = Icons.Outlined.Collections,
                        title = stringResource(R.string.welcome_title_organize),
                        body = stringResource(R.string.welcome_body_organize),
                    )
                    else -> WelcomePage(
                        icon = Icons.Outlined.Lock,
                        title = stringResource(R.string.welcome_title_private),
                        body = stringResource(R.string.welcome_body_private),
                    )
                }
            }
            val pageIndicatorDesc = stringResource(
                R.string.welcome_page_indicator,
                pagerState.currentPage + 1,
                3,
            )
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = KraftSpacing.Spacing16)
                    .semantics {
                        contentDescription = pageIndicatorDesc
                    },
            ) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = if (pagerState.currentPage == i) 20.dp else 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == i) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                    )
                }
            }
            // Fixed button area so Get Started doesn't jump — Next turns into Get Started in place.
            // Both buttons same reasonable size (260x44, not fillMaxWidth stretched), centered.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().height(88.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = {
                            if (pagerState.currentPage < 2) scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            else onDone()
                        },
                        modifier = Modifier.width(260.dp).height(44.dp),
                        shape = RoundedCornerShape(KraftRadius.Standard),
                    ) { Text(if (pagerState.currentPage < 2) stringResource(R.string.next) else stringResource(R.string.get_started)) }
                    Spacer(Modifier.height(KraftSpacing.Spacing8))
                    // Keep Skip space on last page invisible so button doesn't jump
                    if (pagerState.currentPage < 2) {
                        TextButton(
                            onClick = onDone,
                            modifier = Modifier.width(260.dp),
                        ) { Text(stringResource(R.string.skip)) }
                    } else {
                        Spacer(Modifier.height(36.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomePage(icon: ImageVector, title: String, body: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(horizontal = KraftSpacing.Spacing16),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(KraftIconSize.XLarge))
        }
        Spacer(Modifier.height(KraftSpacing.Spacing24))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(KraftSpacing.Spacing12))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
