package com.wallkraft.app.presentation.detail

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.presentation.components.ZoomableImage
import com.wallkraft.app.domain.model.Wallpaper
import kotlinx.coroutines.delay

/**
 * Immersive fullscreen viewer: the image fills the whole screen, the system
 * bars are hidden (transient swipe to bring them back), and the chrome fades
 * away after a short idle period. Tap the image to toggle the chrome; the
 * back gesture/button exits back to the detail page.
 */
@Composable
fun FullscreenViewer(
    wallpaper: Wallpaper,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onSetWallpaper: () -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val window = (context as? Activity)?.window
    var controlsVisible by remember { mutableStateOf(true) }

    if (window != null) {
        val controller = remember(window) {
            WindowInsetsControllerCompat(window, window.decorView)
        }
        DisposableEffect(controller) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
        }
        // Keep the screen awake while viewing.
        DisposableEffect(window) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        }
    }

    // Auto-fade the chrome after a short idle period.
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(2500)
            controlsVisible = false
        }
    }

    BackHandler(onBack = onExit)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        ZoomableImage(
            model = wallpaper.path,
            contentDescription = wallpaper.resolution,
            modifier = Modifier.fillMaxSize(),
            onTap = { controlsVisible = !controlsVisible },
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(Modifier.fillMaxSize()) {
                // Top chrome: back + resolution over a soft gradient.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                            ),
                        )
                        .statusBarsPadding()
                        .padding(horizontal = KraftSpacing.Spacing8, vertical = KraftSpacing.Spacing8),
                ) {
                    IconButton(
                        onClick = onExit,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White,
                        )
                    }
                    Text(
                        text = wallpaper.resolution,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                }

                // Bottom chrome: the usual actions over a soft gradient.
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                            ),
                        )
                        .navigationBarsPadding()
                        .padding(vertical = KraftSpacing.Spacing16),
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) {
                                stringResource(R.string.remove_from_favorites)
                            } else {
                                stringResource(R.string.add_to_favorites)
                            },
                            tint = if (isFavorite) KraftColors.AccentRed else Color.White,
                        )
                    }
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = stringResource(R.string.download), tint = Color.White)
                    }
                    IconButton(
                        onClick = onSetWallpaper,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Filled.Wallpaper, contentDescription = stringResource(R.string.set_as_wallpaper), tint = Color.White)
                    }
                }
            }
        }
    }
}
