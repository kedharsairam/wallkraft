package com.wallkraft.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
            GlanceTheme {
                WidgetContent(context, randomFavorite)
            }
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
            .cornerRadius(16.dp)
            .background(ColorProvider(R.color.widget_background))
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
                .padding(12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = resolution,
                style = TextStyle(
                    color = ColorProvider(android.R.color.white),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = favorite.category,
                style = TextStyle(
                    color = ColorProvider(android.R.color.white),
                    fontSize = 12.sp,
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
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "WallKraft",
            style = TextStyle(
                color = ColorProvider(android.R.color.white),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = "Add favorites to use widget",
            style = TextStyle(
                color = ColorProvider(android.R.color.white),
                fontSize = 14.sp,
            ),
            modifier = GlanceModifier.padding(top = 8.dp),
        )
    }
}
