package com.awab.ai.ml.processors

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.awab.ai.AwabAIApplication
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.*
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import org.json.JSONObject

class SpeechProcessor(private val context: Context) {
    
    companion object {
        private const val TAG = "SpeechProcessor"
        private const val SAMPLE_RATE = 16000
        private const val RECORDING_DURATION = 5000 // 5 seconds
    }
    
    private val modelManager = AwabAIApplication.instance.modelManager
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    
    // Vosk components
    private var voskModel: Model? = null
    private var speechService: SpeechService? = null
    private var isVoskInitialized = false
    
    suspend fun startRecording(): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "بدء التسجيل الصوتي...")
            
            val audioData = recordAudio()
            if (audioData.isNotEmpty()) {
                return@withContext processAudioData(audioData)
            }
            
            ""
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في التسجيل: ${e.message}", e)
            ""
        }
    }
    
    private suspend fun recordAudio(): FloatArray = withContext(Dispatchers.IO) {
        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "فشل في تهيئة مسجل الصوت")
                return@withContext floatArrayOf()
            }
            
            audioRecord?.startRecording()
            isRecording = true
            
            val audioData = mutableListOf<Short>()
            val buffer = ShortArray(bufferSize)
            
            val recordingJob = launch {
                while (isRecording) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        audioData.addAll(buffer.take(readSize))
                    }
                }
            }
            
            // التسجيل لمدة 5 ثوانٍ
            delay(RECORDING_DURATION.toLong())
            stopRecording()
            recordingJob.join()
            
            // تحويل البيانات إلى Float array
            audioData.map { it.toFloat() / Short.MAX_VALUE }.toFloatArray()
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تسجيل الصوت: ${e.message}", e)
            stopRecording()
            floatArrayOf()
        }
    }
    
    private fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        Log.d(TAG, "تم إيقاف التسجيل")
    }
    
    private suspend fun processAudioData(audioData: FloatArray): String {
        return try {
            // معالجة الصوت بنموذج YAMNet للتعرف على المتحدث
            val audioFeatures = modelManager.processAudio(audioData)
            
            if (audioFeatures != null) {
                // استخدام Vosk للتعرف على الكلام
                transcribeWithVosk(audioData)
            } else {
                Log.w(TAG, "فشل في معالجة البيانات الصوتية")
                ""
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في معالجة الصوت: ${e.message}", e)
            ""
        }
    }
    
    private suspend fun transcribeWithVosk(audioData: FloatArray): String = withContext(Dispatchers.IO) {
        try {
            // تحويل البيانات إلى تنسيق WAV مؤقت
            val tempFile = File(context.cacheDir, "temp_audio.wav")
            writeWavFile(tempFile, audioData)
            
            // استخدام مكتبة Vosk (ستحتاج لدمج مكتبة Vosk الأصلية)
            // هذا مثال مبسط - في التطبيق الحقيقي نحتاج لدمج Vosk JNI
            val transcription = processWithVoskModel(tempFile)
            
            // حذف الملف المؤقت
            tempFile.delete()
            
            transcription
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في التحويل النصي: ${e.message}", e)
            ""
        }
    }
    
    private fun writeWavFile(file: File, audioData: FloatArray) {
        try {
            val output = FileOutputStream(file)
            
            // كتابة WAV header
            val header = ByteArray(44)
            val byteBuffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            
            byteBuffer.put("RIFF".toByteArray())
            byteBuffer.putInt(36 + audioData.size * 2)
            byteBuffer.put("WAVE".toByteArray())
            byteBuffer.put("fmt ".toByteArray())
            byteBuffer.putInt(16) // PCM format
            byteBuffer.putShort(1) // AudioFormat
            byteBuffer.putShort(1) // Channels
            byteBuffer.putInt(SAMPLE_RATE)
            byteBuffer.putInt(SAMPLE_RATE * 2)
            byteBuffer.putShort(2) // Block align
            byteBuffer.putShort(16) // Bits per sample
            byteBuffer.put("data".toByteArray())
            byteBuffer.putInt(audioData.size * 2)
            
            output.write(header)
            
            // كتابة البيانات الصوتية
            for (sample in audioData) {
                val shortValue = (sample * Short.MAX_VALUE).toInt().toShort()
                output.write(shortValue.toInt() and 0xFF)
                output.write((shortValue.toInt() shr 8) and 0xFF)
            }
            
            output.close()
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في كتابة ملف WAV: ${e.message}", e)
        }
    }
    
    private suspend fun processWithVoskModel(audioFile: File): String = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!isVoskInitialized) {
                initializeVosk()
            }
            
            if (voskModel == null) {
                Log.w(TAG, "نموذج Vosk غير مهيأ")
                return@withContext "عذراً، نموذج التعرف على الكلام غير متاح"
            }
            
            // استخدام Vosk للتعرف على الكلام
            recognizeWithVosk(audioFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في معالجة Vosk: ${e.message}", e)
            ""
        }
    }
    
    private suspend fun initializeVosk() = withContext(Dispatchers.IO) {
        try {
            val voskModelDir = File(context.filesDir, "models/vosk")
            if (voskModelDir.exists()) {
                voskModel = Model(voskModelDir.absolutePath)
                isVoskInitialized = true
                Log.d(TAG, "تم تهيئة نموذج Vosk")
            } else {
                Log.w(TAG, "مجلد نموذج Vosk غير موجود")
            }
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تهيئة Vosk: ${e.message}", e)
        }
    }
    
    private suspend fun recognizeWithVosk(audioFile: File): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val recognizer = Recognizer(voskModel, SAMPLE_RATE.toFloat())
            val audioBytes = audioFile.readBytes()
            
            // معالجة البيانات الصوتية
            recognizer.acceptWaveForm(audioBytes, audioBytes.size)
            val result = recognizer.finalResult
            
            // تحليل النتيجة JSON
            val jsonResult = JSONObject(result)
            val recognizedText = jsonResult.optString("text", "")
            
            recognizer.close()
            
            if (recognizedText.isNotEmpty()) {
                Log.d(TAG, "نص محول: $recognizedText")
                recognizedText
            } else {
                "لم أتمكن من فهم ما قلته. يرجى المحاولة مرة أخرى."
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في التعرف على الكلام: ${e.message}", e)
            "حدث خطأ في التعرف على الكلام"
        }
    }
    
    private fun simulateArabicSpeechRecognition(audioFile: File): String {
        // محاكاة بسيطة للتعرف على الكلام
        // في التطبيق الحقيقي، سنستخدم Vosk للتعرف الفعلي
        val commonPhrases = listOf(
            "مرحبا",
            "كيف حالك",
            "ما الوقت",
            "اتصل بـ",
            "أرسل رسالة",
            "افتح التطبيق",
            "ما الطقس اليوم"
        )
        
        // إرجاع عبارة عشوائية للمحاكاة
        return commonPhrases.random()
    }
    
    fun analyzeSpeakerIdentity(audioData: FloatArray): String? {
        return try {
            val features = modelManager.processAudio(audioData)
            if (features != null) {
                // مقارنة مع الأصوات المحفوظة
                compareWithStoredVoices(features)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تحليل هوية المتحدث: ${e.message}", e)
            null
        }
    }
    
    private fun compareWithStoredVoices(features: FloatArray): String? {
        // مقارنة مع قاعدة البيانات للأصوات المحفوظة
        // هذا مثال مبسط - في التطبيق الحقيقي نحتاج خوارزمية مقارنة متقدمة
        return "مستخدم معروف" // أو null إذا لم يتم التعرف عليه
    }
}