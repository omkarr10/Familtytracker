package com.familytracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import com.familytracker.data.PreferencesManager
import com.familytracker.data.SupabaseClient
import com.familytracker.services.LocationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BatteryReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BatteryReceiver"
        private const val LOW_BATTERY_THRESHOLD = 15
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BATTERY_LOW -> {
                Log.d(TAG, "Battery low detected")
                handleLowBattery(context)
            }
            Intent.ACTION_BATTERY_CHANGED -> {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val batteryPct = (level * 100 / scale.toFloat()).toInt()
                
                updateBatteryLevel(context, batteryPct)
                
                if (batteryPct <= LOW_BATTERY_THRESHOLD) {
                    handleLowBattery(context)
                }
            }
        }
    }
    
    private fun handleLowBattery(context: Context) {
        // Trigger burst mode for low battery
        if (LocationService.isRunning) {
            LocationService.triggerBurstMode("low_battery")
        }
        
        // Send low battery alert
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferencesManager = PreferencesManager(context)
                val deviceId = preferencesManager.deviceId.first() ?: return@launch
                
                SupabaseClient.insertAlert(
                    deviceId = deviceId,
                    alertType = "low_battery",
                    message = "Battery is critically low!"
                )
                
                Log.d(TAG, "Low battery alert sent")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send low battery alert", e)
            }
        }
    }
    
    private fun updateBatteryLevel(context: Context, level: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferencesManager = PreferencesManager(context)
                val deviceId = preferencesManager.deviceId.first() ?: return@launch
                
                SupabaseClient.updateDeviceStatus(
                    deviceId = deviceId,
                    batteryLevel = level
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update battery level", e)
            }
        }
    }
}
