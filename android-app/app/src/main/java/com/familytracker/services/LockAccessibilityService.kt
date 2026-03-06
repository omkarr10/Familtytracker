package com.familytracker.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * Accessibility Service that can TRULY block the notification bar.
 * TYPE_ACCESSIBILITY_OVERLAY is the only overlay type that can block system UI.
 */
class LockAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "LockAccessibilityService"
        const val ACTION_BLOCK_STATUS_BAR = "com.familytracker.BLOCK_STATUS_BAR"
        const val ACTION_UNBLOCK_STATUS_BAR = "com.familytracker.UNBLOCK_STATUS_BAR"
        
        private var instance: LockAccessibilityService? = null
        
        fun isServiceRunning(): Boolean = instance != null
        
        fun blockStatusBar() {
            instance?.addBlockingOverlay()
        }
        
        fun unblockStatusBar() {
            instance?.removeBlockingOverlay()
        }
    }
    
    private var blockingOverlay: View? = null
    private var windowManager: WindowManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isBlocking = false
    
    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_BLOCK_STATUS_BAR -> addBlockingOverlay()
                ACTION_UNBLOCK_STATUS_BAR -> removeBlockingOverlay()
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // Register for commands
        val filter = IntentFilter().apply {
            addAction(ACTION_BLOCK_STATUS_BAR)
            addAction(ACTION_UNBLOCK_STATUS_BAR)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(commandReceiver, filter)
        
        Log.d(TAG, "LockAccessibilityService created")
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        serviceInfo = info
        
        Log.d(TAG, "LockAccessibilityService connected")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We can detect notification panel opening here
        if (isBlocking && event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val className = event.className?.toString() ?: ""
            // Detect notification shade
            if (className.contains("StatusBar") || 
                className.contains("NotificationPanel") ||
                className.contains("ShadeController")) {
                // Immediately close it
                performGlobalAction(GLOBAL_ACTION_BACK)
                collapseStatusBar()
                Log.d(TAG, "Detected notification panel, closing it")
            }
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "LockAccessibilityService interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        removeBlockingOverlay()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(commandReceiver)
        instance = null
        Log.d(TAG, "LockAccessibilityService destroyed")
    }
    
    fun addBlockingOverlay() {
        if (blockingOverlay != null) return
        isBlocking = true
        
        try {
            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels
            val screenWidth = displayMetrics.widthPixels
            
            // Get status bar height
            val statusBarHeight = getStatusBarHeight()
            
            // Block area: Cover top portion to intercept ALL swipe gestures
            // Android notification gesture can start from anywhere in top ~10% of screen
            val blockHeight = maxOf(statusBarHeight * 3, (screenHeight * 0.15).toInt())
            
            Log.d(TAG, "Creating blocking overlay: screenWidth=$screenWidth, blockHeight=$blockHeight, statusBarHeight=$statusBarHeight")
            
            // Create blocking overlay - BLACK for stealth
            blockingOverlay = View(this).apply {
                setBackgroundColor(Color.BLACK)
                // This touch listener intercepts ALL touches in the overlay area
                setOnTouchListener { _, event ->
                    Log.d(TAG, "Overlay touch intercepted: action=${event.action}")
                    // Collapse status bar on every touch
                    collapseStatusBar()
                    // Return TRUE to CONSUME the touch event - critical!
                    true
                }
            }
            
            // TYPE_ACCESSIBILITY_OVERLAY is the KEY - it's above notification panel!
            // CRITICAL: Do NOT use FLAG_NOT_TOUCH_MODAL - it lets touches pass through!
            val params = WindowManager.LayoutParams(
                screenWidth,
                blockHeight,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                // Flags: NOT_FOCUSABLE allows app interaction, but we BLOCK touches in our area
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.OPAQUE
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }
            
            windowManager?.addView(blockingOverlay, params)
            
            // Start collapse monitoring
            startCollapseMonitoring()
            
            Log.d(TAG, "STATUS BAR BLOCKER OVERLAY ADDED - height=$blockHeight")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add blocking overlay", e)
        }
    }
    
    fun removeBlockingOverlay() {
        isBlocking = false
        stopCollapseMonitoring()
        
        try {
            blockingOverlay?.let {
                windowManager?.removeView(it)
                blockingOverlay = null
                Log.d(TAG, "Blocking overlay removed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove blocking overlay", e)
        }
    }
    
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return if (result > 0) result else 100
    }
    
    private var collapseRunnable: Runnable? = null
    
    private fun startCollapseMonitoring() {
        collapseRunnable = object : Runnable {
            override fun run() {
                if (isBlocking) {
                    collapseStatusBar()
                    handler.postDelayed(this, 50) // Very aggressive - 50ms
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
            val statusBarService = getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val collapse = statusBarManager.getMethod("collapsePanels")
            collapse.invoke(statusBarService)
        } catch (e: Exception) {
            try {
                val service = getSystemService("statusbar")
                val statusBarManager = Class.forName("android.app.StatusBarManager")
                val collapse = statusBarManager.getMethod("collapse")
                collapse.invoke(service)
            } catch (ex: Exception) {
                // Silently fail
            }
        }
    }
}
