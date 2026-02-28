package com.familytracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.familytracker.data.SupabaseClient

class FamilyTrackerApp : Application() {
    
    companion object {
        const val CHANNEL_ID = "location_service_channel"
        const val CHANNEL_NAME = "Location Tracking"
        
        lateinit var instance: FamilyTrackerApp
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        createNotificationChannel()
        SupabaseClient.initialize()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when location tracking is active"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
