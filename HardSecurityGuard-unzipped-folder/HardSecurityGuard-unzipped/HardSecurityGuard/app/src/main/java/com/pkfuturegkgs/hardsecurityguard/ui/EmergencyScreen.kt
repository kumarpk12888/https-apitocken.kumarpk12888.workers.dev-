package com.pkfuturegkgs.hardsecurityguard.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pkfuturegkgs.hardsecurityguard.ui.theme.HsgAccentRed

/**
 * Quick shortcuts into the Android system security settings a worried
 * user most often needs in a hurry. This screen does not and cannot lock
 * the device, wipe data, or perform any remote action — it only opens
 * standard system settings screens.
 */
@Composable
fun EmergencyScreen() {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Emergency Security Screen", style = MaterialTheme.typography.headlineMedium, color = HsgAccentRed)
        Spacer(Modifier.height(8.dp))
        Text(
            "These shortcuts open standard Android settings screens. This app cannot remotely lock, " +
                "wipe, or take any action on your device — you stay in control.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(20.dp))

        val shortcuts = listOf(
            "Security Settings" to Settings.ACTION_SECURITY_SETTINGS,
            "Accessibility Settings" to Settings.ACTION_ACCESSIBILITY_SETTINGS,
            "Developer Options" to Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
            "Device Admin Apps" to "android.settings.SET_DEVICE_ADMIN_APPS",
            "App List / Permissions" to Settings.ACTION_APPLICATION_SETTINGS,
            "Wi-Fi Settings" to Settings.ACTION_WIFI_SETTINGS,
            "VPN Settings" to Settings.ACTION_VPN_SETTINGS
        )

        shortcuts.forEach { (label, action) ->
            Button(
                onClick = { runCatching { context.startActivity(Intent(action)) } },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) { Text(label) }
        }
    }
}
