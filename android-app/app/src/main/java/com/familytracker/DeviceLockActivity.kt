package com.familytracker

import android.app.Activity
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * Full-screen lock activity that completely blocks the device.
 * Can only be unlocked via remote command from dashboard.
 */
class DeviceLockActivity : Activity() {
    
    companion object {
        private const val TAG = "DeviceLockActivity"
        const val ACTION_UNLOCK = "com.familytracker.ACTION_UNLOCK"
        
        private var isLocked = false
        
        fun isDeviceLocked(): Boolean = isLocked
        
        fun lockDevice(context: Context) {
            if (isLocked) return
            isLocked = true
            
            val intent = Intent(context, DeviceLockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            context.startActivity(intent)
        }
        
        fun unlockDevice(context: Context) {
            isLocked = false
            LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(ACTION_UNLOCK))
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
        val textView = TextView(this).apply {
            text = "🔒\n\nDEVICE LOCKED\n\nThis device has been locked remotely.\n\nContact device owner to unlock."
            textSize = 24f
            setTextColor(Color.WHITE)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(Color.parseColor("#1a1a2e"))
            setPadding(48, 48, 48, 48)
        }
        return textView
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
