package com.example

import com.example.engine.NewsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsEngineTest {

    @Test
    fun testClusteringSimilarityAndOverlap() {
        val text1 = "رونمایی از نسل جدید موتورهای هوش مصنوعی توسط سام آلتمن در شرکت OpenAI"
        val text2 = "هوش مصنوعی جدید سام آلتمن و مدل‌های چندرسانه‌ای پیشرفته در OpenAI معرفی شد"

        val similarity = NewsEngine.computeSimilarity(text1, text2)
        assertTrue("Similarity should be high for shared entities and keywords", similarity > 0.35)
    }

    @Test
    fun testEntityExtraction() {
        val content = "وزیر خارجه در تهران با دبیرکل سازمان ملل دیدار و گفتگو کرد."
        val entities = NewsEngine.extractEntities(content)

        val entityNames = entities.map { it.entityOrClaim }
        assertTrue(entityNames.contains("وزیر خارجه"))
        assertTrue(entityNames.contains("تهران"))
        assertTrue(entityNames.contains("سازمان ملل"))
    }

    @Test
    fun testImportanceAndConfidenceScoring() {
        val importance = NewsEngine.calculateImportance("خبر فوری بحران اقتصادی", "تصویب طرح جدید در بانک مرکزی با ارقام نجومی")
        val confidence = NewsEngine.calculateConfidence(mentionCount = 3, sourceTrust = 0.9)
        val priority = NewsEngine.calculatePriority(importance, confidence)

        assertTrue(importance in 0.0..1.0)
        assertTrue(confidence in 0.0..1.0)
        assertTrue(priority in 0.0..1.0)
        assertTrue(priority >= NewsEngine.THRESHOLD_MEDIUM)
    }

    @Test
    fun testFivePartAnalysisGeneration() {
        val analysis = NewsEngine.generateFivePartAnalysis(
            title = "قرارداد کلان ترانزیت ریلی میان کشورهای منطقه به امضا رسید",
            content = "این قرارداد گامی بنیادین در جهت یکپارچه‌سازی سامانه‌های تبادل کالا و ترانزیت کالا است.",
            category = "اقتصاد و بازرگانی"
        )

        assertNotNull(analysis.summary)
        assertTrue(analysis.summary.isNotBlank())
        assertTrue(analysis.impact.isNotBlank())
        assertEquals(3, analysis.images.size)
        assertTrue(analysis.publicOpinions.isNotEmpty())
        assertTrue(analysis.relatedNews.isNotEmpty())
    }
}
