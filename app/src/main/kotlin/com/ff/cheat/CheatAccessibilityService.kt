package com.ff.cheat

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class CheatAccessibilityService : AccessibilityService() {
    companion object { @Volatile var instance: CheatAccessibilityService? = null }
    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() { instance = null; super.onDestroy() }

    fun aimAt(tx: Float, ty: Float, sw: Int, sh: Int, smoothing: Float = 0.07f) {
        val cx = sw/2f; val cy = sh/2f
        val dx = tx-cx; val dy = ty-cy
        val mag = Math.sqrt((dx*dx+dy*dy).toDouble()).toFloat()
        if (mag < 6f) return
        val step = (mag*smoothing).coerceIn(3f, 45f)
        val path = Path().apply { moveTo(cx, cy); lineTo(cx+dx/mag*step, cy+dy/mag*step) }
        dispatchGesture(GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 55L)).build(), null, null)
    }
}
