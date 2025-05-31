package com.awab.ai.ml.processors

import android.content.Context
import android.util.Log
import com.awab.ai.data.models.ConversationContext
import kotlinx.coroutines.*

class TextProcessor(private val context: Context) {
    
    companion object {
        private const val TAG = "TextProcessor"
    }
    
    fun processMedicalQuery(query: String, embeddings: FloatArray): String {
        return try {
            val medicalResponse = analyzeMedicalQuery(query, embeddings)
            buildMedicalResponse(query, medicalResponse)
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في معالجة الاستفسار الطبي: ${e.message}", e)
            "عذراً، لا أستطيع معالجة استفسارك الطبي حالياً."
        }
    }
    
    private fun analyzeMedicalQuery(query: String, embeddings: FloatArray): MedicalAnalysis {
        val lowerQuery = query.lowercase()
        
        return when {
            containsSymptoms(lowerQuery) -> analyzeSymptomsQuery(query, embeddings)
            containsMedications(lowerQuery) -> analyzeMedicationQuery(query, embeddings)
            containsConditions(lowerQuery) -> analyzeConditionQuery(query, embeddings)
            else -> analyzeGeneralMedicalQuery(query, embeddings)
        }
    }
    
    private fun containsSymptoms(query: String): Boolean {
        val symptomKeywords = listOf(
            "أعراض", "ألم", "صداع", "حمى", "دوار", "غثيان", "تعب", "إرهاق",
            "سعال", "زكام", "التهاب", "طفح", "حكة", "ضيق تنفس"
        )
        return symptomKeywords.any { query.contains(it) }
    }
    
    private fun containsMedications(query: String): Boolean {
        val medicationKeywords = listOf(
            "دواء", "حبوب", "علاج", "مضاد حيوي", "مسكن", "فيتامين", "جرعة"
        )
        return medicationKeywords.any { query.contains(it) }
    }
    
    private fun containsConditions(query: String): Boolean {
        val conditionKeywords = listOf(
            "مرض", "داء", "متلازمة", "اضطراب", "التهاب", "عدوى", "إصابة"
        )
        return conditionKeywords.any { query.contains(it) }
    }
    
    private fun analyzeSymptomsQuery(query: String, embeddings: FloatArray): MedicalAnalysis {
        return MedicalAnalysis(
            category = "أعراض",
            confidence = calculateConfidence(embeddings),
            response = "بناءً على الأعراض المذكورة، قد تكون هناك عدة احتمالات. من المهم استشارة طبيب للتشخيص الدقيق.",
            recommendations = listOf(
                "راجع طبيباً مختصاً",
                "اشرب الكثير من الماء",
                "احصل على راحة كافية",
                "راقب تطور الأعراض"
            )
        )
    }
    
    private fun analyzeMedicationQuery(query: String, embeddings: FloatArray): MedicalAnalysis {
        return MedicalAnalysis(
            category = "أدوية",
            confidence = calculateConfidence(embeddings),
            response = "المعلومات حول الأدوية مهمة جداً. يجب دائماً استشارة طبيب أو صيدلي قبل تناول أي دواء.",
            recommendations = listOf(
                "استشر طبيباً أو صيدلياً",
                "اقرأ النشرة الداخلية للدواء",
                "لا تتجاوز الجرعة المحددة",
                "أبلغ عن أي آثار جانبية"
            )
        )
    }
    
    private fun analyzeConditionQuery(query: String, embeddings: FloatArray): MedicalAnalysis {
        return MedicalAnalysis(
            category = "حالات طبية",
            confidence = calculateConfidence(embeddings),
            response = "الحالات الطبية تتطلب تقييماً طبياً دقيقاً. المعلومات العامة مفيدة لكنها لا تغني عن الاستشارة الطبية.",
            recommendations = listOf(
                "احجز موعداً مع طبيب مختص",
                "اجمع معلومات حول التاريخ المرضي",
                "اتبع نمط حياة صحي",
                "تابع التطورات مع الطبيب"
            )
        )
    }
    
    private fun analyzeGeneralMedicalQuery(query: String, embeddings: FloatArray): MedicalAnalysis {
        return MedicalAnalysis(
            category = "استفسار عام",
            confidence = calculateConfidence(embeddings),
            response = "يمكنني تقديم معلومات طبية عامة، لكن يجب دائماً الرجوع للمختصين للحصول على مشورة طبية دقيقة.",
            recommendations = listOf(
                "استشر طبيباً للحصول على تشخيص دقيق",
                "اطلع على مصادر طبية موثوقة",
                "حافظ على نمط حياة صحي"
            )
        )
    }
    
