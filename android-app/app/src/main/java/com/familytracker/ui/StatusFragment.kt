package com.familytracker.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.familytracker.R
import com.familytracker.databinding.FragmentStatusBinding
import com.familytracker.services.LocationService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatusFragment : Fragment() {
    
    private var _binding: FragmentStatusBinding? = null
    private val binding get() = _binding!!
    
    private val requiredPermissions = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION to "Location",
        Manifest.permission.ACCESS_COARSE_LOCATION to "Coarse Location",
        Manifest.permission.ACCESS_BACKGROUND_LOCATION to "Background Location",
        Manifest.permission.CAMERA to "Camera",
        Manifest.permission.SEND_SMS to "SMS",
        Manifest.permission.READ_CONTACTS to "Contacts"
    )
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatusBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateStatus()
        setupPermissionsList()
    }
    
    private fun updateStatus() {
        val isConnected = LocationService.isRunning
        
        // Connection status
        if (isConnected) {
            binding.tvConnectionStatus.text = getString(R.string.connected)
            binding.connectionIndicator.setBackgroundResource(R.drawable.circle_status_active)
            binding.ivConnectionIcon.setImageResource(R.drawable.ic_nav_status)
            binding.ivConnectionIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success))
        } else {
            binding.tvConnectionStatus.text = getString(R.string.disconnected)
            binding.connectionIndicator.setBackgroundResource(R.drawable.circle_status_inactive)
            binding.ivConnectionIcon.setImageResource(R.drawable.ic_wifi_off)
            binding.ivConnectionIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error))
        }
        
        // Battery level
        val batteryManager = requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        binding.tvBattery.text = "$batteryLevel%"
        
        // Update battery icon color based on level
        val batteryColor = when {
            batteryLevel <= 20 -> R.color.error
            batteryLevel <= 50 -> R.color.warning
            else -> R.color.success
        }
        binding.ivBattery.setColorFilter(ContextCompat.getColor(requireContext(), batteryColor))
        
        // Last update
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        binding.tvLastUpdateTime.text = timeFormat.format(Date())
        
        // Location accuracy (placeholder - would come from actual location)
        binding.tvLocationAccuracy.text = "± 10 meters"
    }
    
    private fun setupPermissionsList() {
        binding.permissionsContainer.removeAllViews()
        
        for ((permission, name) in requiredPermissions) {
            val isGranted = ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
            
            val itemView = layoutInflater.inflate(R.layout.item_permission, binding.permissionsContainer, false)
            
            itemView.findViewById<TextView>(R.id.tvPermissionName).text = name
            itemView.findViewById<TextView>(R.id.tvPermissionStatus).apply {
                text = if (isGranted) "Granted" else "Not Granted"
                setTextColor(ContextCompat.getColor(requireContext(), if (isGranted) R.color.success else R.color.error))
            }
            itemView.findViewById<View>(R.id.permissionIndicator).setBackgroundResource(
                if (isGranted) R.drawable.circle_status_active else R.drawable.circle_status_inactive
            )
            
            binding.permissionsContainer.addView(itemView)
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateStatus()
        setupPermissionsList()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
