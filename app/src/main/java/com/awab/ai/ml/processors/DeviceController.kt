package com.awab.ai.ml.processors

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import com.awab.ai.services.AccessibilityService

class DeviceController(private val context: Context) {
    
    companion object {
        private const val TAG = "DeviceController"
    }
    
    suspend fun executeCommand(command: String): String {
        val lowerCommand = command.lowercase()
        
        return when {
            lowerCommand.contains("اتصل") -> handlePhoneCall(command)
            lowerCommand.contains("أرسل رسالة") -> handleSendSMS(command)
            lowerCommand.contains("افتح") -> handleOpenApp(command)
            lowerCommand.contains("سطوع") -> handleBrightness(command)
            lowerCommand.contains("صوت") -> handleVolume(command)
            lowerCommand.contains("واي فاي") -> handleWiFi(command)
            lowerCommand.contains("بلوتوث") -> handleBluetooth(command)
            else -> "لم أتمكن من فهم الأمر. يمكنك المحاولة مرة أخرى؟"
        }
    }
    
    private fun handlePhoneCall(command: String): String {
        return try {
            val phoneNumber = extractPhoneNumber(command)
            if (phoneNumber.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                "جاري الاتصال بـ $phoneNumber"
            } else {
                "لم أتمكن من استخراج رقم الهاتف. يرجى ذكر الرقم بوضوح."
            }
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في الاتصال: ${e.message}", e)
            "فشل في إجراء المكالمة. تأكد من الأذونات المطلوبة."
        }
    }
    
    private fun handleSendSMS(command: String): String {
        return try {
            val phoneNumber = extractPhoneNumber(command)
            val message = extractSMSMessage(command)
            
            if (phoneNumber.isNotEmpty() && message.isNotEmpty()) {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                "تم إرسال الرسالة إلى $phoneNumber"
            } else {
                "يرجى تحديد رقم الهاتف والرسالة. مثال: أرسل رسالة إلى 123456789 نص الرسالة"
            }
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في إرسال الرسالة: ${e.message}", e)
            "فشل في إرسال الرسالة. تأكد من الأذونات المطلوبة."
        }
    }
    
    private fun handleOpenApp(command: String): String {
        return try {
            val appName = extractAppName(command)
            val packageName = getPackageNameForApp(appName)
            
            if (packageName.isNotEmpty()) {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    "تم فتح تطبيق $appName"
                } else {
                    "التطبيق غير مثبت على الجهاز"
                }
            } else {
                "لم أتمكن من العثور على التطبيق المطلوب"
            }
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في فتح التطبيق: ${e.message}", e)
            "فشل في فتح التطبيق"
        }
    }
    
    private fun handleBrightness(command: String): String {
        return try {
            val brightnessLevel = extractBrightnessLevel(command)
            if (brightnessLevel in 0..100) {
                val systemBrightness = (brightnessLevel * 255 / 100).coerceIn(0, 255)
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    systemBrightness
                )
                "تم تعديل السطوع إلى $brightnessLevel%"
            } else {
                "يرجى تحديد مستوى السطوع من 0 إلى 100"
            }
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تعديل السطوع: ${e.message}", e)
            "فشل في تعديل السطوع. تأكد من أذونات تعديل إعدادات النظام."
        }
    }
    
    private fun handleVolume(command: String): String {
        return try {
            val volumeLevel = extractVolumeLevel(command)
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            
            if (volumeLevel in 0..100) {
                val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                val targetVolume = (volumeLevel * maxVolume / 100).coerceIn(0, maxVolume)
                
                audioManager.setStreamVolume(
                    android.media.AudioManager.STREAM_MUSIC,
                    targetVolume,
                    0
                )
                "تم تعديل مستوى الصوت إلى $volumeLevel%"
            } else {
                "يرجى تحديد مستوى الصوت من 0 إلى 100"
            }
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تعديل الصوت: ${e.message}", e)
            "فشل في تعديل مستوى الصوت"
        }
    }
    
    private fun handleWiFi(command: String): String {
        return try {
            val action = if (command.contains("شغل") || command.contains("فعل")) {
                "تفعيل"
            } else if (command.contains("أطفئ") || command.contains("إلغاء")) {
                "إلغاء تفعيل"
            } else {
                return "يرجى تحديد ما إذا كنت تريد تشغيل أو إطفاء الواي فاي"
            }
            
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            
            "تم فتح إعدادات الواي فاي لـ $action"
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في إعدادات الواي فاي: ${e.message}", e)
            "فشل في الوصول لإعدادات الواي فاي"
        }
    }
    
    private fun handleBluetooth(command: String): String {
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            
            "تم فتح إعدادات البلوتوث"
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في إعدادات البلوتوث: ${e.message}", e)
            "فشل في الوصول لإعدادات البلوتوث"
        }
    }
    
    private fun extractPhoneNumber(command: String): String {
        val phoneRegex = Regex("""(\d{3,15})""")
        val match = phoneRegex.find(command)
        return match?.value ?: ""
    }
    
    private fun extractSMSMessage(command: String): String {
        val parts = command.split("رسالة")
        return if (parts.size > 1) {
            parts.last().trim()
        } else ""
    }
    
    private fun extractAppName(command: String): String {
        val appKeywords = mapOf(
            "واتساب" to "whatsapp",
            "فيسبوك" to "facebook",
            "إنستغرام" to "instagram",
            "يوتيوب" to "youtube",
            "تويتر" to "twitter",
            "تليجرام" to "telegram",
            "كاميرا" to "camera",
            "معرض" to "gallery",
            "موسيقى" to "music",
            "متجر" to "play store"
        )
        
        for ((arabic, english) in appKeywords) {
            if (command.contains(arabic)) {
                return english
            }
        }
        
        return ""
    }
    
    private fun getPackageNameForApp(appName: String): String {
        return when (appName.lowercase()) {
            "whatsapp" -> "com.whatsapp"
            "facebook" -> "com.facebook.katana"
            "instagram" -> "com.instagram.android"
            "youtube" -> "com.google.android.youtube"
            "twitter" -> "com.twitter.android"
            "telegram" -> "org.telegram.messenger"
            "camera" -> "com.android.camera2"
            "gallery" -> "com.google.android.apps.photos"
            "music" -> "com.google.android.music"
            "play store" -> "com.android.vending"
            else -> ""
        }
    }
    
    private fun extractBrightnessLevel(command: String): Int {
        val numberRegex = Regex("""(\d+)""")
        val match = numberRegex.find(command)
        return match?.value?.toIntOrNull() ?: -1
    }
    
    private fun extractVolumeLevel(command: String): Int {
        val numberRegex = Regex("""(\d+)""")
        val match = numberRegex.find(command)
        return match?.value?.toIntOrNull() ?: -1
    }
}