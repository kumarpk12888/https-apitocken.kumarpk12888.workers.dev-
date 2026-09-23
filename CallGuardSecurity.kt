package com.pkfuturegkgs.hardsecurityguard.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

// ============================================================
// BLOCKED NUMBERS
// ============================================================

object BlockedNumbersStore {
    private const val PREFS = "call_guard_blocklist"
    private const val KEY = "numbers"

    private fun normalize(number: String): String =
        number.filter { it.isDigit() }.takeLast(10)

    fun getAll(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY, emptySet()) ?: emptySet()
    }

    fun isBlocked(context: Context, number: String): Boolean =
        normalize(number) in getAll(context)

    fun add(context: Context, number: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = getAll(context).toMutableSet()
        current.add(normalize(number))
        prefs.edit().putStringSet(KEY, current).apply()
    }

    fun remove(context: Context, number: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = getAll(context).toMutableSet()
        current.remove(normalize(number))
        prefs.edit().putStringSet(KEY, current).apply()
    }
}

// ============================================================
// COUNTRY GUESS FROM DIALING CODE (not real location)
// ============================================================

object CallerCountryLookup {
    private val codes = listOf(
        "1" to "US / Canada", "7" to "Russia / Kazakhstan", "20" to "Egypt",
        "27" to "South Africa", "30" to "Greece", "31" to "Netherlands",
        "32" to "Belgium", "33" to "France", "34" to "Spain", "36" to "Hungary",
        "39" to "Italy", "40" to "Romania", "41" to "Switzerland", "43" to "Austria",
        "44" to "United Kingdom", "45" to "Denmark", "46" to "Sweden", "47" to "Norway",
        "48" to "Poland", "49" to "Germany", "51" to "Peru", "52" to "Mexico",
        "55" to "Brazil", "60" to "Malaysia", "61" to "Australia", "62" to "Indonesia",
        "63" to "Philippines", "64" to "New Zealand", "65" to "Singapore",
        "66" to "Thailand", "81" to "Japan", "82" to "South Korea", "84" to "Vietnam",
        "86" to "China", "90" to "Turkey", "91" to "India", "92" to "Pakistan",
        "93" to "Afghanistan", "94" to "Sri Lanka", "95" to "Myanmar",
        "971" to "UAE", "966" to "Saudi Arabia", "880" to "Bangladesh",
        "977" to "Nepal", "960" to "Maldives"
    ).sortedByDescending { it.first.length }

    fun guess(rawNumber: String): String {
        val digits = rawNumber.filter { it.isDigit() || it == '+' }
        if (!digits.startsWith("+")) return "Unknown (no country code on this number)"
        val stripped = digits.removePrefix("+")
        return codes.firstOrNull { stripped.startsWith(it.first) }?.second ?: "Unknown"
    }
}

// ============================================================
// CALL HISTORY (blocked + missed)
// ============================================================

data class CallGuardEntry(
    val number: String,
    val type: String, // "BLOCKED" or "MISSED"
    val country: String,
    val timestamp: Long
)

object CallHistoryStore {
    private const val PREFS = "call_guard_history"
    private const val KEY = "entries"
    private const val MAX_ENTRIES = 200

    private fun log(context: Context, number: String, type: String) {
        val country = CallerCountryLookup.guess(number)
        val entry = JSONObject().apply {
            put("number", number)
            put("type", type)
            put("country", country)
            put("timestamp", System.currentTimeMillis())
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        arr.put(entry)
        while (arr.length() > MAX_ENTRIES) arr.remove(0)
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun logBlocked(context: Context, number: String) = log(context, number, "BLOCKED")
    fun logMissed(context: Context, number: String) = log(context, number, "MISSED")

    fun getAll(context: Context): List<CallGuardEntry> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            CallGuardEntry(
                number = o.getString("number"),
                type = o.getString("type"),
                country = o.optString("country", "Unknown"),
                timestamp = o.getLong("timestamp")
            )
        }.reversed()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

// ============================================================
// CALL BLOCKING SERVICE (register in manifest)
// ============================================================

class CallBlockerService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val rawNumber = callDetails.handle?.schemeSpecificPart
        val shouldBlock = rawNumber != null &&
            BlockedNumbersStore.isBlocked(applicationContext, rawNumber)

        val response = CallResponse.Builder()
            .setDisallowCall(shouldBlock)
            .setRejectCall(shouldBlock)
            .setSkipNotification(shouldBlock)
            .setSkipCallLog(false)
            .build()

        respondToCall(callDetails, response)

        if (shouldBlock && rawNumber != null) {
            CallHistoryStore.logBlocked(applicationContext, rawNumber)
        }
    }
}

// ============================================================
// MISSED CALL DETECTION (register in manifest)
// ============================================================

class MissedCallReceiver : BroadcastReceiver() {
    companion object {
        private var wasRinging = false
        private var incomingNumber: String? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.PHONE_STATE") return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                wasRinging = true
                incomingNumber = number
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                wasRinging = false
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (wasRinging) {
                    incomingNumber?.let {
                        val appContext = context.applicationContext
                        if (!BlockedNumbersStore.isBlocked(appContext, it)) {
                            CallHistoryStore.logMissed(appContext, it)
                            MissedCallNotifier.notify(appContext, it)
                        }
                    }
                }
                wasRinging = false
                incomingNumber = null
            }
        }
    }
}

object MissedCallNotifier {
    private const val CHANNEL_ID = "call_guard_missed"
    private const val CHANNEL_NAME = "Missed calls"

