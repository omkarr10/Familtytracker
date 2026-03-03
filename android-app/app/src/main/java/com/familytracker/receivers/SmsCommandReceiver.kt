package com.familytracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import com.familytracker.data.PreferencesManager
import com.familytracker.data.TheftDetectionManager
import com.familytracker.services.AlarmService
import com.familytracker.services.CameraCaptureService
import com.familytracker.services.LocationService
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receives SMS commands when internet is not available
 * 
 * Commands (send to this phone):
 * - "LOCATE" - Reply with current location
 * - "ALARM" - Trigger loud alarm
 * - "LOCK" - Lock the device
 * - "CAPTURE" - Take photos
 * 
 * All commands must be prefixed with the secret code from settings
 * Example: "SECRET123 LOCATE"
 */
class SmsCommandReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SmsCommandReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        
        val bundle = intent.extras ?: return
        val pdus = bundle.get("pdus") as? Array<*> ?: return
        val format = bundle.getString("format")
        
        for (pdu in pdus) {
            val smsMessage = SmsMessage.createFromPdu(pdu as ByteArray, format)
            val sender = smsMessage.displayOriginatingAddress
            val body = smsMessage.messageBody?.uppercase()?.trim() ?: continue
            
            Log.d(TAG, "SMS received from $sender: $body")
            
            processCommand(context, sender, body)
        }
    }
    
    private fun processCommand(context: Context, sender: String, body: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferencesManager = PreferencesManager(context)
                val secretCode = preferencesManager.smsSecretCode.first() ?: return@launch
                val authorizedNumbers = preferencesManager.authorizedNumbers.first()
                
                // Check if sender is authorized
                val isAuthorized = authorizedNumbers.any { 
                    sender.contains(it.takeLast(10)) 
                }
                
                if (!isAuthorized) {
                    Log.w(TAG, "Unauthorized SMS sender: $sender")
                    return@launch
                }
                
                // Check if command starts with secret code
                if (!body.startsWith(secretCode.uppercase())) {
                    Log.d(TAG, "SMS does not contain secret code")
                    return@launch
                }
                
                val command = body.removePrefix(secretCode.uppercase()).trim()
                Log.d(TAG, "Processing SMS command: $command")
                
                when {
                    command.contains("LOCATE") -> handleLocateCommand(context, sender)
                    command.contains("ALARM") -> handleAlarmCommand(context, sender)
                    command.contains("STOPALARM") -> handleStopAlarmCommand(context, sender)
                    command.contains("CAPTURE") -> handleCaptureCommand(context, sender)
                    command.contains("THEFT") -> handleTheftModeCommand(context, sender)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS command", e)
            }
        }
    }
    
    private fun handleLocateCommand(context: Context, replyTo: String) {
        Log.d(TAG, "Executing LOCATE command")
        
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val message = """
                        📍 LOCATION ALERT
                        Lat: ${location.latitude}
                        Long: ${location.longitude}
                        Accuracy: ${location.accuracy}m
                        Maps: https://maps.google.com/?q=${location.latitude},${location.longitude}
                    """.trimIndent()
                    
                    sendSms(context, replyTo, message)
                } else {
                    sendSms(context, replyTo, "Could not get location. Trying again...")
                    LocationService.triggerBurstMode("sms_locate")
                }
            }.addOnFailureListener {
                sendSms(context, replyTo, "Failed to get location: ${it.message}")
            }
        } catch (e: SecurityException) {
            sendSms(context, replyTo, "Location permission denied")
        }
    }
    
    private fun handleAlarmCommand(context: Context, replyTo: String) {
        Log.d(TAG, "Executing ALARM command")
        AlarmService.startAlarm(context)
        sendSms(context, replyTo, "🚨 ALARM ACTIVATED!")
    }
    
    private fun handleStopAlarmCommand(context: Context, replyTo: String) {
        Log.d(TAG, "Executing STOP ALARM command")
        AlarmService.stopAlarm(context)
        sendSms(context, replyTo, "Alarm stopped")
    }
    
    private fun handleCaptureCommand(context: Context, replyTo: String) {
        Log.d(TAG, "Executing CAPTURE command")
        CameraCaptureService.captureTheftPhotos(context)
        sendSms(context, replyTo, "📸 Capturing photos... Check dashboard for images.")
    }
    
    private fun handleTheftModeCommand(context: Context, replyTo: String) {
        Log.d(TAG, "Executing THEFT MODE command")
        TheftDetectionManager.activateTheftMode()
        LocationService.triggerBurstMode("theft_mode")
        CameraCaptureService.captureTheftPhotos(context)
        sendSms(context, replyTo, "🚨 THEFT MODE ACTIVATED!\nTracking every 10 seconds\nCapturing photos")
    }
    
    private fun sendSms(context: Context, phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            
            // Split message if too long
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            
            Log.d(TAG, "SMS sent to $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
        }
    }
}
