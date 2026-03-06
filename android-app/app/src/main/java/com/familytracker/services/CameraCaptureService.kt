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
        
        fun captureTheftPhotos(context: Context) {
            val intent = Intent(context, CameraCaptureService::class.java)
            intent.action = "CAPTURE_THEFT_PHOTOS"
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
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
        startBackgroundThread()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        
        when (intent?.action) {
            "CAPTURE_THEFT_PHOTOS" -> {
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
        serviceScope.cancel()
    }
    
    private fun capturePhotos() {
        Log.d(TAG, "Starting theft photo capture...")
        
        serviceScope.launch {
            // Capture front camera first (thief's face)
            withTimeoutOrNull(5000) { captureFromCamera(true) }
            
            // CRITICAL: close camera before next capture
            closeCamera()
            delay(500)
            
            // Then capture back camera (surroundings)
            withTimeoutOrNull(5000) { captureFromCamera(false) }
            
            closeCamera()
            delay(500)
            
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
        
        // Setup image reader
        imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            var success = false
            image?.let {
                try {
                    val buffer = it.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    processAndUploadPhoto(bytes, isFront)
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
    
    private fun processAndUploadPhoto(bytes: ByteArray, isFront: Boolean) {
        serviceScope.launch {
            try {
                // Compress image
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val compressedBytes = outputStream.toByteArray()
                
                // Convert to base64
                val base64 = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
                
                val deviceId = preferencesManager.deviceId.first()
                
                if (deviceId != null) {
                    // Try to upload immediately
                    try {
                        SupabaseClient.insertTheftPhoto(
                            deviceId = deviceId,
                            photoBase64 = base64,
                            isFrontCamera = isFront
                        )
                        Log.d(TAG, "Theft photo uploaded successfully")
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
                
                bitmap.recycle()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process photo", e)
            }
        }
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
