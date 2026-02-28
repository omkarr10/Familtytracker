package com.familytracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.familytracker.data.PreferencesManager
import com.familytracker.services.LocationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Log.d(TAG, "Boot completed, checking if should start service")
            
            CoroutineScope(Dispatchers.IO).launch {
                val preferencesManager = PreferencesManager(context)
                val deviceId = preferencesManager.deviceId.first()
                
                if (deviceId != null) {
                    Log.d(TAG, "Device ID found, starting location service")
                    
                    val serviceIntent = Intent(context, LocationService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } else {
                    Log.d(TAG, "No device ID, not starting service")
                }
            }
        }
    }
}
