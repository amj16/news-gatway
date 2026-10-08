package com.example

import com.example.ai.NewsProcessor
import com.example.data.local.MentionEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NewsProcessorTest {

    @Test
    fun testProcessMentionReturnsFivePartsStructuredJson() = runBlocking {
        val processor = NewsProcessor(apiKey = "MY_GEMINI_API_KEY")

        val mention = MentionEntity(
            mentionId = "m_test_1",
            storyId = "s_test_1",
            sourceId = "src_zoomit",
            sourceName = "زومیت فناوری",
            title = "رونمایی از نسل جدید موتور پردازش زنده هوش مصنوعی چندوجهی",
            content = "این مدل قادر به پردازش همزمان تصویر، متن و نظرات بازخوردی کاربران است.",
            publishedTime = System.currentTimeMillis(),
            url = "https://zoomit.ir/ai-test"
        )

        val result = processor.processMention(mention)

        // 1. summary
        assertNotNull(result.summary)
        assertTrue(result.summary.isNotBlank())

        // 2. impacts
        assertNotNull(result.impacts)
        assertTrue(result.impacts.isNotBlank())

        // 3. imagesPlaceholder
        assertNotNull(result.imagesPlaceholder)
        assertTrue(result.imagesPlaceholder.isNotEmpty())

        // 4. synthesizedViewpoints
        assertNotNull(result.synthesizedViewpoints)
        assertTrue(result.synthesizedViewpoints.isNotEmpty())
        val firstViewpoint = result.synthesizedViewpoints.first()
        assertTrue(firstViewpoint.perspective.isNotBlank())
        assertTrue(firstViewpoint.statement.isNotBlank())

        // 5. relatedNewsReferences
        assertNotNull(result.relatedNewsReferences)
        assertTrue(result.relatedNewsReferences.isNotEmpty())

        // Structured JSON conversion
        assertNotNull(result.rawJson)
        assertTrue(result.rawJson.startsWith("{"))
    }
}
