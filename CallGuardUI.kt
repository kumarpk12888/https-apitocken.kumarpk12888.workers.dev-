package com.pkfuturegkgs.hardsecurityguard.ui

import android.app.role.RoleManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pkfuturegkgs.hardsecurityguard.security.BlockedNumbersStore
import com.pkfuturegkgs.hardsecurityguard.security.CallGuardEntry
import com.pkfuturegkgs.hardsecurityguard.security.CallHistoryStore
import com.pkfuturegkgs.hardsecurityguard.security.IntruderSelfie
import com.pkfuturegkgs.hardsecurityguard.security.IntruderSelfieStore
import com.pkfuturegkgs.hardsecurityguard.security.PinManager
import com.pkfuturegkgs.hardsecurityguard.security.RemoteAccessAppScanner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================
// CALL GUARD SCREEN
// ============================================================

@Composable
fun CallGuardScreen() {
    val context = LocalContext.current
    var newNumber by remember { mutableStateOf("") }
    var blocked by remember { mutableStateOf(BlockedNumbersStore.getAll(context)) }
    var history by remember { mutableStateOf(CallHistoryStore.getAll(context)) }
    var roleGranted by remember { mutableStateOf(isCallScreeningGranted(context)) }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { roleGranted = isCallScreeningGranted(context) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Call Guard", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Blocks calls from numbers you add below, and logs missed calls " +
                "with a best-effort country guess from the dialing code. " +
                "This app cannot see a caller's real location.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(16.dp))

        if (!roleGranted) {
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                    roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
                }
            }) { Text("Enable call blocking") }
            Spacer(Modifier.height(16.dp))
        } else {
            Text("Call blocking is active.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newNumber,
                onValueChange = { newNumber = it },
                label = { Text("Number to block") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (newNumber.isNotBlank()) {
                    BlockedNumbersStore.add(context, newNumber)
                    blocked = BlockedNumbersStore.getAll(context)
                    newNumber = ""
                }
            }) { Icon(Icons.Filled.Block, contentDescription = "Block") }
        }

        Spacer(Modifier.height(16.dp))
        Text("Blocked numbers", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(blocked.toList()) { number ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(number)
                    IconButton(onClick = {
                        BlockedNumbersStore.remove(context, number)
                        blocked = BlockedNumbersStore.getAll(context)
                    }) { Icon(Icons.Filled.Delete, contentDescription = "Remove") }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Recent activity", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(history) { entry -> CallHistoryRow(entry) }
        }
    }
}

@Composable
private fun CallHistoryRow(entry: CallGuardEntry) {
    val formatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (entry.type == "BLOCKED") Icons.Filled.Block else Icons.Filled.PhoneMissed,
            contentDescription = entry.type
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(entry.number, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${entry.type} \u00b7 ${entry.country} \u00b7 ${formatter.format(Date(entry.timestamp))}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun isCallScreeningGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
    return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
}

// ============================================================
// INTRUDER LOG SCREEN
// ============================================================

@Composable
fun IntruderLogScreen() {
    val context = LocalContext.current
    var selfies by remember { mutableStateOf(IntruderSelfieStore.getAll(context)) }
    val formatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Intruder Log", style = MaterialTheme.typography.headlineSmall)
            if (selfies.isNotEmpty()) {
                TextButton(onClick = {
                    IntruderSelfieStore.deleteAll(context)
                    selfies = IntruderSelfieStore.getAll(context)
                }) { Text("Clear all") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Photos taken automatically on a failed unlock attempt. " +
                "Stored only on this device — never uploaded anywhere.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(16.dp))

        if (selfies.isEmpty()) {
            Text("No intruder photos yet.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2)) {
                gridItems(selfies) { selfie ->
                    IntruderPhotoTile(selfie, formatter) {
                        IntruderSelfieStore.delete(selfie)
                        selfies = IntruderSelfieStore.getAll(context)
                    }
                }
            }
        }
    }
}

@Composable
private fun IntruderPhotoTile(selfie: IntruderSelfie, formatter: SimpleDateFormat, onDelete: () -> Unit) {
    Column(modifier = Modifier.padding(4.dp)) {
        val bitmap = remember(selfie.file.path) { BitmapFactory.decodeFile(selfie.file.path) }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Intruder photo",
                modifier = Modifier.fillMaxWidth().height(160.dp),
                contentScale = ContentScale.Crop
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatter.format(Date(selfie.timestamp)), style = MaterialTheme.typography.bodySmall)
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
        }
    }
}

// ============================================================
// PIN LOCK SCREEN
// ============================================================

@Composable
fun PinLockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Enter PIN", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 6) pin = it.filter(Char::isDigit); error = null },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = error != null
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            if (PinManager.checkPin(context, pin)) onUnlocked()
            else { error = "Wrong PIN"; pin = "" }
        }) { Text("Unlock") }
    }
}

// ============================================================
// PIN SETUP SCREEN
// ============================================================

@Composable
fun PinSetupScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Set a PIN", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "This PIN will be needed to open the app. A wrong attempt " +
                "triggers an intruder selfie if you've enabled that.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 6) pin = it.filter(Char::isDigit) },
            label = { Text("New PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = confirmPin,
            onValueChange = { if (it.length <= 6) confirmPin = it.filter(Char::isDigit) },
            label = { Text("Confirm PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = error != null
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            when {
                pin.length < 4 -> error = "PIN must be at least 4 digits"
                pin != confirmPin -> error = "PINs don't match"
                else -> { PinManager.setPin(context, pin); onDone() }
            }
        }) { Text("Save PIN") }
    }
}

// ============================================================
// REMOTE-ACCESS WARNING CARD (drop into DashboardScreen)
// ============================================================

@Composable
fun RemoteAccessWarningCard() {
    val context = LocalContext.current
    var found by remember { mutableStateOf(RemoteAccessAppScanner.scan(context)) }
    if (found.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Filled.Warning, contentDescription = "Warning")
            Text("Remote-access app detected", style = MaterialTheme.typography.titleMedium)
            Text(
                "If you didn't install this yourself, someone on a call may " +
                    "be trying to get you to share your screen. Never share " +
                    "your screen or OTPs with anyone who calls you.",
                style = MaterialTheme.typography.bodySmall
            )
            found.forEach { app -> Text("\u2022 ${app.label}", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
