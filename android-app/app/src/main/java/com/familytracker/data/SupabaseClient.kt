package com.familytracker.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.Realtime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

object SupabaseClient {
    
    // Supabase credentials
    private const val SUPABASE_URL = "https://vcdqijuwsqkmvwceizdu.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZjZHFpanV3c3FrbXZ3Y2VpemR1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIyNzY3OTIsImV4cCI6MjA4Nzg1Mjc5Mn0.0ygd1r5cozra1aT3oRNfff2Ei8bsz7hI5_sqzJwhN1M"
    
    private lateinit var client: io.github.jan.supabase.SupabaseClient
    
    fun initialize() {
        client = createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Realtime)
        }
    }
    
    suspend fun insertLocation(
        deviceId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float?,
        speed: Float?,
        eventType: String
    ) {
        client.from("locations").insert(
            buildJsonObject {
                put("device_id", deviceId)
                put("latitude", latitude)
                put("longitude", longitude)
                accuracy?.let { put("accuracy", it) }
                speed?.let { put("speed", it) }
                put("event_type", eventType)
                put("created_at", Instant.now().toString())
            }
        )
    }
    
    suspend fun insertAlert(
        deviceId: String,
        alertType: String,
        message: String,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        client.from("alerts").insert(
            buildJsonObject {
                put("device_id", deviceId)
                put("alert_type", alertType)
                put("message", message)
                latitude?.let { put("latitude", it) }
                longitude?.let { put("longitude", it) }
                put("created_at", Instant.now().toString())
            }
        )
    }
    
    suspend fun updateDeviceStatus(
        deviceId: String,
        batteryLevel: Int? = null,
        isOnline: Boolean = true
    ) {
        client.from("devices").update(
            buildJsonObject {
                batteryLevel?.let { put("battery_level", it) }
                put("is_online", isOnline)
                put("last_seen", Instant.now().toString())
            }
        ) {
            filter {
                eq("id", deviceId)
            }
        }
    }
}
