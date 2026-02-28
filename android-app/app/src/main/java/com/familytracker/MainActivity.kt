package com.familytracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.familytracker.data.PreferencesManager
import com.familytracker.databinding.ActivityMainBinding
import com.familytracker.services.LocationService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var preferencesManager: PreferencesManager
    
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            requestBackgroundLocationPermission()
        } else {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show()
        }
    }
    
    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            checkBatteryOptimization()
        } else {
            showBackgroundPermissionRationale()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesManager = PreferencesManager(this)
        
        setupUI()
        checkDeviceId()
    }
    
    private fun setupUI() {
        binding.btnStartTracking.setOnClickListener {
            val deviceId = binding.etDeviceId.text.toString().trim()
            if (deviceId.isEmpty()) {
                binding.tilDeviceId.error = "Please enter Device ID from dashboard"
                return@setOnClickListener
            }
            
            if (!isValidUUID(deviceId)) {
                binding.tilDeviceId.error = "Invalid Device ID format"
                return@setOnClickListener
            }
            
            binding.tilDeviceId.error = null
            saveDeviceIdAndStart(deviceId)
        }
        
        binding.btnStopTracking.setOnClickListener {
            stopTracking()
        }
        
        binding.btnSos.setOnClickListener {
            sendSOS()
        }
    }
    
    private fun checkDeviceId() {
        lifecycleScope.launch {
            val deviceId = preferencesManager.deviceId.first()
            if (deviceId != null) {
                binding.etDeviceId.setText(deviceId)
                updateTrackingStatus(LocationService.isRunning)
            }
        }
    }
    
    private fun saveDeviceIdAndStart(deviceId: String) {
        lifecycleScope.launch {
            preferencesManager.saveDeviceId(deviceId)
            checkLocationPermissions()
        }
    }
    
    private fun checkLocationPermissions() {
        when {
            hasLocationPermissions() -> {
                if (hasBackgroundLocationPermission()) {
                    checkBatteryOptimization()
                } else {
                    requestBackgroundLocationPermission()
                }
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showLocationPermissionRationale()
            }
            else -> {
                requestLocationPermissions()
            }
        }
    }
    
    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    
    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            AlertDialog.Builder(this)
                .setTitle("Background Location Required")
                .setMessage("To track location 24/7, please select 'Allow all the time' in the next screen.")
                .setPositiveButton("Continue") { _, _ ->
                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            checkBatteryOptimization()
        }
    }
    
    private fun showLocationPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Required")
            .setMessage("This app needs location access to track device location for family safety.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestLocationPermissions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showBackgroundPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Background Location Required")
            .setMessage("For 24/7 tracking, please go to Settings and enable 'Allow all the time' for location.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun checkBatteryOptimization() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            AlertDialog.Builder(this)
                .setTitle("Disable Battery Optimization")
                .setMessage("To ensure reliable tracking, please disable battery optimization for this app.")
                .setPositiveButton("Continue") { _, _ ->
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                    startTracking()
                }
                .setNegativeButton("Skip") { _, _ ->
                    startTracking()
                }
                .show()
        } else {
            startTracking()
        }
    }
    
    private fun startTracking() {
        val intent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        updateTrackingStatus(true)
        Toast.makeText(this, "Tracking started", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopTracking() {
        val intent = Intent(this, LocationService::class.java)
        stopService(intent)
        updateTrackingStatus(false)
        Toast.makeText(this, "Tracking stopped", Toast.LENGTH_SHORT).show()
    }
    
    private fun sendSOS() {
        if (LocationService.isRunning) {
            LocationService.triggerSOS()
            Toast.makeText(this, "SOS Alert Sent!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Start tracking first", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateTrackingStatus(isTracking: Boolean) {
        binding.apply {
            if (isTracking) {
                tvStatus.text = "Tracking Active"
                tvStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_dark))
                btnStartTracking.isEnabled = false
                btnStopTracking.isEnabled = true
                btnSos.isEnabled = true
                etDeviceId.isEnabled = false
            } else {
                tvStatus.text = "Tracking Inactive"
                tvStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
                btnStartTracking.isEnabled = true
                btnStopTracking.isEnabled = false
                btnSos.isEnabled = false
                etDeviceId.isEnabled = true
            }
        }
    }
    
    private fun isValidUUID(uuid: String): Boolean {
        val uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()
        return uuidRegex.matches(uuid)
    }
    
    override fun onResume() {
        super.onResume()
        updateTrackingStatus(LocationService.isRunning)
    }
}