    private fun calculateConfidence(embeddings: FloatArray): Float {
        // حساب درجة الثقة بناءً على embeddings
        val magnitude = kotlin.math.sqrt(embeddings.map { it * it }.sum())
        return (magnitude / embeddings.size).coerceIn(0f, 1f)
    }
    
    private fun buildMedicalResponse(query: String, analysis: MedicalAnalysis): String {
        return buildString {
            append("🏥 استفسار طبي - ${analysis.category}\n\n")
            append("${analysis.response}\n\n")
            append("💡 توصيات:\n")
            analysis.recommendations.forEach { recommendation ->
                append("• $recommendation\n")
            }
        }
    }
    
    fun processGeneralQuery(query: String, embeddings: FloatArray, context: ConversationContext): String {
        return try {
            val response = analyzeGeneralQuery(query, embeddings, context)
            buildContextualResponse(query, response, context)
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في معالجة الاستفسار العام: ${e.message}", e)
            generateFallbackResponse(query)
        }
    }
    
    private fun analyzeGeneralQuery(query: String, embeddings: FloatArray, context: ConversationContext): GeneralAnalysis {
        val queryType = determineQueryType(query)
        val sentiment = analyzeSentiment(embeddings)
        val intent = extractIntent(query, context)
        
        return GeneralAnalysis(
            type = queryType,
            sentiment = sentiment,
            intent = intent,
            confidence = calculateConfidence(embeddings),
            contextRelevance = calculateContextRelevance(query, context)
        )
    }
    
    private fun determineQueryType(query: String): QueryType {
        return when {
            query.contains("كيف") || query.contains("طريقة") -> QueryType.HOW_TO
            query.contains("ما هو") || query.contains("تعريف") -> QueryType.DEFINITION
            query.contains("لماذا") || query.contains("سبب") -> QueryType.EXPLANATION
            query.contains("أين") || query.contains("موقع") -> QueryType.LOCATION
            query.contains("متى") || query.contains("وقت") -> QueryType.TIME
            query.contains("من") || query.contains("شخص") -> QueryType.PERSON
            else -> QueryType.GENERAL
        }
    }
    
    private fun analyzeSentiment(embeddings: FloatArray): Sentiment {
        // تحليل مبسط للمشاعر بناءً على embeddings
        val positiveScore = embeddings.take(embeddings.size / 3).sum()
        val negativeScore = embeddings.takeLast(embeddings.size / 3).sum()
        
        return when {
            positiveScore > negativeScore + 0.1 -> Sentiment.POSITIVE
            negativeScore > positiveScore + 0.1 -> Sentiment.NEGATIVE
            else -> Sentiment.NEUTRAL
        }
    }
    
    private fun extractIntent(query: String, context: ConversationContext): Intent {
        val lowerQuery = query.lowercase()
        
        return when {
            lowerQuery.contains("ساعد") || lowerQuery.contains("مساعدة") -> Intent.HELP_REQUEST
            lowerQuery.contains("شكرا") || lowerQuery.contains("شكراً") -> Intent.GRATITUDE
            lowerQuery.contains("معلومات") || lowerQuery.contains("تفاصيل") -> Intent.INFORMATION_SEEKING
            lowerQuery.contains("رأي") || lowerQuery.contains("اقتراح") -> Intent.OPINION_REQUEST
            context.discussedTopics.any { lowerQuery.contains(it.lowercase()) } -> Intent.CONTINUATION
            else -> Intent.GENERAL_INQUIRY
        }
    }
    
    private fun calculateContextRelevance(query: String, context: ConversationContext): Float {
        if (context.lastUserMessage.isEmpty()) return 0f
        
        val queryWords = query.split(" ").map { it.lowercase() }
        val contextWords = context.lastUserMessage.split(" ").map { it.lowercase() }
        val commonWords = queryWords.intersect(contextWords.toSet())
        
        return if (queryWords.isNotEmpty()) {
            commonWords.size.toFloat() / queryWords.size
        } else 0f
    }
    
    private fun buildContextualResponse(query: String, analysis: GeneralAnalysis, context: ConversationContext): String {
        return when (analysis.type) {
            QueryType.HOW_TO -> buildHowToResponse(query, analysis)
            QueryType.DEFINITION -> buildDefinitionResponse(query, analysis)
            QueryType.EXPLANATION -> buildExplanationResponse(query, analysis)
            QueryType.LOCATION -> buildLocationResponse(query, analysis)
            QueryType.TIME -> buildTimeResponse(query, analysis)
            QueryType.PERSON -> buildPersonResponse(query, analysis)
            QueryType.GENERAL -> buildGeneralResponse(query, analysis, context)
        }
    }
    
