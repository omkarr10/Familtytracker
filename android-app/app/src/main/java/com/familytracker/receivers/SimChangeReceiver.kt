package com.familytracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import com.familytracker.data.PreferencesManager
import com.familytracker.data.SupabaseClient
import com.familytracker.data.TheftDetectionManager
import com.familytracker.services.CameraCaptureService
import com.familytracker.services.LocationService
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SimChangeReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SimChangeReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.SIM_STATE_CHANGED") {
            val state = intent.getStringExtra("ss")
            Log.d(TAG, "SIM state changed: $state")
            
            when (state) {
                "ABSENT" -> {
                    // SIM card removed!
                    Log.w(TAG, "🚨 SIM CARD REMOVED!")
                    handleSimRemoved(context)
                }
                "READY" -> {
                    checkSimChange(context)
                }
            }
        }
    }
    
    private fun handleSimRemoved(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Report to theft detection
                TheftDetectionManager.reportSimRemoved()
                
                val preferencesManager = PreferencesManager(context)
                val deviceId = preferencesManager.deviceId.first() ?: return@launch
                val backupPhone = preferencesManager.backupPhone.first()
                
                // Capture photos immediately only if anti-theft mode is active
                if (TheftDetectionManager.isTheftModeActive()) {
                    CameraCaptureService.captureTheftPhotos(context)
                    // Trigger burst location mode
                    if (LocationService.isRunning) {
                        LocationService.triggerBurstMode("sim_removed")
                    }
                } else {
                    Log.w(TAG, "SIM removal anti-theft actions ignored: anti-theft mode is OFF")
                }
                
                // Try to get last location and send SMS before SIM is fully gone
                sendLocationSms(context, backupPhone)
                
                // Send alert to server (may fail if no data)
                try {
                    SupabaseClient.insertAlert(
                        deviceId = deviceId,
                        alertType = "sim_removed",
                        message = "⚠️ SIM CARD HAS BEEN REMOVED!"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send sim_removed alert (no network)", e)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling SIM removal", e)
            }
        }
    }
    
    private fun checkSimChange(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferencesManager = PreferencesManager(context)
                val deviceId = preferencesManager.deviceId.first() ?: return@launch
                val savedSimSerial = preferencesManager.simSerial.first()
                val backupPhone = preferencesManager.backupPhone.first()
                
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                
                try {
                    val currentSimSerial = telephonyManager.simSerialNumber
                    
                    if (savedSimSerial != null && currentSimSerial != savedSimSerial) {
                        Log.w(TAG, "🚨 SIM CARD CHANGED!")
                        
                        // Report to theft detection
                        TheftDetectionManager.reportSimChanged()
                        
                        // Only take anti-theft actions if mode is active
                        if (TheftDetectionManager.isTheftModeActive()) {
                            // Capture photos
                            CameraCaptureService.captureTheftPhotos(context)
                            
                            // Trigger burst mode
                            if (LocationService.isRunning) {
                                LocationService.triggerBurstMode("sim_change")
                            }
                        } else {
                            Log.w(TAG, "SIM change anti-theft actions ignored: anti-theft mode is OFF")
                        }
                        
                        // Send SMS alert
                        sendSimChangeSms(context, backupPhone, currentSimSerial)
                        
                        // Send alert to server
                        SupabaseClient.insertAlert(
                            deviceId = deviceId,
                            alertType = "sim_change",
                            message = "⚠️ SIM card changed! New SIM: ${currentSimSerial?.takeLast(8) ?: "unknown"}"
                        )
                    }
                    
                    // Save current SIM serial
                    if (currentSimSerial != null) {
                        preferencesManager.saveSimSerial(currentSimSerial)
                    }
                    
                } catch (e: SecurityException) {
                    Log.e(TAG, "Permission denied to read SIM serial", e)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error checking SIM change", e)
            }
        }
    }
    
    private fun sendLocationSms(context: Context, backupPhone: String?) {
        if (backupPhone.isNullOrEmpty()) {
            Log.w(TAG, "No backup phone configured for SMS")
            return
        }
        
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val message = """
                        🚨 ALERT: SIM REMOVED!
                        Location: ${location.latitude}, ${location.longitude}
                        Maps: https://maps.google.com/?q=${location.latitude},${location.longitude}
                        Time: ${java.time.LocalDateTime.now()}
                    """.trimIndent()
                    
                    try {
                        val smsManager = SmsManager.getDefault()
                        val parts = smsManager.divideMessage(message)
                        smsManager.sendMultipartTextMessage(backupPhone, null, parts, null, null)
                        Log.d(TAG, "Location SMS sent to $backupPhone")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send SMS", e)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission denied", e)
        }
    }
    
    private fun sendSimChangeSms(context: Context, backupPhone: String?, newSimSerial: String?) {
        if (backupPhone.isNullOrEmpty()) return
        
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                val locationStr = if (location != null) {
                    "Maps: https://maps.google.com/?q=${location.latitude},${location.longitude}"
                } else {
                    "Location: Unknown"
                }
                
                val message = """
                    🚨 ALERT: SIM CHANGED!
                    New SIM: ${newSimSerial?.takeLast(8) ?: "unknown"}
                    $locationStr
                    Time: ${java.time.LocalDateTime.now()}
                """.trimIndent()
                
                try {
                    val smsManager = SmsManager.getDefault()
                    val parts = smsManager.divideMessage(message)
                    smsManager.sendMultipartTextMessage(backupPhone, null, parts, null, null)
                    Log.d(TAG, "SIM change SMS sent to $backupPhone")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send SMS", e)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission denied", e)
        }
    }
}
