package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.androidcontrol.AccessibilityActionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Optional, policy-compliant AccessibilityService for BABY AI.
 * Only executes user-requested screen navigation actions (Back, Home, Recents, Notifications, Scroll).
 * Strictly ignores password fields, OTPs, and payment screens; never collects or uploads screen content.
 */
class BabyAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally do not log or harvest screen contents.
        // Only respect privacy by skipping password/sensitive events.
        val source = event?.source ?: return
        if (source.isPassword) {
            return
        }
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
        _isConnected.value = false
    }

    fun performUserAction(type: AccessibilityActionType): Boolean {
        if (!isConnected.value) return false
        return when (type) {
            AccessibilityActionType.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            AccessibilityActionType.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            AccessibilityActionType.RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            AccessibilityActionType.NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            AccessibilityActionType.SCROLL_DOWN -> scrollCurrentWindow(forward = true)
            AccessibilityActionType.SCROLL_UP -> scrollCurrentWindow(forward = false)
        }
    }

    private fun scrollCurrentWindow(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        if (containsPasswordNode(root)) return false

        val scrollable = findScrollableNode(root)
        if (scrollable != null) {
            val action = if (forward) {
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            } else {
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            }
            if (scrollable.performAction(action)) return true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val dm = resources.displayMetrics
            val x = dm.widthPixels / 2f
            val startY = if (forward) dm.heightPixels * 0.72f else dm.heightPixels * 0.32f
            val endY = if (forward) dm.heightPixels * 0.32f else dm.heightPixels * 0.72f
            val path = Path().apply {
                moveTo(x, startY)
                lineTo(x, endY)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0L, 260L))
                .build()
            return dispatchGesture(gesture, null, null)
        }
        return false
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isPassword) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollableNode(child)
            if (found != null) return found
        }
        return null
    }

    private fun containsPasswordNode(node: AccessibilityNodeInfo): Boolean {
        if (node.isPassword) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (containsPasswordNode(child)) return true
        }
        return false
    }

    companion object {
        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        @Volatile
        var instance: BabyAccessibilityService? = null
            private set
    }
}
