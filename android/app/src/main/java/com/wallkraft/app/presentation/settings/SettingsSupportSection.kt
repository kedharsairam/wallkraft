package com.wallkraft.app.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftSpacing

/**
 * Support section — Buy Me a Coffee.
 *
 * Split from the former SettingsScreen god object.
 */
@Composable
fun SettingsSupportSection() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
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
}
