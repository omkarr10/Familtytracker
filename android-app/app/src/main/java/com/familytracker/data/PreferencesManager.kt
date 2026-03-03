package com.familytracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
        private val BACKUP_PHONE = stringPreferencesKey("backup_phone")
    }
    
    val deviceId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[DEVICE_ID]
    }
    
    val simSerial: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SIM_SERIAL]
    }
    
    val smsSecretCode: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SMS_SECRET_CODE] ?: "TRACKIT"  // Default secret code
    }
    
    val authorizedNumbers: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[AUTHORIZED_NUMBERS] ?: emptySet()
    }
    
    val backupPhone: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[BACKUP_PHONE]
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
    
    suspend fun saveBackupPhone(phone: String) {
        context.dataStore.edit { preferences ->
            preferences[BACKUP_PHONE] = phone
        }
    }
    
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
