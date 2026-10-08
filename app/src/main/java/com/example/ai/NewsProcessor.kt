package com.example.ai

import com.example.BuildConfig
import com.example.data.local.FivePartAnalysis
import com.example.data.local.MentionEntity
import com.example.data.local.PublicOpinion
import com.example.data.local.RelatedNewsSummary
import com.example.engine.NewsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Structured response containing the five required parts:
 * 1. summary: A synthesized, comprehensive news summary
 * 2. impacts: Analysis of direct/indirect repercussions across relevant sectors
 * 3. images: Curated image placeholders / search references
 * 4. synthesizedViewpoints: Synthesized multi-perspective public opinion / social sentiments
 * 5. relatedNewsReferences: Related news topics, shared event references, and background links
 */
data class NewsProcessorResult(
    val summary: String,
    val impacts: String,
    val imagesPlaceholder: List<String>,
    val synthesizedViewpoints: List<SynthesizedViewpoint>,
    val relatedNewsReferences: List<RelatedNewsReference>,
    val rawJson: String
) {
    fun toFivePartAnalysis(): FivePartAnalysis {
        return FivePartAnalysis(
            summary = summary,
            impact = impacts,
            images = imagesPlaceholder,
            publicOpinions = synthesizedViewpoints.map {
                PublicOpinion(
                    author = it.perspective,
                    sentiment = it.sentiment,
                    text = it.statement,
                    sourcePlatform = it.platform,
                    engagement = it.engagementMetrics
                )
            },
            relatedNews = relatedNewsReferences.map {
                RelatedNewsSummary(
                    title = it.title,
                    source = it.source,
                    timeAgo = it.timeframe,
                    reason = it.relationReason
                )
            }
        )
    }
}

data class SynthesizedViewpoint(
    val perspective: String,
    val sentiment: String, // "POSITIVE", "NEUTRAL", "CRITICAL"
    val statement: String,
    val platform: String,
    val engagementMetrics: String
)

data class RelatedNewsReference(
    val title: String,
    val source: String,
    val timeframe: String,
    val relationReason: String
)

