package com.familytracker

import android.Manifest
import android.content.Context
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.familytracker.data.PreferencesManager
import com.familytracker.databinding.ActivityMainBinding
import com.familytracker.receivers.MotionDetectionReceiver
import com.familytracker.services.CommandListenerService
import com.familytracker.services.LocationService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    lateinit var preferencesManager: PreferencesManager
        private set
    
    // Callback for when tracking successfully starts
    var onTrackingStarted: (() -> Unit)? = null
    
    val emergencyContactsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Notify any listeners about the result
    }
    
    private val allPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            requestBackgroundLocationPermission()
        } else {
            Toast.makeText(this, "Location permission is required for full tracking", Toast.LENGTH_LONG).show()
            checkBatteryOptimization()
        }
    }
    
    private val deviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        tryStartTracking()
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
    
    val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            triggerSOSWithPermission()
        } else {
            Toast.makeText(this, "SMS permission required for SOS alerts", Toast.LENGTH_LONG).show()
        }
    }
    
    val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesManager = PreferencesManager(this)
        
        setupNavigation()
        requestInitialPermissions()
        
        // Start command listener for remote commands
        lifecycleScope.launch {
            if (preferencesManager.deviceId.first() != null) {
                CommandListenerService.start(this@MainActivity)
            }
        }
    }
    
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        binding.bottomNavigation.setupWithNavController(navController)
    }
    
    private fun requestInitialPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val needsPermissions = permissions.any { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }
        
        if (needsPermissions) {
            allPermissionsLauncher.launch(permissions.toTypedArray())
        }
    }
    
    // Public methods for fragments to use
    
    fun startTrackingWithDeviceId(deviceId: String) {
        lifecycleScope.launch {
            preferencesManager.saveDeviceId(deviceId)
            checkLocationPermissions()
        }
    }
    
    fun stopTracking() {
        val intent = Intent(this, LocationService::class.java)
        stopService(intent)
        CommandListenerService.stop(this)
        MotionDetectionReceiver.stopMonitoring(this)
        Toast.makeText(this, "Tracking stopped", Toast.LENGTH_SHORT).show()
    }
    
    fun sendSOS() {
        if (!LocationService.isRunning) {
            Toast.makeText(this, "Start tracking first", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            return
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        
        triggerSOSWithPermission()
    }
    
    fun triggerSOSWithPermission() {
        if (LocationService.isRunning) {
            LocationService.triggerSOS()
            Toast.makeText(this, "🆘 SOS Alert Sent!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Start tracking first", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun openEmergencyContacts() {
        val intent = Intent(this, EmergencyContactsActivity::class.java)
        emergencyContactsLauncher.launch(intent)
    }
    
    // Permission flow methods
    
    private fun checkLocationPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needsPermissions = permissions.any { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }

        when {
            !needsPermissions -> {
                if (hasBackgroundLocationPermission()) {
                    checkBatteryOptimization()
                } else {
                    requestBackgroundLocationPermission()
                }
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showPermissionsRationale()
            }
            else -> {
                allPermissionsLauncher.launch(permissions.toTypedArray())
            }
        }
    }
    
    fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
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
    
    private fun showPermissionsRationale() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app needs location, camera, and SMS access to provide full security features.")
            .setPositiveButton("Grant Permissions") { _, _ ->
                requestInitialPermissions()
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
                    checkOverlayPermission()
                }
                .setNegativeButton("Skip") { _, _ ->
                    checkOverlayPermission()
                }
                .show()
        } else {
            checkOverlayPermission()
        }
    }
    
    private fun checkOverlayPermission() {
        // Overlay permission is needed to block notification bar during lock
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("Overlay Permission Required")
                .setMessage("For complete device lock security, please enable 'Display over other apps' permission.")
                .setPositiveButton("Enable") { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                    checkAccessibilityService()
                }
                .setNegativeButton("Skip") { _, _ ->
                    checkAccessibilityService()
                }
                .show()
        } else {
            checkAccessibilityService()
        }
    }
    
    private fun checkAccessibilityService() {
        // Accessibility service is needed to truly block the notification panel
        if (!isAccessibilityServiceEnabled()) {
            AlertDialog.Builder(this)
                .setTitle("Accessibility Service Required")
                .setMessage("For maximum anti-theft protection, please enable TrackIt in Accessibility Settings. This allows complete notification bar blocking during stealth lock.")
                .setPositiveButton("Enable") { _, _ ->
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                    checkDeviceAdminAndStart()
                }
                .setNegativeButton("Skip") { _, _ ->
                    checkDeviceAdminAndStart()
                }
                .show()
        } else {
            checkDeviceAdminAndStart()
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "$packageName/${com.familytracker.services.LockAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(service)
    }
    
    private fun checkDeviceAdminAndStart() {
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        val componentName = android.content.ComponentName(this, com.familytracker.receivers.DeviceAdminReceiver::class.java)
        
        if (!devicePolicyManager.isAdminActive(componentName)) {
            val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required for remote lock and wipe features.")
            }
            deviceAdminLauncher.launch(intent)
        } else {
            tryStartTracking()
        }
    }
    
    private fun tryStartTracking() {
        lifecycleScope.launch {
            if (preferencesManager.deviceId.first() != null) {
                startTrackingService()
            }
        }
    }
    
    private fun startTrackingService() {
        val intent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        CommandListenerService.start(this)
        MotionDetectionReceiver.startMonitoring(this)
        
        onTrackingStarted?.invoke()
        Toast.makeText(this, "Tracking started", Toast.LENGTH_SHORT).show()
    }
    
    fun isValidUUID(uuid: String): Boolean {
        val uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()
        return uuidRegex.matches(uuid)
    }
}
