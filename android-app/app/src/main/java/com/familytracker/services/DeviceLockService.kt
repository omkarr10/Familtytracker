package com.familytracker.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.familytracker.DeviceLockActivity
import com.familytracker.FamilyTrackerApp
import com.familytracker.R
import kotlinx.coroutines.*

/**
 * Service that maintains device lock state and ensures lock screen stays visible.
 * Runs as a foreground service to survive process termination.
 */
class DeviceLockService : Service() {
    
    companion object {
        private const val TAG = "DeviceLockService"
        private const val NOTIFICATION_ID = 1008
        
        private var isRunning = false
        
        fun startLock(context: Context) {
            if (isRunning) return
            
            val intent = Intent(context, DeviceLockService::class.java)
            intent.action = "START_LOCK"
            try {
                context.startForegroundService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service", e)
                context.startService(intent)
            }
        }
        
        fun stopLock(context: Context) {
            val intent = Intent(context, DeviceLockService::class.java)
            intent.action = "STOP_LOCK"
            context.startService(intent)
        }
        
        fun isLockActive(): Boolean = isRunning
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private var monitorJob: Job? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DeviceLockService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_LOCK" -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startLockMode()
            }
            "STOP_LOCK" -> {
                stopLockMode()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopLockMode()
        serviceScope.cancel()
    }
    
    private fun startLockMode() {
        isRunning = true
        Log.d(TAG, "Starting lock mode")
        
        // Acquire wake lock to keep device awake
        acquireWakeLock()
        
        // Launch lock activity
        DeviceLockActivity.lockDevice(this)
        
        // Start monitoring to keep lock screen on top
        startMonitoring()
    }
    
    private fun stopLockMode() {
        isRunning = false
        Log.d(TAG, "Stopping lock mode")
        
        // Stop monitoring
        monitorJob?.cancel()
        monitorJob = null
        
        // Release wake lock
        releaseWakeLock()
        
        // Unlock the device
        DeviceLockActivity.unlockDevice(this)
    }
    
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "FamilyTracker:DeviceLock"
            ).apply {
                acquire(24 * 60 * 60 * 1000L) // 24 hours max
            }
            Log.d(TAG, "Wake lock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }
    }
    
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "Wake lock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release wake lock", e)
        }
    }
    
    private fun startMonitoring() {
        monitorJob = serviceScope.launch {
            while (isRunning) {
                // Periodically ensure lock screen is visible
                if (DeviceLockActivity.isDeviceLocked()) {
                    DeviceLockActivity.lockDevice(this@DeviceLockService)
                }
                delay(2000) // Check every 2 seconds
            }
        }
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, DeviceLockActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, FamilyTrackerApp.CHANNEL_ID)
            .setContentTitle("Device Locked")
            .setContentText("Device is remotely locked")
            .setSmallIcon(R.drawable.ic_location)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Restart if task is removed while locked
        if (isRunning) {
            startLock(this)
            DeviceLockActivity.lockDevice(this)
        }
    }
}
