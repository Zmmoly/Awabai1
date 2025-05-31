package com.awab.ai.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlinx.coroutines.*

class ModelManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ModelManager"
        
        // روابط النماذج
        private const val FACENET_URL = "https://github.com/Zmmoly/zamouli-ai-assistant-520319/releases/download/1/facenet.tflite"
        private const val MOBILENET_URL = "https://github.com/Zmmoly/zamouli-ai-assistant-520319/releases/download/1/mobilenet_v1.1.tflite"
        private const val YAMNET_URL = "https://github.com/Zmmoly/zamouli-ai-assistant-520319/releases/download/1/voxceleb_ECAPA512.onnx"
        private const val CAMELBERT_URL = "https://github.com/Zmmoly/camelbert-tflite/releases/download/v1.0/camelbert-mix.tflite"
        private const val VOCAB_URL = "https://github.com/Zmmoly/camelbert-tflite/releases/download/v1.0/vocab.txt"
        private const val BIO_BERT_URL = "https://github.com/Zmmoly/zamouli-ai-assistant-520319/releases/download/1/bio_clinical_bert.tflite"
        private const val BIO_VOCAB_URL = "https://github.com/Zmmoly/zamouli-ai-assistant-520319/releases/download/1/vocab.1.txt"
        private const val VOSK_MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-ar-0.22-linto-1.1.0.zip"
    }
    
    // TensorFlow Lite models
    private var faceNetInterpreter: Interpreter? = null
    private var mobileNetInterpreter: Interpreter? = null
    private var camelBertInterpreter: Interpreter? = null
    private var bioBertInterpreter: Interpreter? = null
    
    // ONNX model
    private var ortEnvironment: OrtEnvironment? = null
    private var yamNetSession: OrtSession? = null
    
    // Model states
    private var modelsInitialized = false
    private val modelDownloader = ModelDownloader(context)
    
    fun initializeModels() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "بدء تهيئة النماذج...")
                
                // تحميل النماذج إذا لم تكن موجودة
                downloadModelsIfNeeded()
                
                // تهيئة TensorFlow Lite models
                initializeTensorFlowModels()
                
                // تهيئة ONNX model
                initializeOnnxModels()
                
                modelsInitialized = true
                Log.d(TAG, "تم تهيئة جميع النماذج بنجاح")
                
            } catch (e: Exception) {
                Log.e(TAG, "خطأ في تهيئة النماذج: ${e.message}", e)
            }
        }
    }
    
    private suspend fun downloadModelsIfNeeded() {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        
        // قائمة النماذج للتحميل
        val modelsToDownload = listOf(
            Pair("facenet.tflite", FACENET_URL),
            Pair("mobilenet.tflite", MOBILENET_URL),
            Pair("yamnet.onnx", YAMNET_URL),
            Pair("camelbert.tflite", CAMELBERT_URL),
            Pair("vocab.txt", VOCAB_URL),
            Pair("bio_bert.tflite", BIO_BERT_URL),
            Pair("bio_vocab.txt", BIO_VOCAB_URL)
        )
        
        for ((fileName, url) in modelsToDownload) {
            val file = File(modelsDir, fileName)
            if (!file.exists()) {
                Log.d(TAG, "تحميل النموذج: $fileName")
                modelDownloader.downloadModel(url, file)
            }
        }
        
        // تحميل نموذج Vosk (مضغوط)
        val voskZipFile = File(modelsDir, "vosk-model.zip")
        if (!voskZipFile.exists()) {
            Log.d(TAG, "تحميل نموذج Vosk...")
            modelDownloader.downloadModel(VOSK_MODEL_URL, voskZipFile)
            // فك الضغط
            modelDownloader.extractZip(voskZipFile, File(modelsDir, "vosk"))
        }
    }
    
    private fun initializeTensorFlowModels() {
        try {
            val modelsDir = File(context.filesDir, "models")
            
            // تهيئة FaceNet
            val faceNetFile = File(modelsDir, "facenet.tflite")
            if (faceNetFile.exists()) {
                faceNetInterpreter = Interpreter(loadModelFile(faceNetFile))
                Log.d(TAG, "تم تهيئة نموذج FaceNet")
            }
            
            // تهيئة MobileNet
            val mobileNetFile = File(modelsDir, "mobilenet.tflite")
            if (mobileNetFile.exists()) {
                mobileNetInterpreter = Interpreter(loadModelFile(mobileNetFile))
                Log.d(TAG, "تم تهيئة نموذج MobileNet")
            }
            
            // تهيئة CamelBERT
            val camelBertFile = File(modelsDir, "camelbert.tflite")
            if (camelBertFile.exists()) {
                camelBertInterpreter = Interpreter(loadModelFile(camelBertFile))
                Log.d(TAG, "تم تهيئة نموذج CamelBERT")
            }
            
            // تهيئة BioBERT
            val bioBertFile = File(modelsDir, "bio_bert.tflite")
            if (bioBertFile.exists()) {
                bioBertInterpreter = Interpreter(loadModelFile(bioBertFile))
                Log.d(TAG, "تم تهيئة نموذج BioBERT")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تهيئة نماذج TensorFlow: ${e.message}", e)
        }
    }
    
    private fun initializeOnnxModels() {
        try {
            ortEnvironment = OrtEnvironment.getEnvironment()
            
            val modelsDir = File(context.filesDir, "models")
            val yamNetFile = File(modelsDir, "yamnet.onnx")
            
            if (yamNetFile.exists()) {
                yamNetSession = ortEnvironment?.createSession(yamNetFile.absolutePath)
                Log.d(TAG, "تم تهيئة نموذج YAMNet")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تهيئة نماذج ONNX: ${e.message}", e)
        }
    }
    
    private fun loadModelFile(file: File): ByteBuffer {
        val fileInputStream = FileInputStream(file)
        val fileChannel = fileInputStream.channel
        val startOffset = 0L
        val declaredLength = fileChannel.size()
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    // معالجة الوجوه باستخدام FaceNet
    fun processFace(faceData: FloatArray): FloatArray? {
        return try {
            if (faceNetInterpreter == null) {
                Log.w(TAG, "نموذج FaceNet غير مهيأ")
                return null
            }
            
            val inputShape = faceNetInterpreter!!.getInputTensor(0).shape()
            val outputShape = faceNetInterpreter!!.getOutputTensor(0).shape()
            
            val inputBuffer = Array(1) { Array(inputShape[1]) { Array(inputShape[2]) { FloatArray(inputShape[3]) } } }
            val outputBuffer = Array(1) { FloatArray(outputShape[1]) }
            
            // تحويل البيانات إلى الشكل المطلوب
            var index = 0
            for (i in 0 until inputShape[1]) {
                for (j in 0 until inputShape[2]) {
                    for (k in 0 until inputShape[3]) {
                        inputBuffer[0][i][j][k] = faceData[index++]
                    }
                }
            }
            
            faceNetInterpreter!!.run(inputBuffer, outputBuffer)
            outputBuffer[0]
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في معالجة الوجه: ${e.message}", e)
            null
        }
    }
    
    // معالجة الصور باستخدام MobileNet
    fun processImage(imageData: FloatArray): Array<FloatArray>? {
        return try {
            if (mobileNetInterpreter == null) {
                Log.w(TAG, "نموذج MobileNet غير مهيأ")
                return null
            }
            
            val inputShape = mobileNetInterpreter!!.getInputTensor(0).shape()
            val outputShape = mobileNetInterpreter!!.getOutputTensor(0).shape()
            
            val inputBuffer = Array(1) { Array(inputShape[1]) { Array(inputShape[2]) { FloatArray(inputShape[3]) } } }
            val outputBuffer = Array(1) { FloatArray(outputShape[1]) }
            
            // تحويل البيانات
            var index = 0
            for (i in 0 until inputShape[1]) {
                for (j in 0 until inputShape[2]) {
                    for (k in 0 until inputShape[3]) {
                        inputBuffer[0][i][j][k] = imageData[index++]
                    }
                }
            }
            
            mobileNetInterpreter!!.run(inputBuffer, outputBuffer)
            outputBuffer
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في معالجة الصورة: ${e.message}", e)
            null
        }
    }
    
    // معالجة النصوص باستخدام CamelBERT
    fun processText(text: String, isMedical: Boolean = false): FloatArray? {
        return try {
            val interpreter = if (isMedical) bioBertInterpreter else camelBertInterpreter
            
            if (interpreter == null) {
                Log.w(TAG, "نموذج BERT غير مهيأ")
                return null
            }
            
            // معالجة النص وتحويله إلى tokens
            val tokens = tokenizeText(text, isMedical)
            
            val inputShape = interpreter.getInputTensor(0).shape()
            val outputShape = interpreter.getOutputTensor(0).shape()
            
            val inputBuffer = Array(1) { IntArray(inputShape[1]) }
            val outputBuffer = Array(1) { FloatArray(outputShape[1]) }
            
            // تحضير البيانات
            for (i in tokens.indices.take(inputShape[1])) {
                inputBuffer[0][i] = tokens[i]
            }
            
            interpreter.run(inputBuffer, outputBuffer)
            outputBuffer[0]
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في معالجة النص: ${e.message}", e)
            null
        }
    }
    
    // معالجة الصوت باستخدام YAMNet
    fun processAudio(audioData: FloatArray): FloatArray? {
        return try {
            if (yamNetSession == null) {
                Log.w(TAG, "نموذج YAMNet غير مهيأ")
                return null
            }
            
            val inputName = yamNetSession!!.inputNames.iterator().next()
            val inputTensor = OnnxTensor.createTensor(ortEnvironment, audioData)
            
            val results = yamNetSession!!.run(mapOf(inputName to inputTensor))
            val outputTensor = results[0].value as Array<FloatArray>
            
            inputTensor.close()
            results.close()
            
            outputTensor[0]
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في معالجة الصوت: ${e.message}", e)
            null
        }
    }
    
    private fun tokenizeText(text: String, isMedical: Boolean): IntArray {
        // تطبيق tokenization للنص العربي
        // هذا تطبيق مبسط - في التطبيق الحقيقي نحتاج لقراءة ملف vocab
        val words = text.split(" ")
        return words.map { it.hashCode() and 0x7FFFFFFF % 30000 }.toIntArray()
    }
    
    fun areModelsReady(): Boolean = modelsInitialized
    
    fun cleanup() {
        faceNetInterpreter?.close()
        mobileNetInterpreter?.close()
        camelBertInterpreter?.close()
        bioBertInterpreter?.close()
        yamNetSession?.close()
        ortEnvironment?.close()
    }
}