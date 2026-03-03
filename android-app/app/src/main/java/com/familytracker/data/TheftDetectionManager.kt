package com.familytracker.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Theft Detection Scoring System
 * Calculates theft probability based on various indicators
 */
object TheftDetectionManager {
    
    private const val TAG = "TheftDetectionManager"
    
    // Scoring weights
    private const val SIM_REMOVED_SCORE = 50
    private const val SIM_CHANGED_SCORE = 50
    private const val WRONG_PIN_SCORE = 15  // Per attempt, max 3
    private const val AIRPLANE_MODE_SCORE = 25
    private const val UNUSUAL_MOTION_SCORE = 20
    private const val DATA_DISABLED_SCORE = 20
    private const val UNUSUAL_LOCATION_SCORE = 15
    private const val UNUSUAL_TIME_SCORE = 10
    
    // Threshold for theft alert
    private const val THEFT_ALERT_THRESHOLD = 50
    
    private val _theftScore = MutableStateFlow(0)
    val theftScore: StateFlow<Int> = _theftScore
    
    private val _isTheftMode = MutableStateFlow(false)
    val isTheftMode: StateFlow<Boolean> = _isTheftMode
    
    private var wrongPinAttempts = 0
    private val events = mutableListOf<TheftEvent>()
    
    data class TheftEvent(
        val type: String,
        val score: Int,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    fun reportSimRemoved() {
        addEvent("sim_removed", SIM_REMOVED_SCORE)
    }
    
    fun reportSimChanged() {
        addEvent("sim_changed", SIM_CHANGED_SCORE)
    }
    
    fun reportWrongPin() {
        wrongPinAttempts++
        if (wrongPinAttempts <= 3) {
            addEvent("wrong_pin_$wrongPinAttempts", WRONG_PIN_SCORE)
        }
    }
    
    fun reportAirplaneModeOn() {
        addEvent("airplane_mode", AIRPLANE_MODE_SCORE)
    }
    
    fun reportDataDisabled() {
        addEvent("data_disabled", DATA_DISABLED_SCORE)
    }
    
    fun reportUnusualMotion() {
        addEvent("unusual_motion", UNUSUAL_MOTION_SCORE)
    }
    
    fun reportUnusualLocation() {
        addEvent("unusual_location", UNUSUAL_LOCATION_SCORE)
    }
    
    fun reportUnusualTime() {
        addEvent("unusual_time", UNUSUAL_TIME_SCORE)
    }
    
    private fun addEvent(type: String, score: Int) {
        // Prevent duplicate events within 30 seconds
        val now = System.currentTimeMillis()
        val recentSameEvent = events.find { 
            it.type == type && (now - it.timestamp) < 30_000 
        }
        
        if (recentSameEvent != null) {
            Log.d(TAG, "Ignoring duplicate event: $type")
            return
        }
        
        events.add(TheftEvent(type, score))
        
        // Calculate total score (only from last 5 minutes)
        val cutoff = now - (5 * 60 * 1000)
        val recentEvents = events.filter { it.timestamp > cutoff }
        val totalScore = recentEvents.sumOf { it.score }
        
        _theftScore.value = totalScore
        
        Log.w(TAG, "Theft event: $type (+$score) | Total score: $totalScore")
        
        // Check if threshold reached
        if (totalScore >= THEFT_ALERT_THRESHOLD && !_isTheftMode.value) {
            triggerTheftMode()
        }
    }
    
    private fun triggerTheftMode() {
        _isTheftMode.value = true
        Log.e(TAG, "🚨 THEFT MODE ACTIVATED! Score: ${_theftScore.value}")
    }
    
    fun activateTheftMode() {
        _isTheftMode.value = true
        _theftScore.value = 100
        Log.e(TAG, "🚨 THEFT MODE MANUALLY ACTIVATED!")
    }
    
    fun deactivateTheftMode() {
        _isTheftMode.value = false
        _theftScore.value = 0
        wrongPinAttempts = 0
        events.clear()
        Log.d(TAG, "Theft mode deactivated")
    }
    
    fun getRecentEvents(): List<TheftEvent> {
        val cutoff = System.currentTimeMillis() - (5 * 60 * 1000)
        return events.filter { it.timestamp > cutoff }
    }
    
    fun isTheftModeActive(): Boolean = _isTheftMode.value
}
