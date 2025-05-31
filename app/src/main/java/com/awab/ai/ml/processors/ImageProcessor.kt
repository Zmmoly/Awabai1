package com.awab.ai.ml.processors

import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.awab.ai.AwabAIApplication
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlinx.coroutines.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.arabic.ArabicTextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ImageProcessor(private val context: Context) {
    
    companion object {
        private const val TAG = "ImageProcessor"
        private const val INPUT_SIZE = 224
    }
    
    private val modelManager = AwabAIApplication.instance.modelManager
    
    suspend fun captureAndAnalyze(): String = withContext(Dispatchers.Main) {
        try {
            Log.d(TAG, "بدء التقاط وتحليل الصورة...")
            
            val imageCapture = setupCamera()
            val bitmap = captureImage(imageCapture)
            
            if (bitmap != null) {
                return@withContext analyzeImage(bitmap)
            }
            
            "لم أتمكن من التقاط الصورة"
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في معالجة الصورة: ${e.message}", e)
            "حدث خطأ أثناء تحليل الصورة"
        }
    }
    
    private suspend fun setupCamera(): ImageCapture = withContext(Dispatchers.Main) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get()
        
        val imageCapture = ImageCapture.Builder()
            .setTargetRotation(0)
            .build()
        
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                context as androidx.lifecycle.LifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في ربط الكاميرا: ${e.message}", e)
        }
        
        imageCapture
    }
    
    private suspend fun captureImage(imageCapture: ImageCapture): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
                java.io.File(context.cacheDir, "temp_image.jpg")
            ).build()
            
            val result = CompletableDeferred<Bitmap?>()
            
            imageCapture.takePicture(
                outputFileOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val bitmap = BitmapFactory.decodeFile(output.savedUri?.path)
                        result.complete(bitmap)
                    }
                    
                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "خطأ في التقاط الصورة: ${exception.message}", exception)
                        result.complete(null)
                    }
                }
            )
            
            result.await()
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في التقاط الصورة: ${e.message}", e)
            null
        }
    }
    
    suspend fun analyzeImage(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        try {
            // تحضير الصورة للنموذج
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            val imageData = preprocessImage(resizedBitmap)
            
            // تحليل الصورة بـ MobileNet
            val classifications = modelManager.processImage(imageData)
            
            if (classifications != null) {
                interpretClassifications(classifications[0])
            } else {
                "لم أتمكن من تحليل الصورة بواسطة النموذج"
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تحليل الصورة: ${e.message}", e)
            "حدث خطأ أثناء تحليل الصورة"
        }
    }
    
    private fun preprocessImage(bitmap: Bitmap): FloatArray {
        val inputArray = FloatArray(INPUT_SIZE * INPUT_SIZE * 3)
        var pixelIndex = 0
        
        for (y in 0 until INPUT_SIZE) {
            for (x in 0 until INPUT_SIZE) {
                val pixel = bitmap.getPixel(x, y)
                
                // تطبيع القيم إلى [0, 1]
                inputArray[pixelIndex++] = Color.red(pixel) / 255.0f
                inputArray[pixelIndex++] = Color.green(pixel) / 255.0f
                inputArray[pixelIndex++] = Color.blue(pixel) / 255.0f
            }
        }
        
        return inputArray
    }
    
    private fun interpretClassifications(classifications: FloatArray): String {
        // قائمة الفئات الأكثر شيوعاً في MobileNet (مبسطة)
        val categories = listOf(
            "شخص", "سيارة", "طائر", "قطة", "كلب", "زهرة", "شجرة", "منزل",
            "كتاب", "هاتف", "كمبيوتر", "طعام", "كوب", "كرسي", "طاولة"
        )
        
        // العثور على أعلى نتيجة
        val maxIndex = classifications.indices.maxByOrNull { classifications[it] } ?: 0
        val confidence = classifications[maxIndex]
        
        return if (confidence > 0.5f && maxIndex < categories.size) {
            val category = categories[maxIndex]
            val percentage = (confidence * 100).toInt()
            "أرى ${category} بدرجة ثقة ${percentage}%"
        } else {
            analyzeImageContent(classifications)
        }
    }
    
    private fun analyzeImageContent(classifications: FloatArray): String {
        // تحليل أكثر تفصيلاً للصورة
        val topIndices = classifications.indices
            .sortedByDescending { classifications[it] }
            .take(3)
        
        val descriptions = mutableListOf<String>()
        
        for (index in topIndices) {
            val confidence = classifications[index]
            if (confidence > 0.3f) {
                descriptions.add(getDetailedDescription(index, confidence))
            }
        }
        
        return if (descriptions.isNotEmpty()) {
            "في هذه الصورة، يمكنني رؤية: ${descriptions.joinToString("، ")}"
        } else {
            "أرى صورة لكنني لست متأكداً من محتواها. يمكنك وصفها لي؟"
        }
    }
    
    private fun getDetailedDescription(index: Int, confidence: Float): String {
        // وصف مفصل حسب الفئة
        val percentage = (confidence * 100).toInt()
        return when {
            index < 100 -> "كائن حي ($percentage%)"
            index < 200 -> "جماد ($percentage%)"
            index < 300 -> "مبنى أو هيكل ($percentage%)"
            index < 400 -> "نبات أو طبيعة ($percentage%)"
            else -> "شيء مثير للاهتمام ($percentage%)"
        }
    }
    
    fun analyzeImageForFaces(bitmap: Bitmap): List<RectF> {
        return try {
            // استخدام خوارزمية بسيطة للكشف عن الوجوه
            val faceDetector = FaceDetector(bitmap.width, bitmap.height, 10)
            val faces = Array(10) { FaceDetector.Face() }
            val faceCount = faceDetector.findFaces(bitmap, faces)
            
            val faceRects = mutableListOf<RectF>()
            
            for (i in 0 until faceCount) {
                val face = faces[i]
                val midPoint = PointF()
                face.getMidPoint(midPoint)
                val eyeDistance = face.eyesDistance()
                
                val rect = RectF(
                    midPoint.x - eyeDistance,
                    midPoint.y - eyeDistance,
                    midPoint.x + eyeDistance,
                    midPoint.y + eyeDistance
                )
                
                faceRects.add(rect)
            }
            
            faceRects
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في كشف الوجوه: ${e.message}", e)
            emptyList()
        }
    }
    
    suspend fun extractImageText(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val recognizer = TextRecognition.getClient(ArabicTextRecognizerOptions.Builder().build())
            val image = InputImage.fromBitmap(bitmap, 0)
            
            suspendCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val extractedText = visionText.text
                        continuation.resume(
                            if (extractedText.isNotEmpty()) {
                                "النص المستخرج من الصورة:\n$extractedText"
                            } else {
                                "لا يوجد نص واضح في هذه الصورة"
                            }
                        )
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "خطأ في استخراج النص: ${e.message}", e)
                        continuation.resume("فشل في استخراج النص من الصورة")
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في معالجة OCR: ${e.message}", e)
            "حدث خطأ أثناء استخراج النص"
        }
    }
}