package com.familytracker.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.familytracker.BuildConfig
import com.familytracker.R
import com.familytracker.data.PreferencesManager
import com.familytracker.databinding.FragmentSettingsBinding
import com.familytracker.services.ShakeDetectorService
import com.familytracker.services.SpeedAlertService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())
        
        setupUI()
        loadSettings()
    }
    
    private fun setupUI() {
        // Copy device ID
        binding.btnCopyDeviceId.setOnClickListener {
            copyDeviceId()
        }
        
        // Dark mode toggle
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                preferencesManager.saveDarkMode(isChecked)
                AppCompatDelegate.setDefaultNightMode(
                    if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }
        
        // Battery saver toggle
        binding.switchBatterySaver.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                preferencesManager.saveBatterySaver(isChecked)
                Toast.makeText(
                    requireContext(),
                    if (isChecked) "Battery saver enabled" else "Battery saver disabled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        // Shake-to-SOS toggle
        binding.switchShakeToSOS.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                preferencesManager.saveShakeToSOS(isChecked)
                if (isChecked) {
                    ShakeDetectorService.start(requireContext())
                    Toast.makeText(requireContext(), "Shake-to-SOS enabled", Toast.LENGTH_SHORT).show()
                } else {
                    ShakeDetectorService.stop(requireContext())
                    Toast.makeText(requireContext(), "Shake-to-SOS disabled", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Intruder selfie toggle
        binding.switchIntruderSelfie.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                preferencesManager.saveIntruderSelfie(isChecked)
                Toast.makeText(
                    requireContext(),
                    if (isChecked) "Intruder selfie enabled" else "Intruder selfie disabled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        // Speed alerts toggle
        binding.switchSpeedAlerts.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                preferencesManager.saveSpeedAlerts(isChecked)
                if (isChecked) {
                    SpeedAlertService.start(requireContext())
                    Toast.makeText(requireContext(), "Speed alerts enabled", Toast.LENGTH_SHORT).show()
                } else {
                    SpeedAlertService.stop(requireContext())
                    Toast.makeText(requireContext(), "Speed alerts disabled", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Grant permissions button
        binding.btnGrantPermissions.setOnClickListener {
            Toast.makeText(requireContext(), "Check Status tab for permissions", Toast.LENGTH_SHORT).show()
        }
        
        // Version info
        try {
            binding.tvVersion.text = "Version ${BuildConfig.VERSION_NAME}"
        } catch (e: Exception) {
            binding.tvVersion.text = "Version 1.0.0"
        }
    }
    
    private fun loadSettings() {
        lifecycleScope.launch {
            // Load device ID
            val deviceId = preferencesManager.deviceId.first()
            binding.tvDeviceId.text = deviceId ?: "Not set"
            
            // Load dark mode setting
            val isDarkMode = preferencesManager.darkMode.first()
            binding.switchDarkMode.isChecked = isDarkMode
            
            // Load battery saver setting
            val isBatterySaver = preferencesManager.batterySaver.first()
            binding.switchBatterySaver.isChecked = isBatterySaver
            
            // Load update interval
            val interval = preferencesManager.updateInterval.first()
            binding.tvUpdateInterval.text = "${interval} seconds"
            
            // Load safety features
            val shakeToSOS = preferencesManager.shakeToSOS.first()
            binding.switchShakeToSOS.isChecked = shakeToSOS
            
            val intruderSelfie = preferencesManager.intruderSelfie.first()
            binding.switchIntruderSelfie.isChecked = intruderSelfie
            
            val speedAlerts = preferencesManager.speedAlerts.first()
            binding.switchSpeedAlerts.isChecked = speedAlerts
            
            val speedLimit = preferencesManager.speedLimit.first()
            binding.tvSpeedLimit.text = "Alert when over $speedLimit km/h"
        }
    }
    
    private fun copyDeviceId() {
        val deviceId = binding.tvDeviceId.text.toString()
        if (deviceId.isNotEmpty() && deviceId != "Not set") {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Device ID", deviceId)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), getString(R.string.device_id_copied), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "No Device ID to copy", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadSettings()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