    private fun buildHowToResponse(query: String, analysis: GeneralAnalysis): String {
        return "إليك الطريقة للقيام بذلك:\n\n" +
                "1. حدد هدفك بوضوح\n" +
                "2. اجمع المعلومات اللازمة\n" +
                "3. ضع خطة عمل\n" +
                "4. ابدأ بالتنفيذ خطوة بخطوة\n" +
                "5. راقب التقدم وعدّل حسب الحاجة\n\n" +
                "هل تريد المزيد من التفاصيل حول نقطة معينة؟"
    }
    
    private fun buildDefinitionResponse(query: String, analysis: GeneralAnalysis): String {
        return "بناءً على استفسارك، يمكنني تقديم تعريف عام:\n\n" +
                "هذا موضوع واسع يتطلب شرحاً مفصلاً. " +
                "يمكنني تقديم معلومات أكثر تحديداً إذا أوضحت الجانب الذي يهمك أكثر.\n\n" +
                "هل تريد معرفة جانب معين من هذا الموضوع؟"
    }
    
    private fun buildExplanationResponse(query: String, analysis: GeneralAnalysis): String {
        return "إليك التفسير:\n\n" +
                "الأسباب عادة ما تكون متعددة ومترابطة. " +
                "من المهم النظر إلى السياق الكامل لفهم الموضوع بشكل أفضل.\n\n" +
                "هل تريد المزيد من التفاصيل حول سبب معين؟"
    }
    
    private fun buildLocationResponse(query: String, analysis: GeneralAnalysis): String {
        return "بخصوص الموقع الذي تسأل عنه:\n\n" +
                "يمكنني مساعدتك في العثور على معلومات عن المواقع، " +
                "لكنني أحتاج لتفاصيل أكثر دقة لتقديم إجابة مفيدة.\n\n" +
                "يمكنك تحديد المنطقة أو البلد الذي تبحث عنه؟"
    }
    
    private fun buildTimeResponse(query: String, analysis: GeneralAnalysis): String {
        return "بخصوص التوقيت:\n\n" +
                "الوقت الحالي هو ${java.text.SimpleDateFormat("HH:mm", java.util.Locale("ar")).format(java.util.Date())}\n" +
                "التاريخ: ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("ar")).format(java.util.Date())}\n\n" +
                "هل تحتاج معلومات عن وقت محدد أو حدث معين؟"
    }
    
    private fun buildPersonResponse(query: String, analysis: GeneralAnalysis): String {
        return "بخصوص الشخص المذكور:\n\n" +
                "يمكنني مساعدتك في الحصول على معلومات عامة، " +
                "لكنني أحتاج لتوضيح أكثر حول من تقصد تحديداً.\n\n" +
                "هل يمكنك تقديم المزيد من التفاصيل؟"
    }
    
    private fun buildGeneralResponse(query: String, analysis: GeneralAnalysis, context: ConversationContext): String {
        val sentiment = when (analysis.sentiment) {
            Sentiment.POSITIVE -> "أرى أنك مهتم بهذا الموضوع! "
            Sentiment.NEGATIVE -> "أفهم قلقك حول هذا الأمر. "
            Sentiment.NEUTRAL -> ""
        }
        
        val contextual = if (analysis.contextRelevance > 0.3) {
            "بناءً على محادثتنا السابقة، "
        } else ""
        
        return "${sentiment}${contextual}يمكنني مساعدتك في هذا الموضوع.\n\n" +
                "لتقديم إجابة أكثر دقة، يمكنك توضيح:\n" +
                "• الجانب المحدد الذي يهمك\n" +
                "• السياق أو الغرض من السؤال\n" +
                "• أي تفاصيل إضافية مفيدة\n\n" +
                "كيف يمكنني مساعدتك أكثر؟"
    }
    
    private fun generateFallbackResponse(query: String): String {
        return "أعتذر، واجهت صعوبة في فهم استفسارك بشكل كامل.\n\n" +
                "يمكنك إعادة صياغة السؤال أو تقديم المزيد من التفاصيل؟ " +
                "أنا هنا لمساعدتك في أي موضوع تريد مناقشته."
    }
    
    // Data classes
    data class MedicalAnalysis(
        val category: String,
        val confidence: Float,
        val response: String,
        val recommendations: List<String>
    )
    
    data class GeneralAnalysis(
        val type: QueryType,
        val sentiment: Sentiment,
        val intent: Intent,
        val confidence: Float,
        val contextRelevance: Float
    )
    
    enum class QueryType {
        HOW_TO, DEFINITION, EXPLANATION, LOCATION, TIME, PERSON, GENERAL
    }
    
    enum class Sentiment {
        POSITIVE, NEGATIVE, NEUTRAL
    }
    
    enum class Intent {
        HELP_REQUEST, GRATITUDE, INFORMATION_SEEKING, OPINION_REQUEST, CONTINUATION, GENERAL_INQUIRY
    }
}