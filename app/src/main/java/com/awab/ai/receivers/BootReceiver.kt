package com.awab.ai.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.awab.ai.services.AIAssistantService

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "تم استقبال حدث: ${intent.action}")
                
                // بدء خدمة المساعد الذكي
                val serviceIntent = Intent(context, AIAssistantService::class.java)
                context.startForegroundService(serviceIntent)
                
                Log.d(TAG, "تم بدء خدمة المساعد الذكي تلقائياً")
            }
        }
    }
}