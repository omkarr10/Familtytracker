package com.familytracker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.util.Log
import com.familytracker.data.OfflineLocationCache
import com.familytracker.data.PreferencesManager
import com.familytracker.data.SupabaseClient
import com.familytracker.data.TheftDetectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ConnectivityReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "ConnectivityReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                val isAirplaneModeOn = Settings.Global.getInt(
                    context.contentResolver,
                    Settings.Global.AIRPLANE_MODE_ON, 0
                ) != 0
                
                if (isAirplaneModeOn) {
                    Log.w(TAG, "Airplane mode turned ON - suspicious!")
                    TheftDetectionManager.reportAirplaneModeOn()
                } else {
                    Log.d(TAG, "Airplane mode turned OFF")
                    // Try to sync cached data
                    syncCachedData(context)
                }
            }
            
            "android.net.conn.CONNECTIVITY_CHANGE" -> {
                if (isNetworkAvailable(context)) {
                    Log.d(TAG, "Network became available - syncing cached data")
                    syncCachedData(context)
                } else {
                    Log.w(TAG, "Network lost - will cache data offline")
                    TheftDetectionManager.reportDataDisabled()
                }
            }
        }
    }
    
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    private fun syncCachedData(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferencesManager = PreferencesManager(context)
                val deviceId = preferencesManager.deviceId.first() ?: return@launch
                
                // Sync cached locations
                val cachedLocations = OfflineLocationCache.getCachedLocations(context)
                if (cachedLocations.isNotEmpty()) {
                    Log.d(TAG, "Syncing ${cachedLocations.size} cached locations")
                    
                    for (location in cachedLocations) {
                        try {
                            SupabaseClient.insertLocation(
                                deviceId = deviceId,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy,
                                speed = location.speed,
                                eventType = location.eventType
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to sync location", e)
                            return@launch  // Stop if we can't upload
                        }
                    }
                    
                    // Clear cached locations after successful sync
                    OfflineLocationCache.clearCachedLocations(context)
                    Log.d(TAG, "Cached locations synced successfully")
                }
                
                // Sync cached photos
                val cachedPhotos = OfflineLocationCache.getCachedPhotos(context)
                if (cachedPhotos.isNotEmpty()) {
                    Log.d(TAG, "Syncing ${cachedPhotos.size} cached photos")
                    
                    for (photo in cachedPhotos) {
                        try {
                            SupabaseClient.insertTheftPhoto(
                                deviceId = deviceId,
                                photoBase64 = photo.base64Data,
                                isFrontCamera = photo.isFront
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to sync photo", e)
                            return@launch
                        }
                    }
                    
                    OfflineLocationCache.clearCachedPhotos(context)
                    Log.d(TAG, "Cached photos synced successfully")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing cached data", e)
            }
        }
    }
}
