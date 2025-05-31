package com.awab.ai.ml

import android.content.Context
import android.util.Log
import com.awab.ai.data.models.ConversationContext
import com.awab.ai.ml.processors.*
import com.awab.ai.network.HuggingFaceAPI
import com.awab.ai.utils.PreferencesManager
import kotlinx.coroutines.*
import java.util.*

class ConversationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ConversationManager"
    }
    
    private val modelManager = AwabAIApplication.instance.modelManager
    private val preferencesManager = AwabAIApplication.instance.preferencesManager
    private val huggingFaceAPI = HuggingFaceAPI()
    
    // معالجات متخصصة
    private val speechProcessor = SpeechProcessor(context)
    private val imageProcessor = ImageProcessor(context)
    private val faceProcessor = FaceProcessor(context)
    private val textProcessor = TextProcessor(context)
    
    // سياق المحادثة
    private var conversationContext = ConversationContext()
    private val conversationHistory = mutableListOf<Pair<String, String>>()
    
    // معالجة الرسائل النصية
    fun processMessage(message: String, callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = generateResponse(message)
                
                // إضافة إلى تاريخ المحادثة
                conversationHistory.add(Pair(message, response))
                
                // تحديث سياق المحادثة
                updateConversationContext(message, response)
                
                callback(response)
                
            } catch (e: Exception) {
                Log.e(TAG, "خطأ في معالجة الرسالة: ${e.message}", e)
                callback("عذراً، حدث خطأ أثناء معالجة رسالتك. يرجى المحاولة مرة أخرى.")
            }
        }
    }
    
    private suspend fun generateResponse(message: String): String {
        // تحليل نوع الرسالة
        val messageType = analyzeMessageType(message)
        
        return when (messageType) {
            MessageType.PROGRAMMING -> handleProgrammingQuery(message)
            MessageType.MEDICAL -> handleMedicalQuery(message)
            MessageType.GENERAL -> handleGeneralQuery(message)
            MessageType.DEVICE_CONTROL -> handleDeviceControl(message)
            MessageType.SEARCH -> handleSearchQuery(message)
        }
    }
    
    private fun analyzeMessageType(message: String): MessageType {
        val lowerMessage = message.lowercase()
        
        return when {
            containsProgrammingKeywords(lowerMessage) -> MessageType.PROGRAMMING
            containsMedicalKeywords(lowerMessage) -> MessageType.MEDICAL
            containsDeviceControlKeywords(lowerMessage) -> MessageType.DEVICE_CONTROL
            containsSearchKeywords(lowerMessage) -> MessageType.SEARCH
            else -> MessageType.GENERAL
        }
    }
    
    private fun containsProgrammingKeywords(message: String): Boolean {
        val programmingKeywords = listOf(
            "كود", "برمجة", "برنامج", "تطبيق", "خوارزمية", "دالة", "متغير", 
            "كلاس", "كوتلن", "جافا", "بايثون", "javascript", "code", "function",
            "class", "variable", "algorithm", "programming"
        )
        return programmingKeywords.any { message.contains(it) }
    }
    
    private fun containsMedicalKeywords(message: String): Boolean {
        val medicalKeywords = listOf(
            "طبي", "دواء", "مرض", "علاج", "أعراض", "تشخيص", "صحة", "طبيب",
            "مستشفى", "عيادة", "حبوب", "دوار", "ألم", "صداع", "حمى"
        )
        return medicalKeywords.any { message.contains(it) }
    }
    
    private fun containsDeviceControlKeywords(message: String): Boolean {
        val controlKeywords = listOf(
            "افتح", "أغلق", "شغل", "أطفئ", "اتصل", "أرسل رسالة", "صوت", "سطوع",
            "واي فاي", "بلوتوث", "كاميرا", "منبه", "موسيقى"
        )
        return controlKeywords.any { message.contains(it) }
    }
    
    private fun containsSearchKeywords(message: String): Boolean {
        val searchKeywords = listOf(
            "ابحث", "ما هو", "كيف", "أين", "متى", "لماذا", "معلومات عن", "بحث"
        )
        return searchKeywords.any { message.contains(it) }
    }
    
    private suspend fun handleProgrammingQuery(message: String): String {
        // استخدام Hugging Face API للاستعلامات البرمجية
        return huggingFaceAPI.queryFalcon40B(
            buildProgrammingPrompt(message)
        ) ?: "عذراً، لا أستطيع الوصول إلى خدمة الاستعلامات البرمجية حالياً."
    }
    
    private suspend fun handleMedicalQuery(message: String): String {
        if (!modelManager.areModelsReady()) {
            return "عذراً، النماذج الطبية قيد التحميل. يرجى الانتظار قليلاً."
        }
        
        // معالجة النص بـ Bio-ClinicalBERT
        val embeddings = modelManager.processText(message, isMedical = true)
        
        return if (embeddings != null) {
            val response = textProcessor.processMedicalQuery(message, embeddings)
            "$response\n\n⚠️ تنبيه: هذه المعلومات للاستفسار العام فقط وليست استشارة طبية. يرجى استشارة طبيب مختص."
        } else {
            "عذراً، لا أستطيع تحليل استفسارك الطبي حالياً. يرجى المحاولة لاحقاً."
        }
    }
    
    private suspend fun handleGeneralQuery(message: String): String {
        if (!modelManager.areModelsReady()) {
            return "عذراً، النماذج قيد التحميل. يرجى الانتظار قليلاً."
        }
        
        // معالجة النص بـ CamelBERT
        val embeddings = modelManager.processText(message, isMedical = false)
        
        return if (embeddings != null) {
            textProcessor.processGeneralQuery(message, embeddings, conversationContext)
        } else {
            "عذراً، لا أستطيع فهم رسالتك حالياً. يرجى إعادة صياغتها."
        }
    }
    
    private suspend fun handleDeviceControl(message: String): String {
        // تنفيذ أوامر التحكم في الجهاز
        return try {
            val deviceController = DeviceController(context)
            deviceController.executeCommand(message)
        } catch (e: Exception) {
            "عذراً، لا أستطيع تنفيذ هذا الأمر حالياً. تأكد من أن التطبيق له الأذونات المطلوبة."
        }
    }
    
    private suspend fun handleSearchQuery(message: String): String {
        // البحث في الإنترنت (مع الحفاظ على الخصوصية)
        return try {
            val searchProcessor = SearchProcessor(context)
            searchProcessor.performSearch(message)
        } catch (e: Exception) {
            "عذراً، لا أستطيع البحث حالياً. تأكد من الاتصال بالإنترنت."
        }
    }
    
    private fun buildProgrammingPrompt(message: String): String {
        val context = if (conversationHistory.isNotEmpty()) {
            "السياق: ${conversationHistory.takeLast(3).joinToString("\n") { "س: ${it.first}\nج: ${it.second}" }}\n\n"
        } else ""
        
        return """${context}أنت مساعد ذكي متخصص في البرمجة. أجب على السؤال التالي بشكل مفصل ومفيد:

السؤال: $message

يرجى تقديم إجابة شاملة تتضمن:
1. شرح مفصل للموضوع
2. أمثلة عملية مع الكود إن أمكن
3. نصائح إضافية للتطوير
4. أفضل الممارسات

الإجابة:"""
    }
    
    // معالجة التسجيل الصوتي
    fun startVoiceRecording(callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val transcribedText = speechProcessor.startRecording()
                callback(transcribedText)
            } catch (e: Exception) {
                Log.e(TAG, "خطأ في التسجيل الصوتي: ${e.message}", e)
                callback("")
            }
        }
    }
    
    // معالجة الكاميرا
    fun openCamera(callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val imageDescription = imageProcessor.captureAndAnalyze()
                callback(imageDescription)
            } catch (e: Exception) {
                Log.e(TAG, "خطأ في معالجة الكاميرا: ${e.message}", e)
                callback("")
            }
        }
    }
    
    private fun updateConversationContext(userMessage: String, aiResponse: String) {
        conversationContext.apply {
            lastUserMessage = userMessage
            lastAIResponse = aiResponse
            messageCount++
            lastInteractionTime = System.currentTimeMillis()
            
            // تحديث المواضيع المطروحة
            val topics = extractTopics(userMessage)
            discussedTopics.addAll(topics)
            
            // الاحتفاظ بآخر 50 موضوع فقط
            if (discussedTopics.size > 50) {
                discussedTopics = discussedTopics.takeLast(50).toMutableSet()
            }
        }
    }
    
    private fun extractTopics(message: String): List<String> {
        // استخراج المواضيع الرئيسية من الرسالة
        val words = message.split(" ").filter { it.length > 3 }
        return words.distinct().take(5)
    }
    
    fun clearConversationHistory() {
        conversationHistory.clear()
        conversationContext = ConversationContext()
    }
    
    enum class MessageType {
        PROGRAMMING, MEDICAL, GENERAL, DEVICE_CONTROL, SEARCH
    }
}