package com.pkfuturegkgs.hardsecurityguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pkfuturegkgs.hardsecurityguard.ui.HsgNavHost
import com.pkfuturegkgs.hardsecurityguard.ui.theme.HardSecurityGuardTheme
import com.pkfuturegkgs.hardsecurityguard.viewmodel.SecurityViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HardSecurityGuardTheme {
                val viewModel: SecurityViewModel = viewModel()
                HsgNavHost(viewModel = viewModel)
            }
        }
    }
}
