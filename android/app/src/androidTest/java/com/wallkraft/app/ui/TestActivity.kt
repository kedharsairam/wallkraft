package com.wallkraft.app.ui

import androidx.activity.ComponentActivity

/**
 * Minimal activity for Compose instrumented tests.
 *
 * Hilt 2.56 removed HiltTestRunner and HiltTestActivity from the testing
 * artifact. We use plain ComponentActivity here (provides ViewModelStoreOwner
 * for hiltViewModel()) with HiltTestApplication (declared in
 * androidTest/AndroidManifest.xml) providing the Hilt component.
 */
class TestActivity : ComponentActivity()
