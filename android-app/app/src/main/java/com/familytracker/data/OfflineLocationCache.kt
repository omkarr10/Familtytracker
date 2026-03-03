package com.familytracker.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class CachedLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val speed: Float?,
    val eventType: String,
    val timestamp: Long
)

@Serializable
data class TheftPhoto(
    val base64Data: String,
    val isFront: Boolean,
    val timestamp: Long
)

object OfflineLocationCache {
    
    private const val LOCATIONS_FILE = "offline_locations.json"
    private const val PHOTOS_FILE = "theft_photos.json"
    private const val MAX_CACHED_LOCATIONS = 1000
    private const val MAX_CACHED_PHOTOS = 20
    
    private val json = Json { ignoreUnknownKeys = true }
    
    suspend fun cacheLocation(context: Context, location: CachedLocation) {
        withContext(Dispatchers.IO) {
            val locations = getCachedLocations(context).toMutableList()
            locations.add(location)
            
            // Keep only last MAX locations
            val trimmed = locations.takeLast(MAX_CACHED_LOCATIONS)
            
            val file = File(context.filesDir, LOCATIONS_FILE)
            file.writeText(json.encodeToString(trimmed))
        }
    }
    
    suspend fun getCachedLocations(context: Context): List<CachedLocation> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, LOCATIONS_FILE)
                if (file.exists()) {
                    json.decodeFromString<List<CachedLocation>>(file.readText())
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    suspend fun clearCachedLocations(context: Context) {
        withContext(Dispatchers.IO) {
            val file = File(context.filesDir, LOCATIONS_FILE)
            file.delete()
        }
    }
    
    suspend fun cachePhoto(context: Context, photo: TheftPhoto) {
        withContext(Dispatchers.IO) {
            val photos = getCachedPhotos(context).toMutableList()
            photos.add(photo)
            
            // Keep only last MAX photos
            val trimmed = photos.takeLast(MAX_CACHED_PHOTOS)
            
            val file = File(context.filesDir, PHOTOS_FILE)
            file.writeText(json.encodeToString(trimmed))
        }
    }
    
    suspend fun getCachedPhotos(context: Context): List<TheftPhoto> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, PHOTOS_FILE)
                if (file.exists()) {
                    json.decodeFromString<List<TheftPhoto>>(file.readText())
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    suspend fun clearCachedPhotos(context: Context) {
        withContext(Dispatchers.IO) {
            val file = File(context.filesDir, PHOTOS_FILE)
            file.delete()
        }
    }
    
    fun getCachedLocationCount(context: Context): Int {
        val file = File(context.filesDir, LOCATIONS_FILE)
        return if (file.exists()) {
            try {
                json.decodeFromString<List<CachedLocation>>(file.readText()).size
            } catch (e: Exception) {
                0
            }
        } else {
            0
        }
    }
}
