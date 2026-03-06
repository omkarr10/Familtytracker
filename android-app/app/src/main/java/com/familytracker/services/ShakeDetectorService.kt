package com.familytracker.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.familytracker.MainActivity
import com.familytracker.R
import com.familytracker.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class ShakeDetectorService : Service(), SensorEventListener {
    
    companion object {
        private const val TAG = "ShakeDetector"
        private const val CHANNEL_ID = "shake_detector_channel"
        private const val NOTIFICATION_ID = 3001
        
        // Shake detection parameters
        private const val SHAKE_THRESHOLD = 12.0f // m/s²
        private const val SHAKE_COUNT_THRESHOLD = 3 // Number of shakes needed
        private const val SHAKE_RESET_TIME = 2000L // Reset counter after 2 seconds
        private const val COOLDOWN_TIME = 30000L // 30 second cooldown after SOS
        
        @Volatile
        var isRunning = false
            private set
        
        fun start(context: Context) {
            val intent = Intent(context, ShakeDetectorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            context.stopService(Intent(context, ShakeDetectorService::class.java))
        }
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var sensorManager: SensorManager
    private lateinit var preferencesManager: PreferencesManager
    private var accelerometer: Sensor? = null
    
    private var lastShakeTime = 0L
    private var shakeCount = 0
    private var lastSOSTime = 0L
    private var isEnabled = true
    
    // For filtering
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var lastUpdate = 0L
    
    override fun onCreate() {
        super.onCreate()
        
        preferencesManager = PreferencesManager(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            isRunning = true
            Log.d(TAG, "Shake detector started")
        } ?: run {
            Log.e(TAG, "No accelerometer available")
            stopSelf()
        }
        
        // Load preference
        serviceScope.launch {
            preferencesManager.shakeToSOS.collect { enabled ->
                isEnabled = enabled
                Log.d(TAG, "Shake-to-SOS enabled: $enabled")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
        isRunning = false
        Log.d(TAG, "Shake detector stopped")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER || !isEnabled) return
        
        val currentTime = System.currentTimeMillis()
        
        // Throttle updates to ~50ms
        if (currentTime - lastUpdate < 50) return
        
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        
        // Calculate acceleration delta
        val deltaX = x - lastX
        val deltaY = y - lastY
        val deltaZ = z - lastZ
        
        lastX = x
        lastY = y
        lastZ = z
        
        // Skip first reading
        if (lastUpdate == 0L) {
            lastUpdate = currentTime
            return
        }
        
        lastUpdate = currentTime
        
        // Calculate magnitude of acceleration change
        val acceleration = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
        
        if (acceleration > SHAKE_THRESHOLD) {
            // Check if we're in cooldown
            if (currentTime - lastSOSTime < COOLDOWN_TIME) {
                return
            }
            
            // Reset count if too much time passed since last shake
            if (currentTime - lastShakeTime > SHAKE_RESET_TIME) {
                shakeCount = 0
            }
            
            shakeCount++
            lastShakeTime = currentTime
            
            Log.d(TAG, "Shake detected! Count: $shakeCount, Acceleration: $acceleration")
            
            if (shakeCount >= SHAKE_COUNT_THRESHOLD) {
                triggerShakeSOS()
                shakeCount = 0
                lastSOSTime = currentTime
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }
    
    private fun triggerShakeSOS() {
        Log.d(TAG, "🆘 SHAKE SOS TRIGGERED!")
        
        // Vibrate to confirm
        vibrateConfirmation()
        
        // Trigger SOS through LocationService
        if (LocationService.isRunning) {
            LocationService.triggerSOS()
        }
        
        // Also trigger burst mode for rapid location updates
        LocationService.triggerBurstMode("shake_sos")
        
        // Send broadcast for any UI updates
        sendBroadcast(Intent("com.familytracker.SHAKE_SOS_TRIGGERED"))
    }
    
    private fun vibrateConfirmation() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Three short vibrations
            val pattern = longArrayOf(0, 200, 100, 200, 100, 200)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 200, 100, 200, 100, 200), -1)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Shake Detection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors for shake gestures to trigger SOS"
                setShowBadge(false)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Shake-to-SOS Active")
            .setContentText("Shake 3 times quickly to send emergency alert")
            .setSmallIcon(R.drawable.ic_nav_emergency)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
