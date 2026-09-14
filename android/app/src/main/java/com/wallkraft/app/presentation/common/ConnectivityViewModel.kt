package com.wallkraft.app.presentation.common

import androidx.lifecycle.ViewModel
import com.wallkraft.app.util.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * Shared holder for [ConnectivityObserver.isOnline] so screens can collect
 * connectivity without changing their existing ViewModel constructors
 * (which unit tests exercise via secondary constructors).
 */
@HiltViewModel
class ConnectivityViewModel @Inject constructor(
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {
    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline
}
