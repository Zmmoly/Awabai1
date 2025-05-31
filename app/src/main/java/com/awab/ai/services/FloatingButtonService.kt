package com.awab.ai.services

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.ImageButton
import androidx.core.app.NotificationCompat
import com.awab.ai.R
import com.awab.ai.MainActivity

class FloatingButtonService : Service() {
    
    companion object {
        private const val TAG = "FloatingButtonService"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "floating_button_service"
    }
    
    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "بدء خدمة الزر العائم")
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        createFloatingButton()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        removeFloatingButton()
        Log.d(TAG, "إيقاف خدمة الزر العائم")
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "خدمة الزر العائم",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "زر المساعد الذكي العائم"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("الزر العائم نشط")
            .setContentText("اضغط على الزر العائم للوصول السريع")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun createFloatingButton() {
        try {
            floatingView = LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null)
            
            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 100
            }
            
            windowManager.addView(floatingView, layoutParams)
            
            // إعداد الأحداث
            setupFloatingButtonEvents(layoutParams)
            
            Log.d(TAG, "تم إنشاء الزر العائم")
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في إنشاء الزر العائم: ${e.message}", e)
        }
    }
    
    private fun setupFloatingButtonEvents(layoutParams: WindowManager.LayoutParams) {
        val floatingButton = floatingView?.findViewById<ImageButton>(R.id.floating_button)
        
        floatingButton?.setOnClickListener {
            // فتح التطبيق الرئيسي
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        }
        
        // إضافة إمكانية السحب
        floatingButton?.setOnTouchListener(object : View.OnTouchListener {
            private var lastAction = 0
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        lastAction = event.action
                        return true
                    }
                    
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, layoutParams)
                        lastAction = event.action
                        return true
                    }
                    
                    MotionEvent.ACTION_UP -> {
                        if (lastAction == MotionEvent.ACTION_DOWN) {
                            // نقرة عادية
                            v?.performClick()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }
    
    private fun removeFloatingButton() {
        floatingView?.let {
            try {
                windowManager.removeView(it)
                floatingView = null
                Log.d(TAG, "تم إزالة الزر العائم")
            } catch (e: Exception) {
                Log.e(TAG, "خطأ في إزالة الزر العائم: ${e.message}", e)
            }
        }
    }
}