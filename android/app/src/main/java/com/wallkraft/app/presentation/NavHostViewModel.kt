package com.wallkraft.app.presentation

import androidx.lifecycle.ViewModel
import com.wallkraft.app.data.prefs.RotationSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavHostViewModel @Inject constructor(
    val rotationStore: RotationSettingsStore,
) : ViewModel()
