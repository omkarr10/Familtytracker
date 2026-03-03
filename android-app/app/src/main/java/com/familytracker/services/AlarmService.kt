package com.familytracker.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.familytracker.FamilyTrackerApp
import com.familytracker.MainActivity
import com.familytracker.R

class AlarmService : Service() {
    
    companion object {
        private const val TAG = "AlarmService"
        private const val NOTIFICATION_ID = 1004
        private const val ALARM_DURATION_MS = 60_000L  // 1 minute
        
        private var instance: AlarmService? = null
        
        fun startAlarm(context: Context) {
            val intent = Intent(context, AlarmService::class.java)
            intent.action = "START_ALARM"
            context.startForegroundService(intent)
        }
        
        fun stopAlarm(context: Context) {
            val intent = Intent(context, AlarmService::class.java)
            intent.action = "STOP_ALARM"
            context.startService(intent)
        }
        
        fun isPlaying(): Boolean = instance?.isAlarmPlaying ?: false
    }
    
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var originalVolume: Int = 0
    private var isAlarmPlaying = false
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_ALARM" -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startAlarmSound()
            }
            "STOP_ALARM" -> {
                stopAlarmSound()
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSound()
        instance = null
    }
    
    private fun startAlarmSound() {
        if (isAlarmPlaying) return
        isAlarmPlaying = true
        
        Log.w(TAG, "🚨 STARTING LOUD ALARM!")
        
        try {
            // Save current volume and set to max
            audioManager?.let { am ->
                originalVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
                
                // Also try to set ringer mode to normal
                try {
                    am.ringerMode = AudioManager.RINGER_MODE_NORMAL
                } catch (e: Exception) {
                    Log.w(TAG, "Could not change ringer mode")
                }
            }
            
            // Play alarm sound
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmService, alarmUri)
                isLooping = true
                prepare()
                start()
            }
            
            // Start vibration pattern
            val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, 0)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
            
            // Auto-stop after duration
            handler.postDelayed({
                stopAlarmSound()
                stopSelf()
            }, ALARM_DURATION_MS)
            
            Log.d(TAG, "Alarm started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm", e)
            isAlarmPlaying = false
        }
    }
    
    private fun stopAlarmSound() {
        if (!isAlarmPlaying) return
        isAlarmPlaying = false
        
        Log.d(TAG, "Stopping alarm")
        
        handler.removeCallbacksAndMessages(null)
        
        // Stop media player
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping media player", e)
            }
        }
        mediaPlayer = null
        
        // Stop vibration
        vibrator?.cancel()
        
        // Restore original volume
        audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
    }
    
    private fun createNotification(): Notification {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = "STOP_ALARM"
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, FamilyTrackerApp.CHANNEL_ID)
            .setContentTitle("🚨 Security Alarm")
            .setContentText("Tap to stop alarm")
            .setSmallIcon(R.drawable.ic_location)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(R.drawable.ic_location, "Stop Alarm", stopPendingIntent)
            .build()
    }
}
