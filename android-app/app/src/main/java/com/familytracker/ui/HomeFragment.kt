package com.familytracker.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.familytracker.EmergencyContactsActivity
import com.familytracker.R
import com.familytracker.data.PreferencesManager
import com.familytracker.databinding.FragmentHomeBinding
import com.familytracker.services.LocationService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var preferencesManager: PreferencesManager
    private var isTracking = false
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())
        
        setupUI()
        loadDeviceId()
        updateTrackingStatus()
        updateBatteryInfo()
    }
    
    private fun setupUI() {
        // Main status card - toggle tracking
        binding.cardStatus.setOnClickListener {
            toggleTracking()
        }
        
        // Quick action - toggle tracking
        binding.cardToggleTracking.setOnClickListener {
            toggleTracking()
        }
        
        // Quick action - emergency contacts
        binding.cardEmergency.setOnClickListener {
            startActivity(Intent(requireContext(), EmergencyContactsActivity::class.java))
        }
        
        // Copy device ID
        binding.btnCopyId.setOnClickListener {
            copyDeviceId()
        }
    }
    
    private fun loadDeviceId() {
        lifecycleScope.launch {
            val deviceId = preferencesManager.deviceId.first()
            if (deviceId != null) {
                binding.etDeviceId.setText(deviceId)
            }
        }
    }
    
    private fun toggleTracking() {
        lifecycleScope.launch {
            val deviceId = binding.etDeviceId.text.toString().trim()
            
            if (deviceId.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a Device ID first", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            // Save device ID
            preferencesManager.saveDeviceId(deviceId)
            
            if (isTracking) {
                stopTracking()
            } else {
                startTracking()
            }
        }
    }
    
    private fun startTracking() {
        val intent = Intent(requireContext(), LocationService::class.java)
        ContextCompat.startForegroundService(requireContext(), intent)
        isTracking = true
        updateTrackingStatus()
        Toast.makeText(requireContext(), "Tracking started", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopTracking() {
        requireContext().stopService(Intent(requireContext(), LocationService::class.java))
        isTracking = false
        updateTrackingStatus()
        Toast.makeText(requireContext(), "Tracking stopped", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateTrackingStatus() {
        isTracking = LocationService.isRunning
        
        if (isTracking) {
            binding.tvStatusLabel.text = getString(R.string.tracking_active)
            binding.tvStatusSubtitle.text = getString(R.string.your_device_is_protected)
            binding.statusIndicator.setBackgroundResource(R.drawable.circle_status_active)
            binding.statusBackground.setBackgroundResource(R.drawable.card_gradient_success)
            binding.tvTrackingAction.text = getString(R.string.stop_tracking)
            binding.ivTrackingIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error))
        } else {
            binding.tvStatusLabel.text = getString(R.string.tracking_inactive)
            binding.tvStatusSubtitle.text = getString(R.string.tap_to_start)
            binding.statusIndicator.setBackgroundResource(R.drawable.circle_status_inactive)
            binding.statusBackground.setBackgroundResource(R.drawable.card_gradient_primary)
            binding.tvTrackingAction.text = getString(R.string.start_tracking)
            binding.ivTrackingIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success))
        }
    }
    
    private fun updateBatteryInfo() {
        val batteryManager = requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        binding.tvBatteryValue.text = "$batteryLevel%"
        
        // Last update time
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        binding.tvLastUpdate.text = timeFormat.format(Date())
        
        // Accuracy placeholder
        binding.tvAccuracy.text = "~10m"
    }
    
    private fun copyDeviceId() {
        val deviceId = binding.etDeviceId.text.toString()
        if (deviceId.isNotEmpty()) {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Device ID", deviceId)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), getString(R.string.device_id_copied), Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateTrackingStatus()
        updateBatteryInfo()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