class NewsProcessor(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Model selection based on skill rules: 'gemini-3.5-flash' for basic text/summarization tasks
    private val modelName = "gemini-3.5-flash"
    private val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

    /**
     * Takes a news mention and returns a structured JSON response containing the five required parts.
     */
    suspend fun processMention(mention: MentionEntity): NewsProcessorResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // High quality deterministic fallback when API key is not yet set in Secrets panel
            val fallback = NewsEngine.generateFivePartAnalysis(
                title = mention.title,
                content = mention.content,
                category = "تحلیل هوشمند",
                existingImageUrl = mention.imageUrl
            )
            val jsonObject = JSONObject().apply {
                put("summary", fallback.summary)
                put("impacts", fallback.impact)
                put("imagesPlaceholder", JSONArray(fallback.images))
                put("synthesizedViewpoints", JSONArray().apply {
                    fallback.publicOpinions.forEach { op ->
                        put(JSONObject().apply {
                            put("perspective", op.author)
                            put("sentiment", op.sentiment)
                            put("statement", op.text)
                            put("platform", op.sourcePlatform)
                            put("engagementMetrics", op.engagement)
                        })
                    }
                })
                put("relatedNewsReferences", JSONArray().apply {
                    fallback.relatedNews.forEach { rel ->
                        put(JSONObject().apply {
                            put("title", rel.title)
                            put("source", rel.source)
                            put("timeframe", rel.timeAgo)
                            put("relationReason", rel.reason)
                        })
                    }
                })
            }
            return@withContext parseJsonResponse(jsonObject.toString())
        }

        val prompt = """
            You are an advanced journalistic intelligence and fact-checking AI.
            Analyze the following news mention and produce a strict JSON response with 5 required parts:
            
            1. "summary": A well-crafted, objective summary of the main news content (one coherent paragraph).
            2. "impacts": Analysis of key repercussions, potential outcomes, and influence on economic, political, or social fields.
            3. "imagesPlaceholder": Array of 3 high-relevance descriptive image URLs or keywords (e.g. Unsplash photo URLs relevant to the topic).
            4. "synthesizedViewpoints": Array of 3 to 4 synthesized public opinions / social viewpoints with keys:
               - "perspective": e.g. "تحلیل‌گر اقتصادی", "افکار عمومی", "منتقد حوزه"
               - "sentiment": "POSITIVE", "NEUTRAL", or "CRITICAL"
               - "statement": The comment or reaction text in Persian.
               - "platform": e.g. "شبکه‌های اجتماعی", "دیدگاه‌های کاربران"
               - "engagementMetrics": e.g. "۴.۵ هزار بازدید • ۷۸٪ موافق"
            5. "relatedNewsReferences": Array of 2 to 3 related news references with keys:
               - "title": Related headline in Persian
               - "source": News source or research center
               - "timeframe": e.g. "ساعات گذشته", "روزهای اخیر"
               - "relationReason": e.g. "رویداد مشترک", "موجودیت یکسان"
            
            News Title: ${mention.title}
            Source: ${mention.sourceName}
            Content: ${mention.content}
            URL: ${mention.url}
            
            Return ONLY the valid JSON object without markdown code blocks.
        """.trimIndent()

        val requestPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.3)
            })
        }

        val url = "$endpoint?key=$apiKey"
        val body = requestPayload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                // If network/key error, gracefully fallback
                return@withContext fallbackResult(mention, "API Call returned ${response.code}: $responseBody")
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val contentObj = firstCandidate?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""

            parseJsonResponse(text)
        } catch (e: Exception) {
            fallbackResult(mention, e.message ?: "Unknown error")
        }
    }

    private fun parseJsonResponse(rawJson: String): NewsProcessorResult {
        val clean = rawJson.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val obj = JSONObject(clean)

        val summary = obj.optString("summary", "خلاصه در دست پردازش است.")
        val impacts = obj.optString("impacts", "پیامدهای رویداد در دست بررسی کارشناسی قرار دارد.")

        val imagesPlaceholder = mutableListOf<String>()
        obj.optJSONArray("imagesPlaceholder")?.let { arr ->
            for (i in 0 until arr.length()) {
                imagesPlaceholder.add(arr.getString(i))
            }
        }
        if (imagesPlaceholder.isEmpty()) {
            imagesPlaceholder.addAll(listOf(
                "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=800&q=80",
                "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&q=80",
                "https://images.unsplash.com/photo-1590283603385-17ffb3a7f29f?w=800&q=80"
            ))
        }

        val viewpoints = mutableListOf<SynthesizedViewpoint>()
        obj.optJSONArray("synthesizedViewpoints")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                viewpoints.add(
                    SynthesizedViewpoint(
                        perspective = item.optString("perspective", "دیدگاه عمومی"),
                        sentiment = item.optString("sentiment", "NEUTRAL"),
                        statement = item.optString("statement", ""),
                        platform = item.optString("platform", "شبکه‌های اجتماعی"),
                        engagementMetrics = item.optString("engagementMetrics", "پربازدید")
                    )
                )
            }
        }

        val relatedNews = mutableListOf<RelatedNewsReference>()
        obj.optJSONArray("relatedNewsReferences")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                relatedNews.add(
                    RelatedNewsReference(
                        title = item.optString("title", "خبر مرتبط"),
                        source = item.optString("source", "رسانه‌های جمعی"),
                        timeframe = item.optString("timeframe", "به‌تازگی"),
                        relationReason = item.optString("relationReason", "رویداد مشترک")
                    )
                )
            }
        }

        return NewsProcessorResult(
            summary = summary,
            impacts = impacts,
            imagesPlaceholder = imagesPlaceholder,
            synthesizedViewpoints = viewpoints,
            relatedNewsReferences = relatedNews,
            rawJson = clean
        )
    }

    private fun fallbackResult(mention: MentionEntity, reason: String): NewsProcessorResult {
        val fallback = NewsEngine.generateFivePartAnalysis(
            title = mention.title,
            content = mention.content,
            category = "عمومی",
            existingImageUrl = mention.imageUrl
        )
        return NewsProcessorResult(
            summary = fallback.summary,
            impacts = fallback.impact,
            imagesPlaceholder = fallback.images,
            synthesizedViewpoints = fallback.publicOpinions.map {
                SynthesizedViewpoint(it.author, it.sentiment, it.text, it.sourcePlatform, it.engagement)
            },
            relatedNewsReferences = fallback.relatedNews.map {
                RelatedNewsReference(it.title, it.source, it.timeAgo, it.reason)
            },
            rawJson = JSONObject().apply {
                put("summary", fallback.summary)
                put("impacts", fallback.impact)
                put("note", "Fallback generated due to: $reason")
            }.toString()
        )
    }
}
