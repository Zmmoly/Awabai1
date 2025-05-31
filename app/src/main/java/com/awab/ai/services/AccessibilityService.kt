package com.awab.ai.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*

class AccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "AccessibilityService"
        private var instance: AccessibilityService? = null
        
        fun getInstance(): AccessibilityService? = instance
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "تم توصيل خدمة إمكانية الوصول")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // معالجة أحداث إمكانية الوصول
        event?.let {
            when (it.eventType) {
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    Log.d(TAG, "تم النقر على عنصر: ${it.contentDescription}")
                }
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    Log.d(TAG, "تغيير حالة النافذة: ${it.packageName}")
                }
            }
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "تم مقاطعة خدمة إمكانية الوصول")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
        Log.d(TAG, "تم إنهاء خدمة إمكانية الوصول")
    }
    
    fun performClick(x: Float, y: Float) {
        serviceScope.launch {
            try {
                val path = Path().apply {
                    moveTo(x, y)
                }
                
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                    .build()
                
                dispatchGesture(gesture, object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        Log.d(TAG, "تم تنفيذ النقر بنجاح")
                    }
                    
                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        Log.d(TAG, "تم إلغاء النقر")
                    }
                }, null)
                
            } catch (e: Exception) {
                Log.e(TAG, "خطأ في تنفيذ النقر: ${e.message}", e)
            }
        }
    }
    
    fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float) {
        serviceScope.launch {
            try {
                val path = Path().apply {
                    moveTo(startX, startY)
                    lineTo(endX, endY)
                }
                
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
                    .build()
                
                dispatchGesture(gesture, null, null)
                Log.d(TAG, "تم تنفيذ التمرير")
                
            } catch (e: Exception) {
                Log.e(TAG, "خطأ في تنفيذ التمرير: ${e.message}", e)
            }
        }
    }
    
    fun findAndClickElement(text: String): Boolean {
        return try {
            val rootNode = rootInActiveWindow ?: return false
            val targetNode = findNodeByText(rootNode, text)
            
            if (targetNode != null) {
                targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "تم النقر على العنصر: $text")
                true
            } else {
                Log.d(TAG, "لم يتم العثور على العنصر: $text")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في البحث والنقر: ${e.message}", e)
            false
        }
    }
    
    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true ||
            node.contentDescription?.toString()?.contains(text, ignoreCase = true) == true) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val result = findNodeByText(child, text)
                if (result != null) return result
            }
        }
        
        return null
    }
    
    fun performGoBack(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_BACK)
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في الرجوع: ${e.message}", e)
            false
        }
    }
    
    fun performGoHome(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_HOME)
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في الذهاب للرئيسية: ${e.message}", e)
            false
        }
    }
    
    fun getActiveAppPackage(): String? {
        return try {
            rootInActiveWindow?.packageName?.toString()
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في الحصول على التطبيق النشط: ${e.message}", e)
            null
        }
    }
}