    fun notify(context: Context, number: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val country = CallerCountryLookup.guess(number)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setContentTitle("Missed call: $number")
            .setContentText("Dialing code suggests: $country")
            .setAutoCancel(true)
            .build()
        manager.notify(number.hashCode(), notification)
    }
}

// ============================================================
// INTRUDER SELFIE (silent front-camera capture on wrong PIN)
// ============================================================

object IntruderSelfieCapture {
    private const val TAG = "IntruderSelfie"

    fun capture(context: Context) {
        val appContext = context.applicationContext
        if (ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Camera permission not granted — skipping intruder selfie")
            return
        }

        val handlerThread = HandlerThread("IntruderSelfieThread").apply { start() }
        val handler = Handler(handlerThread.looper)
        val manager = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val frontCameraId = manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        }
        if (frontCameraId == null) {
            Log.w(TAG, "No front camera found")
            handlerThread.quitSafely()
            return
        }

        val imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                image.close()
                saveToFile(appContext, bytes)
            }
            reader.close()
        }, handler)

        try {
            manager.openCamera(frontCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                    requestBuilder.addTarget(imageReader.surface)
                    camera.createCaptureSession(
                        listOf(imageReader.surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                session.capture(
                                    requestBuilder.build(),
                                    object : CameraCaptureSession.CaptureCallback() {
                                        override fun onCaptureCompleted(
                                            session: CameraCaptureSession,
                                            request: CaptureRequest,
                                            result: TotalCaptureResult
                                        ) {
                                            camera.close()
                                        }
                                    },
                                    handler
                                )
                            }
                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                Log.w(TAG, "Camera session config failed")
                                camera.close()
                            }
                        },
                        handler
                    )
                }
                override fun onDisconnected(camera: CameraDevice) { camera.close() }
                override fun onError(camera: CameraDevice, error: Int) {
                    Log.w(TAG, "Camera error: $error")
                    camera.close()
                }
                override fun onClosed(camera: CameraDevice) { handlerThread.quitSafely() }
            }, handler)
        } catch (e: SecurityException) {
            Log.w(TAG, "Camera permission denied at capture time", e)
            handlerThread.quitSafely()
        }
    }

    private fun saveToFile(context: Context, bytes: ByteArray) {
        val dir = File(context.filesDir, "intruder_selfies").apply { mkdirs() }
        val file = File(dir, "intruder_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { it.write(bytes) }
        Log.i(TAG, "Intruder selfie saved: ${file.name}")
    }
}

data class IntruderSelfie(val file: File, val timestamp: Long)

object IntruderSelfieStore {
    private const val MAX_KEPT = 30

    fun getAll(context: Context): List<IntruderSelfie> {
        val dir = File(context.filesDir, "intruder_selfies")
        if (!dir.exists()) return emptyList()
        val files = dir.listFiles { f -> f.extension == "jpg" } ?: emptyArray()
        return files.sortedByDescending { it.lastModified() }
            .map { IntruderSelfie(it, it.lastModified()) }
    }

    fun delete(selfie: IntruderSelfie) { selfie.file.delete() }

    fun deleteAll(context: Context) {
        File(context.filesDir, "intruder_selfies").listFiles()?.forEach { it.delete() }
    }

    fun trimOldEntries(context: Context) {
        val all = getAll(context)
        if (all.size > MAX_KEPT) all.drop(MAX_KEPT).forEach { it.file.delete() }
    }
}

// ============================================================
// PIN LOCK
// ============================================================

object PinManager {
    private const val PREFS = "app_lock_prefs"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_ENABLED = "lock_enabled"

    private fun prefs(context: Context) = run {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context, PREFS, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun isLockEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)
    fun isPinSet(context: Context): Boolean = prefs(context).contains(KEY_PIN_HASH)

    fun setPin(context: Context, pin: String) {
        prefs(context).edit().putString(KEY_PIN_HASH, hash(pin)).putBoolean(KEY_ENABLED, true).apply()
    }

    fun disableLock(context: Context) {
        prefs(context).edit().putBoolean(KEY_ENABLED, false).apply()
    }

    fun enableLock(context: Context) {
        if (isPinSet(context)) prefs(context).edit().putBoolean(KEY_ENABLED, true).apply()
    }

    fun checkPin(context: Context, entered: String): Boolean {
        val stored = prefs(context).getString(KEY_PIN_HASH, null) ?: return false
        val matches = stored == hash(entered)
        if (!matches) IntruderSelfieCapture.capture(context)
        return matches
    }
}

// ============================================================
// REMOTE-ACCESS APP DETECTOR (screen-share scam warning)
// ============================================================

object RemoteAccessAppScanner {
    private val knownPackages = mapOf(
        "com.anydesk.anydeskandroid" to "AnyDesk",
        "com.teamviewer.quicksupport.market" to "TeamViewer QuickSupport",
        "com.teamviewer.host.market" to "TeamViewer Host",
        "com.teamviewer.teamviewer.market.mobile" to "TeamViewer",
        "com.vysor.vysor" to "Vysor",
        "com.airdroid" to "AirDroid",
        "com.unwiredrevolution.airdroid" to "AirDroid",
        "com.splashtop.remote.pad.v2" to "Splashtop",
        "com.rsupport.rs.activity.remote.ui.langpack" to "RSupport / RemoteCall",
        "com.microsoft.rdc.android" to "Microsoft Remote Desktop"
    )

    data class FoundApp(val packageName: String, val label: String)

    fun scan(context: Context): List<FoundApp> {
        val pm = context.packageManager
        return knownPackages.mapNotNull { (pkg, label) ->
            try {
                pm.getPackageInfo(pkg, 0)
                FoundApp(pkg, label)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }
    }
}
