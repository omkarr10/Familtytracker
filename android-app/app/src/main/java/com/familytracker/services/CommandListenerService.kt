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
