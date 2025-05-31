package com.awab.ai.services

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.awab.ai.R
import com.awab.ai.MainActivity
import com.awab.ai.ml.ConversationManager
import kotlinx.coroutines.*

class AIAssistantService : Service() {
    
    companion object {
        private const val TAG = "AIAssistantService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "awab_ai_service"
    }
    
    private lateinit var conversationManager: ConversationManager
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "بدء خدمة المساعد الذكي")
        
        conversationManager = ConversationManager(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        // تهيئة المكونات في الخلفية
        serviceScope.launch {
            initializeServiceComponents()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "تشغيل خدمة المساعد الذكي")
        return START_STICKY // إعادة تشغيل الخدمة إذا تم إيقافها
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "إيقاف خدمة المساعد الذكي")
        serviceScope.cancel()
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "خدمة المساعد الذكي عواب",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "خدمة المساعد الذكي تعمل في الخلفية"
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
            .setContentTitle("المساعد الذكي عواب")
            .setContentText("جاهز لمساعدتك")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }
    
    private suspend fun initializeServiceComponents() {
        try {
            Log.d(TAG, "تهيئة مكونات الخدمة...")
            
            // تهيئة النماذج
            val modelManager = com.awab.ai.AwabAIApplication.instance.modelManager
            if (!modelManager.areModelsReady()) {
                Log.d(TAG, "انتظار تهيئة النماذج...")
                // انتظار تهيئة النماذج
                while (!modelManager.areModelsReady()) {
                    delay(1000)
                }
            }
            
            Log.d(TAG, "تم تهيئة جميع المكونات بنجاح")
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تهيئة المكونات: ${e.message}", e)
        }
    }
}