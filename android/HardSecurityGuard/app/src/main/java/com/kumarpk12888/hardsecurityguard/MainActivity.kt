package com.kumarpk12888.hardsecurityguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.kumarpk12888.hardsecurityguard.ui.theme.HardSecurityGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HardSecurityGuardTheme(darkTheme = isSystemInDarkTheme()) {
                HardSecurityGuardApp()
            }
        }
    }
}
