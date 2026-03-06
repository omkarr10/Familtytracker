package com.familytracker.services

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import android.util.Base64
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.familytracker.FamilyTrackerApp
import com.familytracker.MainActivity
import com.familytracker.R
import com.familytracker.data.OfflineLocationCache
import com.familytracker.data.PreferencesManager
import com.familytracker.data.SupabaseClient
import com.familytracker.data.TheftPhoto
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.coroutines.resume
import java.io.ByteArrayOutputStream

class CameraCaptureService : Service() {
    
    companion object {
        private const val TAG = "CameraCaptureService"
        private const val NOTIFICATION_ID = 1003
        
        // Optimized capture settings
        private const val IMAGE_WIDTH = 480  // Reduced from 640 for faster capture
        private const val IMAGE_HEIGHT = 360 // Reduced from 480 for faster capture
        private const val JPEG_QUALITY = 60  // Reduced from 70 for faster processing
        private const val CAPTURE_TIMEOUT = 8000L // Reduced timeout
        private const val CAMERA_SWITCH_DELAY = 300L // Reduced delay between cameras
        
        fun captureTheftPhotos(context: Context, continuous: Boolean = false) {
            val intent = Intent(context, CameraCaptureService::class.java)
            intent.action = "CAPTURE_THEFT_PHOTOS"
            intent.putExtra("continuous", continuous)
            try {
                context.startForegroundService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "startForegroundService failed, falling back to startService", e)
                context.startService(intent)
            }
        }
    }
    
    private var cameraDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null
    private var captureSession: CameraCaptureSession? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var preferencesManager: PreferencesManager
    private var isContinuousCapture = false
    
    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
        startBackgroundThread()
        acquireWakeLock()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        
        when (intent?.action) {
            "CAPTURE_THEFT_PHOTOS" -> {
                // Read continuous flag from intent extra
                isContinuousCapture = intent.getBooleanExtra("continuous", false)
                Log.d(TAG, "Capture requested - continuous: $isContinuousCapture")
                capturePhotos()
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        closeCamera()
        stopBackgroundThread()
        releaseWakeLock()
        serviceScope.cancel()
    }
    
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "FamilyTracker:CameraCapture"
            ).apply {
                acquire(60 * 1000L) // 60 seconds max
            }
            Log.d(TAG, "Wake lock acquired for camera capture")
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
    
    private fun capturePhotos() {
        Log.d(TAG, "Starting theft photo capture...")
        // Only allow capture if anti-theft mode is active
        if (!com.familytracker.data.TheftDetectionManager.isTheftModeActive()) {
            Log.w(TAG, "Photo capture ignored: anti-theft mode is OFF")
            stopSelf()
            return
        }
        serviceScope.launch {
            // Use instance variable for continuous mode (3 photos each camera vs 1)
            val maxAttempts = if (isContinuousCapture) 3 else 1
            Log.d(TAG, "Capturing with maxAttempts: $maxAttempts (continuous: $isContinuousCapture)")
            
            // Capture front camera first (thief's face) - higher priority
            var frontSuccess = false
            repeat(maxAttempts) { attempt ->
                val result = withTimeoutOrNull(CAPTURE_TIMEOUT) { captureFromCamera(true) }
                if (result == true) {
                    Log.d(TAG, "Front camera capture succeeded on attempt ${attempt + 1}")
                    frontSuccess = true
                    return@repeat
                } else {
                    Log.w(TAG, "Front camera capture failed on attempt ${attempt + 1}")
                    closeCamera()
                    delay(CAMERA_SWITCH_DELAY)
                }
            }
            // CRITICAL: close camera before next capture
            closeCamera()
            delay(CAMERA_SWITCH_DELAY)
            
            // Then capture back camera (surroundings)
            var backSuccess = false
            repeat(maxAttempts) { attempt ->
                val result = withTimeoutOrNull(CAPTURE_TIMEOUT) { captureFromCamera(false) }
                if (result == true) {
                    Log.d(TAG, "Back camera capture succeeded on attempt ${attempt + 1}")
                    backSuccess = true
                    return@repeat
                } else {
                    Log.w(TAG, "Back camera capture failed on attempt ${attempt + 1}")
                    closeCamera()
                    delay(CAMERA_SWITCH_DELAY)
                }
            }
            closeCamera()
            
            Log.d(TAG, "Capture complete - Front: $frontSuccess, Back: $backSuccess")
            
            // Stop service after captures
            stopSelf()
        }
    }
    
    private suspend fun captureFromCamera(isFront: Boolean): Boolean = suspendCancellableCoroutine { continuation ->
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Camera permission not granted")
            if (continuation.isActive) continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = getCameraId(cameraManager, isFront)
        
        if (cameraId == null) {
            Log.e(TAG, "No ${if (isFront) "front" else "back"} camera found")
            if (continuation.isActive) continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        
        try {
            openCameraAndCapture(cameraManager, cameraId, isFront) { success ->
                if (continuation.isActive) {
                    continuation.resume(success)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture from ${if (isFront) "front" else "back"} camera", e)
            if (continuation.isActive) continuation.resume(false)
        }
    }
    
    private fun getCameraId(cameraManager: CameraManager, isFront: Boolean): String? {
        val facing = if (isFront) CameraCharacteristics.LENS_FACING_FRONT else CameraCharacteristics.LENS_FACING_BACK
        
        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            if (characteristics.get(CameraCharacteristics.LENS_FACING) == facing) {
                return cameraId
            }
        }
        return null
    }
    
    private fun openCameraAndCapture(cameraManager: CameraManager, cameraId: String, isFront: Boolean, onComplete: (Boolean) -> Unit) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            onComplete(false)
            return
        }
        
        // Setup image reader with optimized settings
        imageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT, ImageFormat.JPEG, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            var success = false
            image?.let {
                try {
                    val buffer = it.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    processAndUploadPhotoAsync(bytes, isFront)
                    success = true
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading image", e)
                } finally {
                    it.close()
                }
            }
            onComplete(success)
        }, backgroundHandler)
        
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                createCaptureSession(isFront, onComplete)
            }
            
            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                cameraDevice = null
                onComplete(false)
            }
            
            override fun onError(camera: CameraDevice, error: Int) {
                Log.e(TAG, "Camera error: $error")
                camera.close()
                cameraDevice = null
                onComplete(false)
            }
        }, backgroundHandler)
    }
    
    private fun createCaptureSession(isFront: Boolean, onComplete: (Boolean) -> Unit) {
        val camera = cameraDevice ?: return onComplete(false)
        val reader = imageReader ?: return onComplete(false)
        
        try {
            camera.createCaptureSession(
                listOf(reader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        takePicture(isFront, onComplete)
                    }
                    
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Capture session configuration failed")
                        onComplete(false)
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create capture session", e)
            onComplete(false)
        }
    }
    
    private fun takePicture(isFront: Boolean, onComplete: (Boolean) -> Unit) {
        val camera = cameraDevice ?: return onComplete(false)
        val reader = imageReader ?: return onComplete(false)
        val session = captureSession ?: return onComplete(false)
        
        try {
            val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(reader.surface)
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            
            session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    Log.d(TAG, "Photo captured requested from ${if (isFront) "front" else "back"} camera")
                    // The ImageReader listener will call onComplete
                }

                override fun onCaptureFailed(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    failure: CaptureFailure
                ) {
                    Log.e(TAG, "Photo capture failed")
                    onComplete(false)
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to take picture", e)
            onComplete(false)
        }
    }
    
    private fun processAndUploadPhotoAsync(bytes: ByteArray, isFront: Boolean) {
        // Fire and forget - don't block camera capture
        serviceScope.launch(Dispatchers.Default) {
            try {
                // Decode with optimized settings
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 1 // Already captured at low res
                    inPreferredConfig = Bitmap.Config.RGB_565 // Faster than ARGB_8888
                }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                
                if (bitmap == null) {
                    Log.e(TAG, "Failed to decode image")
                    return@launch
                }
                
                // Compress with optimized quality
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
                val compressedBytes = outputStream.toByteArray()
                
                // Convert to base64
                val base64 = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
                
                bitmap.recycle()
                
                val deviceId = preferencesManager.deviceId.first()
                
                if (deviceId != null) {
                    // Upload in background
                    uploadPhoto(deviceId, base64, isFront)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process photo", e)
            }
        }
    }
    
    private suspend fun uploadPhoto(deviceId: String, base64: String, isFront: Boolean) {
        try {
            SupabaseClient.insertTheftPhoto(
                deviceId = deviceId,
                photoBase64 = base64,
                isFrontCamera = isFront
            )
            Log.d(TAG, "Theft photo uploaded successfully (${if (isFront) "front" else "back"})")
        } catch (e: Exception) {
            // Cache locally if upload fails
            Log.e(TAG, "Failed to upload photo, caching locally", e)
            OfflineLocationCache.cachePhoto(
                this@CameraCaptureService,
                TheftPhoto(
                    base64Data = base64,
                    isFront = isFront,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
    
    @Deprecated("Use processAndUploadPhotoAsync instead", ReplaceWith("processAndUploadPhotoAsync(bytes, isFront)"))
    private fun processAndUploadPhoto(bytes: ByteArray, isFront: Boolean) {
        processAndUploadPhotoAsync(bytes, isFront)
    }
    
    private fun closeCamera() {
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        imageReader?.close()
        imageReader = null
    }
    
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }
    
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Failed to stop background thread", e)
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
            .setContentTitle("Security Check")
            .setContentText("Verifying device security...")
            .setSmallIcon(R.drawable.ic_location)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
