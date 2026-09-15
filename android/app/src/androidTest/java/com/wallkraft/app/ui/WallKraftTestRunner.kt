package com.wallkraft.app.ui

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Replacement for HiltTestRunner (removed in Hilt 2.56).
 *
 * HiltTestRunner's only job was to swap the application under test to
 * HiltTestApplication. We do the same by overriding newApplication().
 * Declared as testInstrumentationRunner in build.gradle.kts.
 */
class WallKraftTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader,
        className: String,
        context: Context,
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
