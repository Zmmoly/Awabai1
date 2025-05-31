package com.awab.ai.ml.processors

import android.content.Context
import android.graphics.*
import android.util.Log
import com.awab.ai.AwabAIApplication
import com.awab.ai.data.database.FaceEntity
import kotlinx.coroutines.*

class FaceProcessor(private val context: Context) {
    
    companion object {
        private const val TAG = "FaceProcessor"
        private const val FACE_SIZE = 160
        private const val SIMILARITY_THRESHOLD = 0.8f
    }
    
    private val modelManager = AwabAIApplication.instance.modelManager
    private val database = AwabAIApplication.instance.database
    
    suspend fun processAndIdentifyFace(bitmap: Bitmap, faceRect: RectF): String = withContext(Dispatchers.IO) {
        try {
            // استخراج منطقة الوجه
            val faceBitmap = extractFaceFromBitmap(bitmap, faceRect)
            
            if (faceBitmap != null) {
                // الحصول على embeddings الوجه
                val faceEmbeddings = extractFaceEmbeddings(faceBitmap)
                
                if (faceEmbeddings != null) {
                    // البحث عن وجه مطابق في قاعدة البيانات
                    val knownFace = findMatchingFace(faceEmbeddings)
                    
                    if (knownFace != null) {
                        "تم التعرف على: ${knownFace.name}"
                    } else {
                        "وجه غير معروف. هل تريد تسجيله؟"
                    }
                } else {
                    "لم أتمكن من معالجة الوجه"
                }
            } else {
                "لم أتمكن من استخراج الوجه من الصورة"
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في معالجة الوجه: ${e.message}", e)
            "حدث خطأ في تحليل الوجه"
        }
    }
    
    private fun extractFaceFromBitmap(bitmap: Bitmap, faceRect: RectF): Bitmap? {
        return try {
            val left = (faceRect.left.coerceAtLeast(0f)).toInt()
            val top = (faceRect.top.coerceAtLeast(0f)).toInt()
            val width = (faceRect.width().coerceAtMost(bitmap.width - left.toFloat())).toInt()
            val height = (faceRect.height().coerceAtMost(bitmap.height - top.toFloat())).toInt()
            
            if (width > 0 && height > 0) {
                val faceBitmap = Bitmap.createBitmap(bitmap, left, top, width, height)
                Bitmap.createScaledBitmap(faceBitmap, FACE_SIZE, FACE_SIZE, true)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في استخراج الوجه: ${e.message}", e)
            null
        }
    }
    
    private fun extractFaceEmbeddings(faceBitmap: Bitmap): FloatArray? {
        return try {
            // تحضير الصورة للنموذج
            val inputData = preprocessFaceImage(faceBitmap)
            
            // استخدام نموذج FaceNet للحصول على embeddings
            modelManager.processFace(inputData)
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في استخراج embeddings: ${e.message}", e)
            null
        }
    }
    
    private fun preprocessFaceImage(bitmap: Bitmap): FloatArray {
        val inputArray = FloatArray(FACE_SIZE * FACE_SIZE * 3)
        var pixelIndex = 0
        
        for (y in 0 until FACE_SIZE) {
            for (x in 0 until FACE_SIZE) {
                val pixel = bitmap.getPixel(x, y)
                
                // تطبيع القيم وفقاً لمتطلبات FaceNet
                inputArray[pixelIndex++] = (Color.red(pixel) - 127.5f) / 128f
                inputArray[pixelIndex++] = (Color.green(pixel) - 127.5f) / 128f
                inputArray[pixelIndex++] = (Color.blue(pixel) - 127.5f) / 128f
            }
        }
        
        return inputArray
    }
    
    private suspend fun findMatchingFace(faceEmbeddings: FloatArray): FaceEntity? {
        return try {
            val allFaces = database.faceDao().getAllFaces()
            
            for (face in allFaces) {
                val similarity = calculateCosineSimilarity(faceEmbeddings, face.embeddings)
                if (similarity > SIMILARITY_THRESHOLD) {
                    return face
                }
            }
            
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في البحث عن الوجه: ${e.message}", e)
            null
        }
    }
    
    private fun calculateCosineSimilarity(embeddings1: FloatArray, embeddings2: FloatArray): Float {
        if (embeddings1.size != embeddings2.size) return 0f
        
        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f
        
        for (i in embeddings1.indices) {
            dotProduct += embeddings1[i] * embeddings2[i]
            norm1 += embeddings1[i] * embeddings1[i]
            norm2 += embeddings2[i] * embeddings2[i]
        }
        
        val denominator = kotlin.math.sqrt(norm1) * kotlin.math.sqrt(norm2)
        return if (denominator > 0) dotProduct / denominator else 0f
    }
    
    suspend fun registerNewFace(bitmap: Bitmap, faceRect: RectF, name: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val faceBitmap = extractFaceFromBitmap(bitmap, faceRect)
            
            if (faceBitmap != null) {
                val faceEmbeddings = extractFaceEmbeddings(faceBitmap)
                
                if (faceEmbeddings != null) {
                    // حفظ الوجه في قاعدة البيانات
                    val faceEntity = FaceEntity(
                        name = name,
                        embeddings = faceEmbeddings,
                        registrationDate = System.currentTimeMillis()
                    )
                    
                    database.faceDao().insertFace(faceEntity)
                    Log.d(TAG, "تم تسجيل وجه جديد: $name")
                    true
                } else {
                    false
                }
            } else {
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تسجيل الوجه: ${e.message}", e)
            false
        }
    }
    
    suspend fun updateFaceEmbeddings(faceId: Long, newEmbeddings: FloatArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val existingFace = database.faceDao().getFaceById(faceId)
            
            if (existingFace != null) {
                // دمج embeddings جديدة مع القديمة لتحسين الدقة
                val improvedEmbeddings = mergeEmbeddings(existingFace.embeddings, newEmbeddings)
                
                val updatedFace = existingFace.copy(
                    embeddings = improvedEmbeddings,
                    lastSeenDate = System.currentTimeMillis()
                )
                
                database.faceDao().updateFace(updatedFace)
                Log.d(TAG, "تم تحديث embeddings للوجه: ${existingFace.name}")
                true
            } else {
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تحديث الوجه: ${e.message}", e)
            false
        }
    }
    
    private fun mergeEmbeddings(oldEmbeddings: FloatArray, newEmbeddings: FloatArray): FloatArray {
        if (oldEmbeddings.size != newEmbeddings.size) return newEmbeddings
        
        // متوسط مرجح للـ embeddings القديمة والجديدة
        val weight = 0.7f // وزن أكبر للـ embeddings القديمة
        return FloatArray(oldEmbeddings.size) { i ->
            oldEmbeddings[i] * weight + newEmbeddings[i] * (1 - weight)
        }
    }
    
    suspend fun getAllRegisteredFaces(): List<FaceEntity> = withContext(Dispatchers.IO) {
        try {
            database.faceDao().getAllFaces()
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في جلب الوجوه: ${e.message}", e)
            emptyList()
        }
    }
    
    suspend fun deleteFace(faceId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            database.faceDao().deleteFaceById(faceId)
            Log.d(TAG, "تم حذف الوجه: $faceId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في حذف الوجه: ${e.message}", e)
            false
        }
    }
}