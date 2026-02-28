package com.familytracker.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.familytracker.FamilyTrackerApp
import com.familytracker.MainActivity
import com.familytracker.R
import com.familytracker.data.PreferencesManager
import com.familytracker.data.SupabaseClient
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.Instant

class LocationService : Service() {
    
    companion object {
        private const val TAG = "LocationService"
        private const val NOTIFICATION_ID = 1001
        private const val LOCATION_INTERVAL = 3 * 60 * 1000L  // 3 minutes
        private const val BURST_INTERVAL = 10 * 1000L  // 10 seconds
        private const val BURST_DURATION = 2 * 60 * 1000L  // 2 minutes
        
        var isRunning = false
            private set
        
        private var serviceInstance: LocationService? = null
        
        fun triggerSOS() {
            serviceInstance?.sendSOSAlert()
        }
        
        fun triggerBurstMode(eventType: String) {
            serviceInstance?.startBurstMode(eventType)
        }
    }
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var preferencesManager: PreferencesManager
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var deviceId: String? = null
    private var isBurstMode = false
    private var burstEventType = "burst"
    private var burstStartTime = 0L
    
    override fun onCreate() {
        super.onCreate()
        serviceInstance = this
        
        preferencesManager = PreferencesManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        setupLocationCallback()
        
        Log.d(TAG, "Service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        isRunning = true
        
        serviceScope.launch {
            deviceId = preferencesManager.deviceId.first()
            if (deviceId != null) {
                startLocationUpdates()
            } else {
                Log.e(TAG, "No device ID found")
                stopSelf()
            }
        }
        
        return START_STICKY  // Restart if killed
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceInstance = null
        stopLocationUpdates()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }
    
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val eventType = if (isBurstMode) burstEventType else "normal"
                    
                    serviceScope.launch {
                        try {
                            sendLocationToServer(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy,
                                speed = location.speed,
                                eventType = eventType
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to send location", e)
                            // TODO: Queue for later
                        }
                    }
                    
                    // Check if burst mode should end
                    if (isBurstMode && System.currentTimeMillis() - burstStartTime > BURST_DURATION) {
                        stopBurstMode()
                    }
                }
            }
        }
    }
    
    private fun startLocationUpdates() {
        val interval = if (isBurstMode) BURST_INTERVAL else LOCATION_INTERVAL
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            interval
        ).apply {
            setMinUpdateIntervalMillis(interval / 2)
            setWaitForAccurateLocation(false)
        }.build()
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d(TAG, "Location updates started (interval: ${interval}ms)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted", e)
            stopSelf()
        }
    }
    
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d(TAG, "Location updates stopped")
    }
    
    private fun startBurstMode(eventType: String) {
        if (!isBurstMode) {
            isBurstMode = true
            burstEventType = eventType
            burstStartTime = System.currentTimeMillis()
            
            // Restart location updates with faster interval
            stopLocationUpdates()
            startLocationUpdates()
            
            Log.d(TAG, "Burst mode started: $eventType")
        }
    }
    
    private fun stopBurstMode() {
        if (isBurstMode) {
            isBurstMode = false
            
            // Restart with normal interval
            stopLocationUpdates()
            startLocationUpdates()
            
            Log.d(TAG, "Burst mode stopped")
        }
    }
    
    private fun sendSOSAlert() {
        serviceScope.launch {
            try {
                // Get current location
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    serviceScope.launch {
                        if (location != null) {
                            // Send SOS location
                            sendLocationToServer(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy,
                                speed = location.speed,
                                eventType = "sos"
                            )
                            
                            // Send SOS alert
                            sendAlert(
                                alertType = "sos",
                                message = "SOS button pressed!",
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                            
                            // Start burst mode
                            startBurstMode("sos")
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Location permission not granted", e)
            }
        }
    }
    
    private suspend fun sendLocationToServer(
        latitude: Double,
        longitude: Double,
        accuracy: Float?,
        speed: Float?,
        eventType: String
    ) {
        val id = deviceId ?: return
        
        try {
            SupabaseClient.insertLocation(
                deviceId = id,
                latitude = latitude,
                longitude = longitude,
                accuracy = accuracy,
                speed = speed,
                eventType = eventType
            )
            Log.d(TAG, "Location sent: $latitude, $longitude ($eventType)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send location to server", e)
            throw e
        }
    }
    
    private suspend fun sendAlert(
        alertType: String,
        message: String,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        val id = deviceId ?: return
        
        try {
            SupabaseClient.insertAlert(
                deviceId = id,
                alertType = alertType,
                message = message,
                latitude = latitude,
                longitude = longitude
            )
            Log.d(TAG, "Alert sent: $alertType")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send alert", e)
        }
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, FamilyTrackerApp.CHANNEL_ID)
            .setContentTitle("Family Tracker")
            .setContentText("Location tracking is active")
            .setSmallIcon(R.drawable.ic_location)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
