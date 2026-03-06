package com.familytracker

import android.app.Activity
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.familytracker.receivers.DeviceAdminReceiver
import kotlin.random.Random

/**
 * Full-screen lock activity that completely blocks the device.
 * Uses Device Admin to set random password - device becomes truly unusable.
 * Can only be unlocked via remote command from dashboard.
 */
class DeviceLockActivity : Activity() {
    
    companion object {
        private const val TAG = "DeviceLockActivity"
        const val ACTION_UNLOCK = "com.familytracker.ACTION_UNLOCK"
        private const val PREFS_NAME = "device_lock_prefs"
        private const val KEY_LOCK_PASSWORD = "lock_password"
        private const val KEY_IS_LOCKED = "is_locked"
        
        private var isLocked = false
        
        fun isDeviceLocked(): Boolean = isLocked
        
        fun lockDevice(context: Context) {
            if (isLocked) {
                // Already locked, just bring activity to front
                val intent = Intent(context, DeviceLockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                context.startActivity(intent)
                return
            }
            
            isLocked = true
            
            // Save lock state
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IS_LOCKED, true)
                .apply()
            
            // Try to set a random password using Device Admin
            setRandomPassword(context)
            
            val intent = Intent(context, DeviceLockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            context.startActivity(intent)
        }
        
        private fun setRandomPassword(context: Context) {
            try {
                val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val componentName = ComponentName(context, DeviceAdminReceiver::class.java)
                
                if (devicePolicyManager.isAdminActive(componentName)) {
                    // Generate a random 16-digit password
                    val password = (1..16).map { Random.nextInt(0, 10) }.joinToString("")
                    
                    // Try to reset password (works on older Android or if device owner)
                    @Suppress("DEPRECATION")
                    val success = devicePolicyManager.resetPassword(password, DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY)
                    
                    if (success) {
                        // Save the password so we can clear it on unlock
                        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit()
                            .putString(KEY_LOCK_PASSWORD, password)
                            .apply()
                        Log.d(TAG, "Random password set successfully")
                    } else {
                        Log.w(TAG, "Could not set password - using overlay lock only")
                    }
                    
                    // Lock the device
                    devicePolicyManager.lockNow()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting random password", e)
            }
        }
        
        fun unlockDevice(context: Context) {
            isLocked = false
            
            // Clear lock state
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IS_LOCKED, false)
                .apply()
            
            // Clear the password we set
            clearPassword(context)
            
            LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(ACTION_UNLOCK))
        }
        
        private fun clearPassword(context: Context) {
            try {
                val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val componentName = ComponentName(context, DeviceAdminReceiver::class.java)
                
                if (devicePolicyManager.isAdminActive(componentName)) {
                    // Try to reset to empty password
                    @Suppress("DEPRECATION")
                    val success = devicePolicyManager.resetPassword("", 0)
                    
                    if (success) {
                        Log.d(TAG, "Password cleared successfully")
                    } else {
                        Log.w(TAG, "Could not clear password")
                    }
                }
                
                // Clear saved password
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .remove(KEY_LOCK_PASSWORD)
                    .apply()
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing password", e)
            }
        }
        
        fun checkAndRestoreLock(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.getBoolean(KEY_IS_LOCKED, false)) {
                isLocked = true
                lockDevice(context)
            }
        }
    }
    
    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_UNLOCK) {
                isLocked = false
                finishAndRemoveTask()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make it full screen and show over lock screen
        setupWindowFlags()
        
        // Create and set the lock screen view
        setContentView(createLockView())
        
        // Register for unlock broadcasts
        LocalBroadcastManager.getInstance(this).registerReceiver(
            unlockReceiver,
            IntentFilter(ACTION_UNLOCK)
        )
    }
    
    private fun setupWindowFlags() {
        // Show over lock screen and keep screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        
        // Dismiss keyguard
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }
        
        // Full screen immersive
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        
        // Prevent screenshots
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }
    
    private fun createLockView(): View {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0d1117"))
            setPadding(64, 64, 64, 64)
        }
        
        val lockIcon = TextView(this).apply {
            text = "🔒"
            textSize = 72f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
        
        val title = TextView(this).apply {
            text = "DEVICE LOCKED"
            textSize = 32f
            setTextColor(Color.parseColor("#ff6b6b"))
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, 48, 0, 24)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        val message = TextView(this).apply {
            text = "This device has been remotely locked by the owner.\n\nThe device cannot be used until it is unlocked from the Family Tracker dashboard.\n\nIf you found this device, please contact the owner."
            textSize = 18f
            setTextColor(Color.parseColor("#8b949e"))
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, 24, 0, 48)
            setLineSpacing(8f, 1f)
        }
        
        val footer = TextView(this).apply {
            text = "Protected by Family Tracker"
            textSize = 14f
            setTextColor(Color.parseColor("#484f58"))
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
        
        layout.addView(lockIcon)
        layout.addView(title)
        layout.addView(message)
        layout.addView(footer)
        
        return layout
    }
    
    override fun onBackPressed() {
        // Block back button when locked
        if (isLocked) {
            return
        }
        super.onBackPressed()
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Block all keys when locked
        if (isLocked) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_HOME,
                KeyEvent.KEYCODE_APP_SWITCH,
                KeyEvent.KEYCODE_MENU -> return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    
    override fun onPause() {
        super.onPause()
        // If still locked, restart this activity to stay on top
        if (isLocked) {
            lockDevice(this)
        }
    }
    
    override fun onStop() {
        super.onStop()
        // Restart if locked
        if (isLocked) {
            lockDevice(this)
        }
    }
    
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // User pressed home - restart lock activity if still locked
        if (isLocked) {
            lockDevice(this)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(unlockReceiver)
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && isLocked) {
            // Lost focus, bring back to front
            lockDevice(this)
        } else if (hasFocus) {
            setupWindowFlags()
        }
    }
}
