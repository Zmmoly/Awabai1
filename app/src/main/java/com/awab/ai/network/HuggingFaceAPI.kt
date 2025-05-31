package com.awab.ai.network

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HuggingFaceAPI {
    
    companion object {
        private const val TAG = "HuggingFaceAPI"
        private const val BASE_URL = "https://api-inference.huggingface.co/models/"
        private const val FALCON_40B_MODEL = "tiiuae/falcon-40b"
        private const val TIMEOUT_SECONDS = 30L
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    private val mediaType = "application/json".toMediaType()
    
    suspend fun queryFalcon40B(prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            val apiKey = System.getenv("HUGGING_FACE_API_KEY")
            
            if (apiKey.isNullOrEmpty()) {
                Log.e(TAG, "مفتاح API غير موجود")
                return@withContext null
            }
            
            val requestBody = JSONObject().apply {
                put("inputs", prompt)
                put("parameters", JSONObject().apply {
                    put("max_new_tokens", 500)
                    put("temperature", 0.7)
                    put("top_p", 0.9)
                    put("do_sample", true)
                    put("return_full_text", false)
                })
            }
            
            val request = Request.Builder()
                .url("$BASE_URL$FALCON_40B_MODEL")
                .post(requestBody.toString().toRequestBody(mediaType))
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                response.body?.string()?.let { responseBody ->
                    parseResponse(responseBody)
                }
            } else {
                Log.e(TAG, "فشل في الاستعلام: ${response.code} - ${response.message}")
                null
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "خطأ في الشبكة: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "خطأ عام: ${e.message}", e)
            null
        }
    }
    
    private fun parseResponse(responseBody: String): String? {
        return try {
            val jsonArray = org.json.JSONArray(responseBody)
            if (jsonArray.length() > 0) {
                val firstResult = jsonArray.getJSONObject(0)
                val generatedText = firstResult.getString("generated_text")
                cleanGeneratedText(generatedText)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تحليل الاستجابة: ${e.message}", e)
            null
        }
    }
    
    private fun cleanGeneratedText(text: String): String {
        return text
            .trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("^\"|\"$"), "")
    }
}