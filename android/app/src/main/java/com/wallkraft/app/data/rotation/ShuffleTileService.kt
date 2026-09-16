/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Kedhar Sairam
 */
package com.wallkraft.app.data.rotation

import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.app.KeyguardManager
import com.wallkraft.app.R
import com.wallkraft.app.domain.repository.FavoritesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ShuffleTileEntryPoint {
    fun favoritesRepository(): FavoritesRepository
}

class ShuffleTileService : TileService() {

    private var serviceScope: CoroutineScope? = null

    override fun onClick() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        serviceScope = scope

        val tile = qsTile ?: return
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (keyguardManager.isDeviceLocked) {
            unlockAndRun { doShuffle() }
        } else {
            doShuffle()
        }
    }

    private fun doShuffle() {
        val scope = serviceScope ?: return
        // Manual runs bypass schedule OFF check (intentional — tile works even
        // when schedule is off).
        RotationScheduler.rotateNow(applicationContext)

        scope.launch {
            delay(1_000L)
            val tile = qsTile ?: return@launch
            tile.state = Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

    override fun onStartListening() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        serviceScope = scope

        scope.launch {
            val entryPoint = EntryPointAccessors.fromApplication(
                application,
                ShuffleTileEntryPoint::class.java,
            )
            val count = entryPoint.favoritesRepository().observeWallpapers().first().size
            val tile = qsTile ?: return@launch
            if (count == 0) {
                tile.state = Tile.STATE_UNAVAILABLE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    tile.subtitle = getString(R.string.tile_no_favorites)
                }
            } else {
                tile.state = Tile.STATE_ACTIVE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    tile.subtitle = null
                }
            }
            tile.updateTile()
        }
    }

    override fun onStopListening() {
        serviceScope?.cancel()
        serviceScope = null
    }
}
