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
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.familytracker.receivers.DeviceAdminReceiver
import com.familytracker.services.LocationService
import com.familytracker.services.LockAccessibilityService
import kotlin.random.Random

/**
 * Full-screen lock activity that completely blocks the device.
 * STEALTH MODE: Makes phone appear completely dead/off while still tracking.
 * Can only be unlocked via remote command from dashboard.
 */
class DeviceLockActivity : Activity() {
    
    companion object {
        private const val TAG = "DeviceLockActivity"
        const val ACTION_UNLOCK = "com.familytracker.ACTION_UNLOCK"
        private const val PREFS_NAME = "device_lock_prefs"
        private const val KEY_LOCK_PASSWORD = "lock_password"
        private const val KEY_IS_LOCKED = "is_locked"
        private const val KEY_STEALTH_MODE = "stealth_mode"
        
        private var isLocked = false
        private var isStealthMode = false
        
        fun isDeviceLocked(): Boolean = isLocked
        fun isInStealthMode(): Boolean = isStealthMode
        
        fun lockDevice(context: Context, stealth: Boolean = false) {
            if (isLocked && !stealth) {
                // Already locked, just bring activity to front
                val intent = Intent(context, DeviceLockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                context.startActivity(intent)
                return
            }
            
            isLocked = true
            isStealthMode = stealth
            
            // Save lock state
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IS_LOCKED, true)
                .putBoolean(KEY_STEALTH_MODE, stealth)
                .apply()
            
            // Try to set a random password using Device Admin
            setRandomPassword(context)
            
            // Trigger location burst to track thief
            LocationService.triggerBurstMode("device_locked")
            
            val intent = Intent(context, DeviceLockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                putExtra("stealth", stealth)
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
            isStealthMode = false
            
            // Clear lock state
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IS_LOCKED, false)
                .putBoolean(KEY_STEALTH_MODE, false)
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
                isStealthMode = prefs.getBoolean(KEY_STEALTH_MODE, false)
                lockDevice(context, isStealthMode)
            }
        }
    }
    
    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_UNLOCK) {
                isLocked = false
                isStealthMode = false
                finishAndRemoveTask()
            }
        }
    }
    
    // Receiver to detect when screen turns off (power button pressed)
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF && isLocked) {
                // Power button was pressed - immediately wake up screen
                wakeUpScreen()
            }
        }
    }
    
    private fun wakeUpScreen() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or 
                PowerManager.ACQUIRE_CAUSES_WAKEUP or 
                PowerManager.ON_AFTER_RELEASE,
                "FamilyTracker:WakeUp"
            )
            wakeLock.acquire(3000L)
            wakeLock.release()
            
            // Bring lock activity back to front
            handler.postDelayed({
                if (isLocked) {
                    lockDevice(this, stealthModeEnabled)
                }
            }, 100)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to wake screen", e)
        }
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private var tapCount = 0
    private var lastTapTime = 0L
    private var stealthModeEnabled = false
    private var statusBarBlocker: View? = null
    private var windowManager: WindowManager? = null
    private var collapseRunnable: Runnable? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if stealth mode from intent or saved state
        stealthModeEnabled = intent?.getBooleanExtra("stealth", false) ?: false
        if (!stealthModeEnabled) {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            stealthModeEnabled = prefs.getBoolean(KEY_STEALTH_MODE, false)
        }
        isStealthMode = stealthModeEnabled
        
        // Set basic window flags BEFORE setContentView
        setupWindowFlagsEarly()
        
        // Create and set the lock screen view (stealth = black screen)
        setContentView(createLockView())
        
        // Set system UI flags AFTER setContentView (requires DecorView)
        setupSystemUI()
        
        // Block the status/notification bar with an overlay
        blockStatusBar()
        
        // Register for unlock broadcasts
        LocalBroadcastManager.getInstance(this).registerReceiver(
            unlockReceiver,
            IntentFilter(ACTION_UNLOCK)
        )
        
        // Register for screen off events (power button press)
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        
        // In stealth mode, turn screen off after 2 seconds to appear dead
        if (stealthModeEnabled) {
            handler.postDelayed({
                turnScreenOff()
            }, 2000)
        }
    }
    
    private fun turnScreenOff() {
        try {
            // Reduce brightness to minimum
            val params = window.attributes
            params.screenBrightness = 0.0f
            window.attributes = params
            
            // Don't keep screen on in stealth mode
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (e: Exception) {
            Log.e(TAG, "Error turning screen off", e)
        }
    }
    
    private fun blockStatusBar() {
        // FIRST: Try using accessibility service (can truly block system UI)
        if (LockAccessibilityService.isServiceRunning()) {
            Log.d(TAG, "✓ LockAccessibilityService is running - using it to block status bar")
            LockAccessibilityService.blockStatusBar()
            // The accessibility service overlay is sufficient - no need for app overlay
            // But we still start collapse monitoring as backup
            startCollapseMonitoring()
            return
        } else {
            Log.w(TAG, "⚠ LockAccessibilityService is NOT running! Status bar blocking will be LIMITED.")
            Log.w(TAG, "⚠ User needs to enable Accessibility Service in Settings for full blocking.")
        }
        
        // Fallback: Use TYPE_APPLICATION_OVERLAY (limited - can't block system UI)
        try {
            // Check if we have overlay permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Log.w(TAG, "No overlay permission - cannot add visual overlay")
                return
            }
            
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            // Get screen dimensions
            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels
            val screenWidth = displayMetrics.widthPixels
            
            // Get status bar height
            val statusBarHeight = getStatusBarHeight()
            
            // Create a view that covers top portion of screen to block notification pulls
            statusBarBlocker = View(this).apply {
                setBackgroundColor(if (stealthModeEnabled) Color.BLACK else Color.TRANSPARENT)
                // Consume ALL touch events
                setOnTouchListener { _, _ ->
                    // Immediately collapse any notification panel
                    collapseStatusBar()
                    true
                }
            }
            
            // Layout params for status bar blocking overlay
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR
            }
            
            // Cover top 50% of screen with EXTRA height starting ABOVE visible screen
            // to catch the invisible swipe area
            val blockerHeight = (screenHeight * 0.5).toInt() + statusBarHeight
            
            val params = WindowManager.LayoutParams(
                screenWidth,
                blockerHeight,
                layoutType,
                // Critical flags combination for blocking touch
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                // Start ABOVE the visible screen to cover the status bar touch area
                y = -statusBarHeight
            }
            
            windowManager?.addView(statusBarBlocker, params)
            Log.d(TAG, "Status bar blocker added: height=$blockerHeight, y=-$statusBarHeight")
            
            // Start continuous collapse monitoring - very aggressive at 50ms intervals
            startCollapseMonitoring()
            
            // Also start monitoring window focus to detect notification panel opening
            startFocusMonitoring()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to block status bar", e)
        }
    }
    
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return if (result > 0) result else 100 // Default fallback
    }
    
    private fun startFocusMonitoring() {
        // Additional monitoring - when we lose focus (notification panel opened), regain it
        handler.post(object : Runnable {
            override fun run() {
                if (isLocked) {
                    if (!hasWindowFocus()) {
                        // Lost focus - notification panel might be open
                        collapseStatusBar()
                        // Bring our activity back to front
                        val intent = Intent(this@DeviceLockActivity, DeviceLockActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                        startActivity(intent)
                    }
                    handler.postDelayed(this, 200)
                }
            }
        })
    }
    
    private fun startCollapseMonitoring() {
        // Continuously collapse the status bar every 50ms while locked - AGGRESSIVE
        collapseRunnable = object : Runnable {
            override fun run() {
                if (isLocked) {
                    collapseStatusBar()
                    handler.postDelayed(this, 50) // Very fast - 50ms
                }
            }
        }
        handler.post(collapseRunnable!!)
    }
    
    private fun stopCollapseMonitoring() {
        collapseRunnable?.let { handler.removeCallbacks(it) }
        collapseRunnable = null
    }
    
    @Suppress("DEPRECATION", "DiscouragedPrivateApi")
    private fun collapseStatusBar() {
        try {
            // Use reflection to collapse the status bar
            val statusBarService = getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val collapse = statusBarManager.getMethod("collapsePanels")
            collapse.invoke(statusBarService)
        } catch (e: Exception) {
            // Method might not exist on all devices, try alternative
            try {
                val service = getSystemService("statusbar")
                val statusBarManager = Class.forName("android.app.StatusBarManager")
                val collapse = statusBarManager.getMethod("collapse")
                collapse.invoke(service)
            } catch (ex: Exception) {
                // Silently fail - not all devices support this
            }
        }
    }
    
    private fun removeStatusBarBlocker() {
        // Stop the collapse monitoring
        stopCollapseMonitoring()
        
        // Also unblock via accessibility service if running
        if (LockAccessibilityService.isServiceRunning()) {
            LockAccessibilityService.unblockStatusBar()
        }
        
        try {
            statusBarBlocker?.let {
                windowManager?.removeView(it)
                statusBarBlocker = null
                Log.d(TAG, "Status bar blocker removed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove status bar blocker", e)
        }
    }
    
    private fun setupWindowFlagsEarly() {
        // Show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            if (!stealthModeEnabled) {
                setTurnScreenOn(true)
            }
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            if (!stealthModeEnabled) {
                @Suppress("DEPRECATION")
                window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            }
        }
        
        // Dismiss keyguard
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }
        
        // Full screen immersive
        if (!stealthModeEnabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        
        // Prevent screenshots
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    
    private fun setupSystemUI() {
        // System UI visibility requires DecorView - call after setContentView
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error setting system UI", e)
        }
    }
    
    private fun setupWindowFlags() {
        // Combined setup for onWindowFocusChanged
        setupWindowFlagsEarly()
        setupSystemUI()
    }
    
    private fun createLockView(): View {
        // STEALTH MODE: Just pure black screen - phone appears dead
        if (stealthModeEnabled) {
            return View(this).apply {
                setBackgroundColor(Color.BLACK)
                // Intercept all touches silently
                setOnTouchListener { _, _ -> true }
            }
        }
        
        // NORMAL LOCK MODE: Show lock message
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
                KeyEvent.KEYCODE_MENU,
                KeyEvent.KEYCODE_POWER,
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN -> return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (isLocked && ev != null) {
            // If touch starts at top 15% of screen, collapse status bar immediately
            val screenHeight = resources.displayMetrics.heightPixels
            if (ev.rawY < screenHeight * 0.15f) {
                collapseStatusBar()
            }
            // On any touch event, aggressively collapse
            if (ev.action == MotionEvent.ACTION_DOWN || ev.action == MotionEvent.ACTION_MOVE) {
                collapseStatusBar()
            }
        }
        return super.dispatchTouchEvent(ev)
    }
    
    override fun onPause() {
        super.onPause()
        // If still locked, restart this activity to stay on top
        if (isLocked) {
            lockDevice(this)
        }
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (isLocked && !hasFocus) {
            // Lost focus - notification panel might be opening
            // Aggressively collapse it
            collapseStatusBar()
            handler.postDelayed({
                collapseStatusBar()
            }, 50)
            handler.postDelayed({
                collapseStatusBar()
            }, 100)
            // Bring activity back to front
            lockDevice(this)
        } else if (hasFocus) {
            setupWindowFlags()
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
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
        // Remove the status bar blocker overlay
        removeStatusBarBlocker()
    }
}
