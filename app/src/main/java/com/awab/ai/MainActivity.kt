package com.awab.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.awab.ai.databinding.ActivityMainBinding
import com.awab.ai.services.AIAssistantService
import com.awab.ai.services.FloatingButtonService
import com.awab.ai.ui.adapters.ChatAdapter
import com.awab.ai.data.models.ChatMessage
import com.awab.ai.ml.ConversationManager
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var conversationManager: ConversationManager
    private val chatMessages = mutableListOf<ChatMessage>()
    
    companion object {
        private const val REQUEST_PERMISSIONS = 1001
        private const val REQUEST_OVERLAY_PERMISSION = 1002
        private const val REQUEST_ACCESSIBILITY_PERMISSION = 1003
        
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // تهيئة المكونات
        initializeComponents()
        
        // إعداد واجهة المستخدم
        setupUI()
        
        // طلب الأذونات
        requestPermissions()
        
        // بدء الخدمات
        startServices()
    }
    
    private fun initializeComponents() {
        conversationManager = ConversationManager(this)
        
        // إعداد قائمة المحادثة
        chatAdapter = ChatAdapter(chatMessages)
        binding.recyclerViewChat.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = chatAdapter
        }
        
        // إضافة رسالة ترحيب
        addWelcomeMessage()
    }
    
    private fun setupUI() {
        // زر الإرسال
        binding.buttonSend.setOnClickListener {
            val userMessage = binding.editTextMessage.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                sendMessage(userMessage)
                binding.editTextMessage.text.clear()
            }
        }
        
        // زر التسجيل الصوتي
        binding.buttonVoice.setOnClickListener {
            startVoiceRecording()
        }
        
        // زر الكاميرا
        binding.buttonCamera.setOnClickListener {
            openCamera()
        }
        
        // زر الإعدادات
        binding.buttonSettings.setOnClickListener {
            openSettings()
        }
    }
    
    private fun addWelcomeMessage() {
        val welcomeMessage = ChatMessage(
            text = "مرحباً! أنا عواب، مساعدك الذكي الشخصي. كيف يمكنني مساعدتك اليوم؟",
            isFromUser = false,
            timestamp = System.currentTimeMillis()
        )
        chatMessages.add(welcomeMessage)
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        binding.recyclerViewChat.scrollToPosition(chatMessages.size - 1)
    }
    
    private fun sendMessage(text: String) {
        // إضافة رسالة المستخدم
        val userMessage = ChatMessage(
            text = text,
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        )
        chatMessages.add(userMessage)
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        
        // الحصول على رد من المساعد الذكي
        conversationManager.processMessage(text) { response ->
            runOnUiThread {
                val aiMessage = ChatMessage(
                    text = response,
                    isFromUser = false,
                    timestamp = System.currentTimeMillis()
                )
                chatMessages.add(aiMessage)
                chatAdapter.notifyItemInserted(chatMessages.size - 1)
                binding.recyclerViewChat.scrollToPosition(chatMessages.size - 1)
            }
        }
        
        binding.recyclerViewChat.scrollToPosition(chatMessages.size - 1)
    }
    
    private fun startVoiceRecording() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.RECORD_AUDIO)) {
            // بدء تسجيل الصوت
            conversationManager.startVoiceRecording { transcribedText ->
                runOnUiThread {
                    if (transcribedText.isNotEmpty()) {
                        sendMessage(transcribedText)
                    }
                }
            }
        } else {
            requestAudioPermission()
        }
    }
    
    private fun openCamera() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
            // فتح الكاميرا
            conversationManager.openCamera { imageDescription ->
                runOnUiThread {
                    if (imageDescription.isNotEmpty()) {
                        val message = "تم تحليل الصورة: $imageDescription"
                        sendMessage(message)
                    }
                }
            }
        } else {
            requestCameraPermission()
        }
    }
    
    private fun openSettings() {
        // فتح شاشة الإعدادات
        Toast.makeText(this, "إعدادات التطبيق", Toast.LENGTH_SHORT).show()
    }
    
    private fun requestPermissions() {
        if (!EasyPermissions.hasPermissions(this, *REQUIRED_PERMISSIONS)) {
            EasyPermissions.requestPermissions(
                PermissionRequest.Builder(this, REQUEST_PERMISSIONS, *REQUIRED_PERMISSIONS)
                    .setRationale("التطبيق يحتاج هذه الأذونات للعمل بشكل صحيح")
                    .setPositiveButtonText("موافق")
                    .setNegativeButtonText("إلغاء")
                    .build()
            )
        }
        
        // طلب أذونات خاصة
        requestOverlayPermission()
        requestAccessibilityPermission()
    }
    
    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
        }
    }
    
    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivityForResult(intent, REQUEST_ACCESSIBILITY_PERMISSION)
    }
    
    private fun requestAudioPermission() {
        EasyPermissions.requestPermissions(
            this,
            "التطبيق يحتاج إذن الميكروفون للتعرف على الصوت",
            REQUEST_PERMISSIONS,
            Manifest.permission.RECORD_AUDIO
        )
    }
    
    private fun requestCameraPermission() {
        EasyPermissions.requestPermissions(
            this,
            "التطبيق يحتاج إذن الكاميرا لتحليل الصور",
            REQUEST_PERMISSIONS,
            Manifest.permission.CAMERA
        )
    }
    
    private fun startServices() {
        // بدء خدمة المساعد الذكي
        val aiServiceIntent = Intent(this, AIAssistantService::class.java)
        startForegroundService(aiServiceIntent)
        
        // بدء خدمة الزر العائم
        if (Settings.canDrawOverlays(this)) {
            val floatingServiceIntent = Intent(this, FloatingButtonService::class.java)
            startService(floatingServiceIntent)
        }
    }
    
    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(this, "تم منح الأذونات بنجاح", Toast.LENGTH_SHORT).show()
    }
    
    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(this, "بعض الأذونات مطلوبة لعمل التطبيق بشكل كامل", Toast.LENGTH_LONG).show()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_OVERLAY_PERMISSION -> {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "تم منح إذن النافذة العائمة", Toast.LENGTH_SHORT).show()
                    startServices()
                }
            }
            REQUEST_ACCESSIBILITY_PERMISSION -> {
                Toast.makeText(this, "يرجى تفعيل خدمة إمكانية الوصول للتطبيق", Toast.LENGTH_LONG).show()
            }
        }
    }
}