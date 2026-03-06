package com.familytracker.ui

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.familytracker.EmergencyContactsActivity
import com.familytracker.R
import com.familytracker.data.PreferencesManager
import com.familytracker.databinding.FragmentEmergencyBinding
import com.familytracker.services.LocationService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EmergencyFragment : Fragment() {
    
    private var _binding: FragmentEmergencyBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var preferencesManager: PreferencesManager
    private var sosHandler = Handler(Looper.getMainLooper())
    private var sosRunnable: Runnable? = null
    private var isPressing = false
    private var pulseAnimator: AnimatorSet? = null
    
    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            triggerSOS()
        } else {
            Toast.makeText(requireContext(), "SMS permission required for SOS", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmergencyBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())
        
        setupSOSButton()
        setupContactsButton()
        loadContacts()
        startPulseAnimation()
    }
    
    private fun setupSOSButton() {
        binding.btnSOS.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isPressing = true
                    sosRunnable = Runnable {
                        if (isPressing) {
                            checkAndTriggerSOS()
                        }
                    }
                    sosHandler.postDelayed(sosRunnable!!, 3000) // 3 second hold
                    
                    // Scale animation while pressing
                    binding.btnSOS.animate()
                        .scaleX(0.9f)
                        .scaleY(0.9f)
                        .setDuration(100)
                        .start()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isPressing = false
                    sosRunnable?.let { sosHandler.removeCallbacks(it) }
                    
                    // Reset scale
                    binding.btnSOS.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupContactsButton() {
        binding.btnAddContact.setOnClickListener {
            startActivity(Intent(requireContext(), EmergencyContactsActivity::class.java))
        }
    }
    
    private fun loadContacts() {
        lifecycleScope.launch {
            val contacts = preferencesManager.emergencyContacts.first()
            
            binding.contactsContainer.removeAllViews()
            
            if (contacts.isEmpty()) {
                binding.tvNoContacts.visibility = View.VISIBLE
            } else {
                binding.tvNoContacts.visibility = View.GONE
                
                contacts.forEachIndexed { index, contact ->
                    val itemView = layoutInflater.inflate(R.layout.item_contact, binding.contactsContainer, false)
                    itemView.findViewById<TextView>(R.id.tvContactName).text = contact.first
                    itemView.findViewById<TextView>(R.id.tvContactNumber).text = contact.second
                    itemView.findViewById<View>(R.id.contactAvatar).apply {
                        val colors = listOf(R.color.primary, R.color.secondary, R.color.accent)
                        setBackgroundResource(R.drawable.circle_bg_primary)
                        backgroundTintList = ContextCompat.getColorStateList(requireContext(), colors[index % colors.size])
                    }
                    binding.contactsContainer.addView(itemView)
                }
            }
        }
    }
    
    private fun startPulseAnimation() {
        val scaleOuterX = ObjectAnimator.ofFloat(binding.sosPulseOuter, "scaleX", 1f, 1.2f, 1f)
        val scaleOuterY = ObjectAnimator.ofFloat(binding.sosPulseOuter, "scaleY", 1f, 1.2f, 1f)
        val alphaOuter = ObjectAnimator.ofFloat(binding.sosPulseOuter, "alpha", 0.3f, 0.1f, 0.3f)
        
        val scaleInnerX = ObjectAnimator.ofFloat(binding.sosPulseInner, "scaleX", 1f, 1.15f, 1f)
        val scaleInnerY = ObjectAnimator.ofFloat(binding.sosPulseInner, "scaleY", 1f, 1.15f, 1f)
        val alphaInner = ObjectAnimator.ofFloat(binding.sosPulseInner, "alpha", 0.5f, 0.2f, 0.5f)
        
        pulseAnimator = AnimatorSet().apply {
            playTogether(scaleOuterX, scaleOuterY, alphaOuter, scaleInnerX, scaleInnerY, alphaInner)
            duration = 2000
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (_binding != null) {
                        start()
                    }
                }
            })
            start()
        }
    }
    
    private fun checkAndTriggerSOS() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) 
            == PackageManager.PERMISSION_GRANTED) {
            triggerSOS()
        } else {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }
    
    private fun triggerSOS() {
        lifecycleScope.launch {
            val contacts = preferencesManager.emergencyContacts.first()
            
            if (contacts.isEmpty()) {
                Toast.makeText(requireContext(), "Please add emergency contacts first", Toast.LENGTH_LONG).show()
                return@launch
            }
            
            // Trigger location burst
            LocationService.triggerBurstMode("sos")
            
            // Build SOS message
            val locationString = "Location: https://maps.google.com/?q=0,0" // Would use actual location
            val message = "🚨 SOS ALERT! I need help!\n\n$locationString\n\nSent from TrackIt"
            
            try {
                val smsManager = SmsManager.getDefault()
                for ((name, number) in contacts) {
                    smsManager.sendTextMessage(number, null, message, null, null)
                }
                Toast.makeText(requireContext(), getString(R.string.sos_sent), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to send SOS: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadContacts()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        pulseAnimator?.cancel()
        sosRunnable?.let { sosHandler.removeCallbacks(it) }
        _binding = null
    }
}
