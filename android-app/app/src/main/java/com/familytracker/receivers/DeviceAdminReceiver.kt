package com.familytracker.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class DeviceAdminReceiver : DeviceAdminReceiver() {
    
    companion object {
        private const val TAG = "DeviceAdminReceiver"
    }
    
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device admin enabled")
        Toast.makeText(context, "Anti-theft protection enabled", Toast.LENGTH_SHORT).show()
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w(TAG, "Device admin disabled")
        Toast.makeText(context, "Anti-theft protection disabled", Toast.LENGTH_SHORT).show()
    }
    
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Log.w(TAG, "Device admin disable requested")
        return "Disabling will remove anti-theft protection. Are you sure?"
    }
    
    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        Log.w(TAG, "Password failed attempt detected")
        
        // Report wrong PIN attempt for theft detection
        com.familytracker.data.TheftDetectionManager.reportWrongPin()
        
        // If theft mode is active, capture photo
        if (com.familytracker.data.TheftDetectionManager.isTheftModeActive()) {
            com.familytracker.services.CameraCaptureService.captureTheftPhotos(context)
        }
    }
}
