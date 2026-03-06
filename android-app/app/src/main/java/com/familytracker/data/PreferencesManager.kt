package com.familytracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    
    companion object {
        private val DEVICE_ID = stringPreferencesKey("device_id")
        private val SIM_SERIAL = stringPreferencesKey("sim_serial")
        private val SMS_SECRET_CODE = stringPreferencesKey("sms_secret_code")
        private val AUTHORIZED_NUMBERS = stringSetPreferencesKey("authorized_numbers")
        private val EMERGENCY_CONTACTS = stringSetPreferencesKey("emergency_contacts")
        private val BACKUP_PHONE = stringPreferencesKey("backup_phone")
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val BATTERY_SAVER = booleanPreferencesKey("battery_saver")
        private val UPDATE_INTERVAL = intPreferencesKey("update_interval")
        private val SHAKE_TO_SOS = booleanPreferencesKey("shake_to_sos")
        private val INTRUDER_SELFIE = booleanPreferencesKey("intruder_selfie")
        private val SPEED_ALERTS = booleanPreferencesKey("speed_alerts")
        private val SPEED_LIMIT = intPreferencesKey("speed_limit")
    }
    
    val deviceId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[DEVICE_ID] ?: "1ce54707-f014-4f54-a57f-51f623f8517c"
    }
    
    val simSerial: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SIM_SERIAL]
    }
    
    val smsSecretCode: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SMS_SECRET_CODE] ?: "TRACKIT"
    }
    
    val authorizedNumbers: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[AUTHORIZED_NUMBERS] ?: emptySet()
    }
    
    // Emergency contacts stored as "name|number" strings and parsed as pairs
    val emergencyContacts: Flow<List<Pair<String, String>>> = context.dataStore.data.map { preferences ->
        val contactSet = preferences[EMERGENCY_CONTACTS] ?: emptySet()
        contactSet.mapNotNull { entry ->
            val parts = entry.split("|", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }
    }
    
    val backupPhone: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[BACKUP_PHONE]
    }
    
    val darkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE] ?: false
    }
    
    val batterySaver: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BATTERY_SAVER] ?: false
    }
    
    val updateInterval: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[UPDATE_INTERVAL] ?: 30 // default 30 seconds
    }
    
    val shakeToSOS: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHAKE_TO_SOS] ?: true // enabled by default
    }
    
    val intruderSelfie: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[INTRUDER_SELFIE] ?: true // enabled by default
    }
    
    val speedAlerts: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SPEED_ALERTS] ?: false // disabled by default
    }
    
    val speedLimit: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SPEED_LIMIT] ?: 120 // 120 km/h default
    }
    
    suspend fun saveDeviceId(deviceId: String) {
        context.dataStore.edit { preferences ->
            preferences[DEVICE_ID] = deviceId
        }
    }
    
    suspend fun saveSimSerial(serial: String) {
        context.dataStore.edit { preferences ->
            preferences[SIM_SERIAL] = serial
        }
    }
    
    suspend fun saveSmsSecretCode(code: String) {
        context.dataStore.edit { preferences ->
            preferences[SMS_SECRET_CODE] = code
        }
    }
    
    suspend fun saveAuthorizedNumbers(numbers: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[AUTHORIZED_NUMBERS] = numbers
        }
    }
    
    suspend fun saveEmergencyContacts(contacts: List<Pair<String, String>>) {
        context.dataStore.edit { preferences ->
            val contactSet = contacts.map { "${it.first}|${it.second}" }.toSet()
            preferences[EMERGENCY_CONTACTS] = contactSet
        }
    }
    
    suspend fun saveBackupPhone(phone: String) {
        context.dataStore.edit { preferences ->
            preferences[BACKUP_PHONE] = phone
        }
    }
    
    suspend fun saveDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE] = enabled
        }
    }
    
    suspend fun saveBatterySaver(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BATTERY_SAVER] = enabled
        }
    }
    
    suspend fun saveUpdateInterval(interval: Int) {
        context.dataStore.edit { preferences ->
            preferences[UPDATE_INTERVAL] = interval
        }
    }
    
    suspend fun saveShakeToSOS(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHAKE_TO_SOS] = enabled
        }
    }
    
    suspend fun saveIntruderSelfie(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[INTRUDER_SELFIE] = enabled
        }
    }
    
    suspend fun saveSpeedAlerts(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SPEED_ALERTS] = enabled
        }
    }
    
    suspend fun saveSpeedLimit(limit: Int) {
        context.dataStore.edit { preferences ->
            preferences[SPEED_LIMIT] = limit
        }
    }
    
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
