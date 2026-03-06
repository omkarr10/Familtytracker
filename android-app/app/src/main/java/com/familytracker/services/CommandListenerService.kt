package com.familytracker.services

import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.familytracker.data.PreferencesManager
import com.familytracker.data.SupabaseClient
import com.familytracker.data.TheftDetectionManager
import com.familytracker.receivers.DeviceAdminReceiver
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Listens for remote commands from the dashboard:
 * - lock: Lock the device
 * - alarm: Trigger loud alarm
 * - capture: Take photos
 * - locate: Send immediate location
 * - wipe: Factory reset (if device admin)
 */
class CommandListenerService : Service() {
    
    companion object {
        private const val TAG = "CommandListenerService"
        
        fun start(context: Context) {
            val intent = Intent(context, CommandListenerService::class.java)
            context.startService(intent)
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, CommandListenerService::class.java)
            context.stopService(intent)
        }
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
        Log.d(TAG, "Command listener service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            startListeningForCommands()
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Command listener service destroyed")
    }
    
    private suspend fun startListeningForCommands() {
        val deviceId = preferencesManager.deviceId.first()
        if (deviceId == null) {
            Log.e(TAG, "Device ID is null, stopping listener")
            return
        }
        
        Log.d(TAG, "Starting command listener for device: $deviceId")
        try {
            // Poll for commands every 30 seconds (realtime requires more setup)
            while (true) {
                try {
                    checkForCommands(deviceId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking commands", e)
                }
                delay(30_000)  // Check every 30 seconds
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Command listener cancelled")
        }
    }
    
    private suspend fun checkForCommands(deviceId: String) {
        Log.d(TAG, "Checking for pending commands...")
        try {
            val commands = SupabaseClient.getPendingCommands(deviceId)
            Log.d(TAG, "Found ${commands.size} pending commands")
            
            for (command in commands) {
                Log.d(TAG, "Executing command: ${command.command}")
                executeCommand(command.command, command.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check commands", e)
        }
    }
    
    private suspend fun executeCommand(command: String, commandId: String) {
        try {
            when (command) {
                "lock" -> lockDevice()
                "alarm" -> triggerAlarm()
                "capture" -> capturePhotos()
                "locate" -> sendImmediateLocation()
                "wipe" -> wipeDevice()
                "activate_theft_mode" -> activateTheftMode()
                "deactivate_theft_mode" -> deactivateTheftMode()
                "stop_alarm" -> stopAlarm()
            }
            
            // Mark command as executed
            SupabaseClient.markCommandExecuted(commandId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute command: $command", e)
        }
    }
    
    private fun lockDevice() {
        Log.d(TAG, "Locking device...")
        
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, DeviceAdminReceiver::class.java)
        
        if (devicePolicyManager.isAdminActive(componentName)) {
            devicePolicyManager.lockNow()
            Log.d(TAG, "Device locked successfully")
        } else {
            Log.e(TAG, "Device admin not active, cannot lock")
        }
    }
    
    private fun triggerAlarm() {
        Log.d(TAG, "Triggering alarm...")
        AlarmService.startAlarm(this)
    }
    
    private fun stopAlarm() {
        Log.d(TAG, "Stopping alarm...")
        AlarmService.stopAlarm(this)
    }
    
    private fun capturePhotos() {
        Log.d(TAG, "Capturing photos...")
        CameraCaptureService.captureTheftPhotos(this)
    }
    
    private fun sendImmediateLocation() {
        Log.d(TAG, "Sending immediate location...")
        LocationService.triggerBurstMode("remote_locate")
    }
    
    private fun wipeDevice() {
        Log.w(TAG, "WIPING DEVICE...")
        
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, DeviceAdminReceiver::class.java)
        
        if (devicePolicyManager.isAdminActive(componentName)) {
            // This will factory reset the device!
            devicePolicyManager.wipeData(0)
        } else {
            Log.e(TAG, "Device admin not active, cannot wipe")
        }
    }
    
    private fun activateTheftMode() {
        Log.d(TAG, "Activating theft mode...")
        TheftDetectionManager.activateTheftMode()
        
        // Start burst mode
        LocationService.triggerBurstMode("theft_mode")
        
        // Capture photos
        CameraCaptureService.captureTheftPhotos(this)
    }
    
    private fun deactivateTheftMode() {
        Log.d(TAG, "Deactivating theft mode...")
        TheftDetectionManager.deactivateTheftMode()
    }
}
