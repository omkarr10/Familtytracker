package com.familytracker.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.familytracker.FamilyTrackerApp
import com.familytracker.MainActivity
import com.familytracker.R
import com.familytracker.data.PreferencesManager
import com.familytracker.data.SupabaseClient
import com.familytracker.data.TheftDetectionManager
import com.familytracker.receivers.DeviceAdminReceiver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
        // Run as a lightweight foreground service so Android keeps it alive
        startForeground(1005, createNotification())

        // Ensure we only start one listener loop
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
            stopSelf()
            return
        }

        Log.d(TAG, "Starting command listener for device: $deviceId")
        try {
            // Poll for commands frequently so dashboard clicks feel instant
            while (true) {
                try {
                    checkForCommands(deviceId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking commands", e)
                }
                // Short delay keeps commands responsive but avoids tight loop
                delay(5_000)
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
                // Cache parameters before execution
                cacheCommandParameters(command.id, parseParameters(command.parameters))
                executeCommand(command.command, command.id)
                // Clear cached parameters after execution
                clearCommandParameters(command.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check commands", e)
        }
    }
    
    // Parse JsonElement parameters into a Map
    private fun parseParameters(params: kotlinx.serialization.json.JsonElement?): Map<String, Any>? {
        if (params == null) return null
        return try {
            val jsonObject = params as? kotlinx.serialization.json.JsonObject ?: return null
            jsonObject.mapValues { (_, value) ->
                when (value) {
                    is kotlinx.serialization.json.JsonPrimitive -> {
                        when {
                            value.isString -> value.content
                            value.content == "true" -> true
                            value.content == "false" -> false
                            else -> value.content.toIntOrNull() ?: value.content
                        }
                    }
                    else -> value.toString()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse command parameters", e)
            null
        }
    }
    
    private suspend fun executeCommand(command: String, commandId: String) {
        try {
            when (command) {
                "lock" -> {
                    // Complete lock - works even without anti-theft mode for security
                    lockDeviceComplete()
                }
                "unlock" -> {
                    // Unlock the device
                    unlockDevice()
                }
                "alarm" -> {
                    // Gate alarm by anti-theft mode
                    if (TheftDetectionManager.isTheftModeActive()) {
                        triggerAlarm()
                    } else {
                        Log.w(TAG, "Alarm command ignored: anti-theft mode is OFF")
                    }
                }
                "capture" -> {
                    // Only allow capture if anti-theft mode is active
                    if (TheftDetectionManager.isTheftModeActive()) {
                        // Check for continuous capture parameter
                        val continuous = getCommandParameter(commandId, "continuous") == true
                        capturePhotos(continuous)
                    } else {
                        Log.w(TAG, "Capture command ignored: anti-theft mode is OFF")
                    }
                }
                "locate" -> sendImmediateLocation()
                "wipe" -> {
                    // Gate wipe by anti-theft mode
                    if (TheftDetectionManager.isTheftModeActive()) {
                        wipeDevice()
                    } else {
                        Log.w(TAG, "Wipe command ignored: anti-theft mode is OFF")
                    }
                }
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
    
    private fun lockDeviceComplete() {
        Log.d(TAG, "Complete device lock...")
        
        // Start the DeviceLockService which handles complete lock
        DeviceLockService.startLock(this)
        
        // Also use device admin to lock the screen
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, DeviceAdminReceiver::class.java)
        
        if (devicePolicyManager.isAdminActive(componentName)) {
            devicePolicyManager.lockNow()
        }
        
        Log.d(TAG, "Device locked completely")
    }
    
    private fun unlockDevice() {
        Log.d(TAG, "Unlocking device...")
        
        // Stop the DeviceLockService
        DeviceLockService.stopLock(this)
        
        Log.d(TAG, "Device unlocked")
    }
    
    private fun lockDevice() {
        Log.d(TAG, "Locking device (simple)...")
        
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
    
    private fun capturePhotos(continuous: Boolean = false) {
        Log.d(TAG, "Capturing photos...")
        CameraCaptureService.captureTheftPhotos(this, continuous)
    }
    // Cached command parameters from DB
    private var commandParameters: Map<String, Map<String, Any>> = emptyMap()
    
    // Store parameters when fetching commands
    private fun cacheCommandParameters(commandId: String, params: Map<String, Any>?) {
        if (params != null) {
            commandParameters = commandParameters + (commandId to params)
        }
    }
    
    // Helper to get command parameter from cached data
    private fun getCommandParameter(commandId: String, key: String): Boolean {
        return commandParameters[commandId]?.get(key) as? Boolean ?: false
    }
    
    // Clear cached parameters after command execution
    private fun clearCommandParameters(commandId: String) {
        commandParameters = commandParameters - commandId
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

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, FamilyTrackerApp.CHANNEL_ID)
            .setContentTitle("Security Commands Active")
            .setContentText("Listening for remote lock, alarm, and photo commands")
            .setSmallIcon(R.drawable.ic_location)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
