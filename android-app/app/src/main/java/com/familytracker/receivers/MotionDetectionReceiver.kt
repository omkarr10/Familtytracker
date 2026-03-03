package com.familytracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.familytracker.data.TheftDetectionManager
import com.familytracker.services.CameraCaptureService
import com.familytracker.services.LocationService
import kotlin.math.sqrt

/**
 * Detects unusual motion patterns that may indicate theft:
 * - Sudden grab (high acceleration)
 * - Phone picked up when it was stationary
 * - Running motion after being stationary
 */
class MotionDetectionReceiver : BroadcastReceiver(), SensorEventListener {
    
    companion object {
        private const val TAG = "MotionDetectionReceiver"
        
        // Acceleration threshold for "grab" detection (in m/s²)
        private const val GRAB_THRESHOLD = 25.0f
        
        // Time phone must be stationary before motion is suspicious
        private const val STATIONARY_THRESHOLD_MS = 30_000L  // 30 seconds
        
        // Minimum time between motion alerts
        private const val ALERT_COOLDOWN_MS = 60_000L  // 1 minute
        
        private var instance: MotionDetectionReceiver? = null
        private var isMonitoring = false
        
        fun startMonitoring(context: Context) {
            if (isMonitoring) return
            
            instance = MotionDetectionReceiver()
            instance?.startSensorMonitoring(context)
            isMonitoring = true
        }
        
        fun stopMonitoring(context: Context) {
            instance?.stopSensorMonitoring(context)
            instance = null
            isMonitoring = false
        }
    }
    
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastMoveTime = System.currentTimeMillis()
    private var lastAlertTime = 0L
    private var isStationary = false
    private var context: Context? = null
    
    // For smoothing acceleration values
    private val accelerationBuffer = mutableListOf<Float>()
    private val bufferSize = 10
    
    override fun onReceive(context: Context, intent: Intent) {
        // This receiver is primarily used through the static methods
        Log.d(TAG, "Motion detection receiver triggered")
    }
    
    fun startSensorMonitoring(context: Context) {
        this.context = context
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        accelerometer?.let {
            sensorManager?.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            Log.d(TAG, "Motion monitoring started")
        }
    }
    
    fun stopSensorMonitoring(context: Context) {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        accelerometer = null
        this.context = null
        Log.d(TAG, "Motion monitoring stopped")
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        
        // Calculate total acceleration (minus gravity)
        val gravity = 9.81f
        val totalAcceleration = sqrt(x * x + y * y + z * z) - gravity
        
        // Add to buffer for smoothing
        accelerationBuffer.add(totalAcceleration)
        if (accelerationBuffer.size > bufferSize) {
            accelerationBuffer.removeAt(0)
        }
        
        val avgAcceleration = accelerationBuffer.average().toFloat()
        
        val now = System.currentTimeMillis()
        
        // Check if phone is stationary (very low acceleration)
        if (avgAcceleration < 0.5f) {
            if (!isStationary && (now - lastMoveTime) > STATIONARY_THRESHOLD_MS) {
                isStationary = true
                Log.d(TAG, "Phone is now stationary")
            }
        } else {
            lastMoveTime = now
            
            // If was stationary and now suddenly moving fast - suspicious!
            if (isStationary && avgAcceleration > GRAB_THRESHOLD) {
                if (now - lastAlertTime > ALERT_COOLDOWN_MS) {
                    onPossibleGrab()
                    lastAlertTime = now
                }
            }
            
            isStationary = false
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
    
    private fun onPossibleGrab() {
        Log.w(TAG, "⚠️ POSSIBLE GRAB DETECTED!")
        
        // Report to theft detection manager
        TheftDetectionManager.reportUnusualMotion()
        
        // If theft mode is already active, take immediate action
        if (TheftDetectionManager.isTheftModeActive()) {
            context?.let { ctx ->
                // Capture photos immediately
                CameraCaptureService.captureTheftPhotos(ctx)
                
                // Trigger burst location mode
                LocationService.triggerBurstMode("grab_detected")
            }
        }
    }
}
