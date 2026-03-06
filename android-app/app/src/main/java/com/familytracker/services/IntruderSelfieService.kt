package com.familytracker.services

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import com.familytracker.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service to capture intruder selfies when wrong PIN is entered.
 * Uses the front camera silently to capture photos.
 */
class IntruderSelfieService(private val context: Context) {
    
    companion object {
        private const val TAG = "IntruderSelfie"
        private const val IMAGE_WIDTH = 640
        private const val IMAGE_HEIGHT = 480
        private const val MAX_RETRY_COUNT = 3
        
        fun captureIntruder(context: Context, reason: String = "wrong_pin") {
            val service = IntruderSelfieService(context)
            service.capturePhoto(reason)
        }
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preferencesManager = PreferencesManager(context)
    
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    
    fun capturePhoto(reason: String) {
        scope.launch {
            val isEnabled = preferencesManager.intruderSelfie.first()
            if (!isEnabled) {
                Log.d(TAG, "Intruder selfie disabled")
                return@launch
            }
            
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Camera permission not granted")
                return@launch
            }
            
            try {
                startBackgroundThread()
                openFrontCamera(reason)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to capture intruder photo", e)
                cleanup()
            }
        }
    }
    
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("IntruderCamera").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }
    
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }
    
    private fun openFrontCamera(reason: String) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        
        // Find front camera
        val frontCameraId = cameraManager.cameraIdList.find { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        }
        
        if (frontCameraId == null) {
            Log.e(TAG, "No front camera found")
            cleanup()
            return
        }
        
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                
                imageReader = ImageReader.newInstance(
                    IMAGE_WIDTH, IMAGE_HEIGHT, ImageFormat.JPEG, 1
                ).apply {
                    setOnImageAvailableListener({ reader ->
                        val image = reader.acquireLatestImage()
                        image?.let {
                            processImage(it, reason)
                            it.close()
                        }
                        cleanup()
                    }, backgroundHandler)
                }
                
                cameraManager.openCamera(frontCameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        createCaptureSession(reason)
                    }
                    
                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                        cleanup()
                    }
                    
                    override fun onError(camera: CameraDevice, error: Int) {
                        Log.e(TAG, "Camera error: $error")
                        camera.close()
                        cleanup()
                    }
                }, backgroundHandler)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Camera permission denied", e)
            cleanup()
        }
    }
    
    @Suppress("DEPRECATION")
    private fun createCaptureSession(reason: String) {
        val camera = cameraDevice ?: return
        val reader = imageReader ?: return
        
        try {
            camera.createCaptureSession(
                listOf(reader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        captureImage()
                    }
                    
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Capture session configuration failed")
                        cleanup()
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create capture session", e)
            cleanup()
        }
    }
    
    private fun captureImage() {
        val camera = cameraDevice ?: return
        val session = captureSession ?: return
        val reader = imageReader ?: return
        
        try {
            val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(reader.surface)
                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                // Disable flash for stealth
                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
            }
            
            session.capture(captureBuilder.build(), null, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture image", e)
            cleanup()
        }
    }
    
    private fun processImage(image: Image, reason: String) {
        scope.launch {
            try {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                
                // Decode and rotate if needed (front camera is usually mirrored)
                var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                
                // Mirror the image horizontally
                val matrix = Matrix().apply { preScale(-1f, 1f) }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
                
                // Compress to JPEG
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val compressedBytes = outputStream.toByteArray()
                
                // Convert to Base64
                val base64Image = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
                
                // Upload to server
                uploadIntruderPhoto(base64Image, reason)
                
                Log.d(TAG, "Intruder photo captured and uploaded")
                
                // Send broadcast
                context.sendBroadcast(Intent("com.familytracker.INTRUDER_CAPTURED").apply {
                    putExtra("reason", reason)
                })
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process image", e)
            }
        }
    }
    
    private suspend fun uploadIntruderPhoto(base64Image: String, reason: String) {
        val deviceId = preferencesManager.deviceId.first() ?: return
        
        try {
            val url = URL("https://family-tracker-api.onrender.com/api/devices/$deviceId/intruder")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 15000
                readTimeout = 15000
            }
            
            val payload = JSONObject().apply {
                put("image", base64Image)
                put("reason", reason)
                put("timestamp", System.currentTimeMillis())
            }
            
            connection.outputStream.bufferedWriter().use { writer ->
                writer.write(payload.toString())
            }
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                Log.d(TAG, "Intruder photo uploaded successfully")
            } else {
                Log.e(TAG, "Failed to upload: HTTP $responseCode")
            }
            
            connection.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
        }
    }
    
    private fun cleanup() {
        try {
            captureSession?.close()
            captureSession = null
            
            cameraDevice?.close()
            cameraDevice = null
            
            imageReader?.close()
            imageReader = null
            
            stopBackgroundThread()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}
