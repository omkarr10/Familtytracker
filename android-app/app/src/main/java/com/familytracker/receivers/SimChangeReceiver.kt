package com.familytracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.familytracker.data.PreferencesManager
import com.familytracker.data.SupabaseClient
import com.familytracker.services.LocationService
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
            
            if (state == "READY") {
                checkSimChange(context)
            }
        }
    }
    
    private fun checkSimChange(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferencesManager = PreferencesManager(context)
                val deviceId = preferencesManager.deviceId.first() ?: return@launch
                val savedSimSerial = preferencesManager.simSerial.first()
                
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                
                try {
                    val currentSimSerial = telephonyManager.simSerialNumber
                    
                    if (savedSimSerial != null && currentSimSerial != savedSimSerial) {
                        Log.w(TAG, "SIM CARD CHANGED!")
                        
                        // Trigger burst mode
                        if (LocationService.isRunning) {
                            LocationService.triggerBurstMode("sim_change")
                        }
                        
                        // Send alert
                        SupabaseClient.insertAlert(
                            deviceId = deviceId,
                            alertType = "sim_change",
                            message = "SIM card has been changed!"
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
}
