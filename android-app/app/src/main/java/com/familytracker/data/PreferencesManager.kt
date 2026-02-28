package com.familytracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    
    companion object {
        private val DEVICE_ID = stringPreferencesKey("device_id")
        private val SIM_SERIAL = stringPreferencesKey("sim_serial")
    }
    
    val deviceId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[DEVICE_ID]
    }
    
    val simSerial: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SIM_SERIAL]
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
    
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
