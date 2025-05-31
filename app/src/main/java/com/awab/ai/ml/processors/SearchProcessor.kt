package com.awab.ai.ml.processors

import android.content.Context
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class SearchProcessor(private val context: Context) {
    
    companion object {
        private const val TAG = "SearchProcessor"
    }
    
    private val client = OkHttpClient()
    
    suspend fun performSearch(query: String): String = withContext(Dispatchers.IO) {
        try {
            val searchResults = searchInternet(query)
            formatSearchResults(searchResults)
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في البحث: ${e.message}", e)
            "عذراً، لا أستطيع البحث حالياً. تأكد من الاتصال بالإنترنت."
        }
    }
    
    private suspend fun searchInternet(query: String): List<SearchResult> {
        // استخدام محرك بحث بسيط
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val searchUrl = "https://www.google.com/search?q=$encodedQuery"
        
        val request = Request.Builder()
            .url(searchUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Android 12; Mobile; rv:68.0) Gecko/68.0 Firefox/88.0")
            .build()
        
        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                parseSearchResults(response.body?.string() ?: "")
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في طلب البحث: ${e.message}", e)
            emptyList()
        }
    }
    
    private fun parseSearchResults(html: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        
        return try {
            val document: Document = Jsoup.parse(html)
            
            // تحليل نتائج Google
            val searchResults = document.select("div.g")
            
            for (element in searchResults.take(5)) {
                val titleElement = element.selectFirst("h3")
                val linkElement = element.selectFirst("a")
                val snippetElement = element.selectFirst("span[data-ved]") 
                    ?: element.selectFirst(".VwiC3b")
                
                val title = titleElement?.text() ?: ""
                val url = linkElement?.attr("href") ?: ""
                val snippet = snippetElement?.text() ?: ""
                
                if (title.isNotEmpty() && url.isNotEmpty()) {
                    results.add(
                        SearchResult(
                            title = title,
                            snippet = snippet.take(200),
                            url = if (url.startsWith("/url?q=")) {
                                url.substringAfter("/url?q=").substringBefore("&")
                            } else url
                        )
                    )
                }
            }
            
            results
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في تحليل نتائج البحث: ${e.message}", e)
            emptyList()
        }
    }
    
    private fun formatSearchResults(results: List<SearchResult>): String {
        return if (results.isNotEmpty()) {
            buildString {
                append("🔍 نتائج البحث:\n\n")
                results.take(3).forEachIndexed { index, result ->
                    append("${index + 1}. ${result.title}\n")
                    append("${result.snippet}\n\n")
                }
                append("ℹ️ المعلومات من الإنترنت - يرجى التحقق من المصادر")
            }
        } else {
            "لم أجد نتائج مناسبة لاستفسارك. يمكنك إعادة صياغة السؤال؟"
        }
    }
    
    data class SearchResult(
        val title: String,
        val snippet: String,
        val url: String
    )
}