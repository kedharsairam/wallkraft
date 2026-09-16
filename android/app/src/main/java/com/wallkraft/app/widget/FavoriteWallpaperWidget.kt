/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Kedhar Sairam
 */
package com.wallkraft.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.action.actionStartActivity
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.core.design.KraftTypeScale
import com.wallkraft.app.R
import com.wallkraft.app.data.db.FavoriteEntity
import com.wallkraft.app.data.db.WallKraftDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.random.Random

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    fun database(): WallKraftDatabase
}

class FavoriteWallpaperWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        )
        val database = entryPoint.database()
        val favoritesDao = database.favoriteDao()

        val favorites = withContext(Dispatchers.IO) {
            favoritesDao.observeAll().first()
        }

        val randomFavorite = if (favorites.isNotEmpty()) {
            favorites[Random.nextInt(favorites.size)]
        } else {
            null
        }

        provideContent {
            WidgetContent(context, randomFavorite)
        }
    }
}

@Composable
private fun WidgetContent(
    context: Context,
    favorite: FavoriteEntity?,
) {
    val intent = Intent(context, com.wallkraft.app.MainActivity::class.java).apply {
        putExtra("destination", "favorites")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(KraftRadius.Medium)
            .background(ColorProvider(KraftColors.WidgetBackground))
            .clickable(actionStartActivity(intent)),
        contentAlignment = Alignment.Center,
    ) {
        if (favorite != null) {
            FavoriteContent(context, favorite)
        } else {
            EmptyContent()
        }
    }
}

@Composable
private fun FavoriteContent(
    context: Context,
    favorite: FavoriteEntity,
) {
    val resolution = "${favorite.dimensionX}x${favorite.dimensionY}"

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(KraftSpacing.Spacing12),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = resolution,
                style = TextStyle(
                    color = ColorProvider(KraftColors.TextPrimary),
                    fontSize = KraftTypeScale.WidgetBody,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = favorite.category,
                style = TextStyle(
                    color = ColorProvider(KraftColors.TextPrimary),
                    fontSize = KraftTypeScale.Caption1,
                ),
            )
        }
    }
}

@Composable
private fun EmptyContent() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(KraftSpacing.Spacing16),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "WallKraft",
            style = TextStyle(
                color = ColorProvider(KraftColors.TextPrimary),
                fontSize = KraftTypeScale.Title3,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = "Add favorites to use widget",
            style = TextStyle(
                color = ColorProvider(KraftColors.TextPrimary),
                fontSize = KraftTypeScale.WidgetBody,
            ),
            modifier = GlanceModifier.padding(top = KraftSpacing.Spacing8),
        )
    }
}
