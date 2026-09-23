package com.pkfuturegkgs.hardsecurityguard.security.checks

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.pkfuturegkgs.hardsecurityguard.security.SecurityCheckResult
import com.pkfuturegkgs.hardsecurityguard.security.Severity

/**
 * Reports current network transport and whether a VPN is active. This is
 * informational rather than pass/fail in the strict sense — no VPN isn't
 * a "warning" for most users, so it's scored as INFO/LOW rather than
 * penalizing the score heavily. Being on an unencrypted public Wi-Fi
 * network without a VPN is flagged at a slightly higher severity.
 */
class VpnNetworkCheck : SecurityCheck {
    override val id = "vpn_network"

    override suspend fun run(context: Context): SecurityCheckResult {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }

        val hasVpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isMetered = caps?.let { !it.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) } ?: false

        val concern = isWifi && !hasVpn

        return SecurityCheckResult(
            id = id,
            title = "VPN / Network Status",
            passed = !concern,
            severity = if (concern) Severity.LOW else Severity.INFO,
            message = buildString {
                append(if (hasVpn) "VPN is active. " else "No VPN detected. ")
                append(
                    when {
                        isWifi -> "Connected via Wi-Fi."
                        caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Connected via mobile data."
                        else -> "No active network detected."
                    }
                )
                if (isMetered) append(" (metered connection)")
            },
            recommendation = if (concern) {
                "On unfamiliar Wi-Fi networks, consider using a trusted VPN."
            } else null
        )
    }
}
