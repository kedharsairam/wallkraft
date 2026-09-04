package com.wallkraft.app.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wallkraft.app.AppContainer
import com.wallkraft.app.BuildConfig
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.core.design.KraftIconSize
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.core.design.KraftTopBar
import com.wallkraft.app.domain.model.Category
import com.wallkraft.app.domain.model.Orientation
import com.wallkraft.app.domain.model.Purity
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.util.displayName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/** Chip colors — dark theme only. */
@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = KraftColors.ChipSelectedContainer,
    selectedLabelColor = KraftColors.ChipSelectedLabel,
)

/** Purity SFW chip colors — dark theme only. */
@Composable
private fun puritySfwChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = KraftColors.PuritySfwContainer,
    selectedLabelColor = KraftColors.PuritySfwLabel,
)

/** Purity sketchy chip colors — dark theme only. */
@Composable
private fun puritySketchyChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = KraftColors.PuritySketchyContainer,
    selectedLabelColor = KraftColors.PuritySketchyLabel,
)

/** Purity NSFW chip colors — dark theme only. */
@Composable
private fun purityNsfwChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = KraftColors.PurityNsfwContainer,
    selectedLabelColor = KraftColors.PurityNsfwLabel,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(container: AppContainer, navBarPadding: Dp = 0.dp) {
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory { initializer { SettingsViewModel(container.settings, container.api) } },
    )
    val settings by viewModel.settings.collectAsState()
    val apiKeyText by viewModel.apiKeyText.collectAsState()
    val isValidating by viewModel.isValidating.collectAsState()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showApiDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var cacheSizeText by remember { mutableStateOf("—") }
    val githubUrl = stringResource(R.string.github_url)

    // Compute cache size — refresh on every ON_START so returning from
    // detail screen (where a download may have happened) shows current size.
    fun refreshCacheSize() {
        scope.launch(Dispatchers.IO) {
            val cacheDir = context.cacheDir
            val searchCache = File(cacheDir, "search_cache")
            val coilCache = File(cacheDir, "coil")
            var total = 0L
            listOf(searchCache, coilCache, File(cacheDir, "image_cache")).forEach { dir ->
                if (dir.exists()) total += dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }
            cacheSizeText = formatBytes(total)
        }
    }
    LaunchedEffect(Unit) { refreshCacheSize() }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                refreshCacheSize()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val cacheClearedMsg = stringResource(R.string.cache_cleared)
    val apiSavedMsg = stringResource(R.string.api_key_saved)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { KraftTopBar(title = stringResource(R.string.settings_title)) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KraftSpacing.Spacing20, vertical = KraftSpacing.Spacing16)
                .padding(bottom = navBarPadding),
            verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing24),
        ) {
            // Default Filters — single collapsible section. Shows a compact
            // summary when collapsed, all filter chips when expanded.
            SettingsGroup(title = stringResource(R.string.browsing_title)) {
                // Pre-resolve display names for summary text
                val catGeneral = stringResource(R.string.category_general)
                val catAnime = stringResource(R.string.category_anime)
                val catPeople = stringResource(R.string.category_people)
                val puritySfw = stringResource(R.string.purity_sfw)
                val puritySketchy = stringResource(R.string.purity_sketchy)
                val purityNsfw = stringResource(R.string.purity_nsfw)
                val sortDateAdded = stringResource(R.string.sorting_date_added)
                val sortHot = stringResource(R.string.sorting_hot)
                val sortRandom = stringResource(R.string.sorting_random)
                val sortViews = stringResource(R.string.sorting_views)
                val sortFavorites = stringResource(R.string.sorting_favorites)
                val orientBoth = stringResource(R.string.orientation_both)
                val orientPortrait = stringResource(R.string.orientation_portrait)
                val orientLandscape = stringResource(R.string.orientation_landscape)
                val allLabel = stringResource(R.string.filter_all)

                var expanded by remember { mutableStateOf(false) }

                // Summary row — tap to expand/collapse
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(KraftRadius.Standard))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            expanded = !expanded
                        }
                        .padding(vertical = KraftSpacing.Spacing8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // Categories summary
                        val catSummary = if (settings.categories.size == Category.entries.size) {
                            allLabel
                        } else {
                            settings.categories.joinToString { cat ->
                                when (cat) {
                                    Category.General -> catGeneral
                                    Category.Anime -> catAnime
                                    Category.People -> catPeople
                                }
                            }
                        }
                        // Purity summary
                        val purSummary = if (settings.purity.size == Purity.entries.size) {
                            allLabel
                        } else {
                            settings.purity.joinToString { p ->
                                when (p) {
                                    Purity.SFW -> puritySfw
                                    Purity.Sketchy -> puritySketchy
                                    Purity.NSFW -> purityNsfw
                                }
                            }
                        }
                        // Sorting + Orientation
                        val sortSummary = when (settings.sorting) {
                            Sorting.DateAdded -> sortDateAdded
                            Sorting.Hot -> sortHot
                            Sorting.Random -> sortRandom
                            Sorting.Views -> sortViews
                            Sorting.Favorites -> sortFavorites
                        }
                        val orientSummary = when (settings.orientation) {
                            Orientation.Both -> orientBoth
                            Orientation.Portrait -> orientPortrait
                            Orientation.Landscape -> orientLandscape
                        }
                        Text(
                            text = "$catSummary • $purSummary • $sortSummary • $orientSummary",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(KraftIconSize.Small),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Expanded filters
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(spring(dampingRatio = 0.7f, stiffness = 400f)) + fadeIn(),
                    exit = shrinkVertically(spring(dampingRatio = 0.7f, stiffness = 400f)) + fadeOut(),
                ) {
                    Column {
                        // Categories
                        FilterSectionLabel(stringResource(R.string.settings_categories))
                        Spacer(Modifier.height(KraftSpacing.Spacing8))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                            verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Category.entries.forEach { category ->
                                val selected = category in settings.categories
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val newSet = if (selected) settings.categories - category else settings.categories + category
                                        viewModel.setCategories(newSet)
                                    },
                                    label = { Text(category.displayName()) },
                                    colors = chipColors(),
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = KraftSpacing.Spacing12),
                            color = MaterialTheme.colorScheme.outline,
                        )
                        // Purity
                        FilterSectionLabel(stringResource(R.string.settings_purity))
                        Spacer(Modifier.height(KraftSpacing.Spacing8))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                            verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            // NSFW shown always — locked when no valid API key.
                            val hasApiKey = settings.apiKeyValid
                            Purity.entries.forEach { purity ->
                                val isNsfwLocked = purity == Purity.NSFW && !hasApiKey
                                val selected = purity in settings.purity && !isNsfwLocked
                                val currentChipColors = when {
                                    purity == Purity.SFW -> puritySfwChipColors()
                                    purity == Purity.Sketchy -> puritySketchyChipColors()
                                    isNsfwLocked -> FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    else -> purityNsfwChipColors()
                                }
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        if (isNsfwLocked) return@FilterChip
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val newSet = if (selected) settings.purity - purity else settings.purity + purity
                                        viewModel.setPurity(newSet)
                                    },
                                    label = { Text(purity.displayName()) },
                                    leadingIcon = if (isNsfwLocked) {
                                        { Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(KraftIconSize.Tiny)) }
                                    } else null,
                                    colors = currentChipColors,
                                    enabled = !isNsfwLocked,
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = KraftSpacing.Spacing12),
                            color = MaterialTheme.colorScheme.outline,
                        )
                        // Sorting
                        FilterSectionLabel(stringResource(R.string.settings_sorting))
                        Spacer(Modifier.height(KraftSpacing.Spacing8))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                            verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Sorting.entries.forEach { sorting ->
                                FilterChip(
                                    selected = settings.sorting == sorting,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.setSorting(sorting)
                                    },
                                    label = { Text(sorting.displayName()) },
                                    colors = chipColors(),
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = KraftSpacing.Spacing12),
                            color = MaterialTheme.colorScheme.outline,
                        )
                        // Orientation
                        FilterSectionLabel(stringResource(R.string.settings_orientation))
                        Spacer(Modifier.height(KraftSpacing.Spacing8))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                            verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Orientation.entries.forEach { orientation ->
                                FilterChip(
                                    selected = settings.orientation == orientation,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.setOrientation(orientation)
                                    },
                                    label = { Text(orientation.displayName()) },
                                    colors = chipColors(),
                                )
                            }
                        }
                    }
                }
            }

            // Data
            SettingsGroup(title = stringResource(R.string.data_title)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.data_saver_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.data_saver_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = KraftSpacing.Spacing2),
                        )
                    }
                    Switch(
                        checked = settings.dataSaverMode,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setDataSaverMode(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = KraftSpacing.Spacing12),
                    color = MaterialTheme.colorScheme.outline,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.cache_title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "${stringResource(R.string.cache_description)} • $cacheSizeText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showClearCacheDialog = true
                    }) {
                        Text(stringResource(R.string.clear_cache))
                    }
                }
            }

            // Support
            SettingsGroup(title = stringResource(R.string.buy_me_a_coffee_title)) {
                Text(
                    text = stringResource(R.string.buy_me_a_coffee_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = KraftSpacing.Spacing12),
                )
                BuyMeACoffeeButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/kedhartech"))
                        context.startActivity(intent)
                    },
                )
            }

            // Advanced
            SettingsGroup(title = stringResource(R.string.advanced_title)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(KraftRadius.Standard))
                        .clickable { showApiDialog = true }
                        .padding(vertical = KraftSpacing.Spacing8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.api_key_title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        val statusText = when {
                            apiKeyText.isBlank() -> stringResource(R.string.api_key_not_set)
                            isValidating -> stringResource(R.string.api_key_verifying)
                            settings.apiKeyValid -> stringResource(R.string.api_key_valid)
                            else -> stringResource(R.string.api_key_invalid)
                        }
                        val statusColor = when {
                            apiKeyText.isBlank() -> MaterialTheme.colorScheme.onSurfaceVariant
                            isValidating -> MaterialTheme.colorScheme.onSurfaceVariant
                            settings.apiKeyValid -> KraftColors.AuroraGreen
                            else -> KraftColors.AuroraRed
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(KraftIconSize.Small),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // About
            SettingsGroup(title = stringResource(R.string.about_title)) {
                // Developer credit
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = KraftSpacing.Spacing12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    coil3.compose.AsyncImage(
                        model = stringResource(R.string.github_avatar_url),
                        contentDescription = stringResource(R.string.about_developer),
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp)),
                    )
                    Spacer(Modifier.width(KraftSpacing.Spacing12))
                    Column {
                        Text(
                            text = stringResource(R.string.about_developer),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(R.string.about_developer_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Text(
                    text = stringResource(R.string.version_format, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = KraftSpacing.Spacing12),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                AboutRow(
                    title = stringResource(R.string.github_title),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                        context.startActivity(intent)
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                AboutRow(
                    title = stringResource(R.string.privacy_title),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showPrivacyDialog = true
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                val licensesUrl = stringResource(R.string.licenses_url)
                AboutRow(
                    title = stringResource(R.string.licenses_title),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(licensesUrl))
                        context.startActivity(intent)
                    },
                )
            }
        }
    }

    if (showApiDialog) {
        ApiKeyDialog(
            initial = apiKeyText,
            onDismiss = { showApiDialog = false },
            onSave = { key ->
                viewModel.setApiKey(key)
                showApiDialog = false
                scope.launch { snackbarHostState.showSnackbar(apiSavedMsg) }
            },
        )
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.cache_title)) },
            text = { Text(stringResource(R.string.cache_description)) },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showClearCacheDialog = false
                    scope.launch(Dispatchers.IO) {
                        File(context.cacheDir, "search_cache").deleteRecursively()
                        File(context.cacheDir, "coil").deleteRecursively()
                        File(context.cacheDir, "image_cache").deleteRecursively()
                        // Update UI on main thread first, then show snackbar.
                        withContext(Dispatchers.Main) {
                            cacheSizeText = formatBytes(0)
                            snackbarHostState.showSnackbar(cacheClearedMsg)
                        }
                    }
                }) {
                    Text(stringResource(R.string.clear_cache))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(stringResource(R.string.privacy_title)) },
            text = {
                Text(
                    text = stringResource(R.string.privacy_content),
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = KraftSpacing.Spacing8, start = KraftSpacing.Spacing4),
        )
        Surface(
            shape = RoundedCornerShape(KraftRadius.Standard),
            // Settings group card
            // Light: #FFFFFF (white on #F2F2F7 page), Dark: #1C1C1E (on #000000 page)
            color = MaterialTheme.colorScheme.surfaceBright,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(KraftSpacing.Spacing16)) { content() }
        }
    }
}

@Composable
private fun AboutRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KraftRadius.Standard))
            .clickable(onClick = onClick)
            .padding(vertical = KraftSpacing.Spacing12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(KraftIconSize.Small),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ApiKeyDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    var visible by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.api_key_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.api_key_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = KraftSpacing.Spacing12),
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 64) text = it },
                    placeholder = { Text(stringResource(R.string.api_key_hint)) },
                    singleLine = true,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.trim()) }) { Text(stringResource(R.string.api_key_dialog_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${(kb * 10).roundToInt() / 10.0} KB"
    val mb = kb / 1024.0
    if (mb < 1024) return "${(mb * 10).roundToInt() / 10.0} MB"
    val gb = mb / 1024.0
    return "${(gb * 10).roundToInt() / 10.0} GB"
}

@Composable
private fun BuyMeACoffeeButton(onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "bmcScale",
    )
    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(150)
            pressed = false
        }
    }
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.bmc_button),
            contentDescription = stringResource(R.string.buy_me_a_coffee_title),
            modifier = Modifier
                .width(182.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(KraftRadius.Standard))
                .clickable {
                    pressed = true
                    onClick()
                },
        )
    }
}

@Composable
private fun FilterSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}


