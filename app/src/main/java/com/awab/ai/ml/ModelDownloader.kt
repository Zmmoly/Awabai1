package com.awab.ai.ml

import android.content.Context
import android.util.Log
import okhttp3.*
import java.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile

class ModelDownloader(private val context: Context) {
    
    companion object {
        private const val TAG = "ModelDownloader"
    }
    
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    
    suspend fun downloadModel(url: String, destinationFile: File) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "بدء تحميل النموذج من: $url")
            
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                response.body?.let { body ->
                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(destinationFile)
                    
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    val contentLength = body.contentLength()
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        
                        if (contentLength > 0) {
                            val progress = (totalBytesRead * 100 / contentLength).toInt()
                            Log.d(TAG, "تقدم التحميل: $progress%")
                        }
                    }
                    
                    outputStream.close()
                    inputStream.close()
                    
                    Log.d(TAG, "تم تحميل النموذج بنجاح: ${destinationFile.name}")
                }
            } else {
                throw IOException("فشل في تحميل النموذج: ${response.code}")
            }
            
            response.close()
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تحميل النموذج: ${e.message}", e)
            throw e
        }
    }
    
    suspend fun extractZip(zipFile: File, destinationDir: File) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "بدء فك ضغط الملف: ${zipFile.name}")
            
            if (!destinationDir.exists()) {
                destinationDir.mkdirs()
            }
            
            val zip = ZipFile(zipFile)
            zip.extractAll(destinationDir.absolutePath)
            
            Log.d(TAG, "تم فك الضغط بنجاح باستخدام Zip4j")
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في فك الضغط: ${e.message}", e)
            throw e
        }
    }
}