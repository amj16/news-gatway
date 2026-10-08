package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey
    val storyId: String,
    val title: String,
    val summary: String,
    val impact: String,
    val category: String,
    val importanceScore: Double, // 0.0 - 1.0
    val confidenceScore: Double, // 0.0 - 1.0
    val priorityScore: Double,   // Calculated priority
    val firstSeen: Long,
    val lastSeen: Long,
    val mentionCount: Int = 1,
    val primaryImageUrl: String? = null
)

@Entity(tableName = "mentions")
data class MentionEntity(
    @PrimaryKey
    val mentionId: String,
    val storyId: String,
    val sourceId: String,
    val sourceName: String,
    val title: String,
    val content: String,
    val publishedTime: Long,
    val url: String,
    val author: String? = null,
    val imageUrl: String? = null
)

@Entity(tableName = "evidences")
data class EvidenceEntity(
    @PrimaryKey
    val evidenceId: String,
    val mentionId: String,
    val storyId: String,
    val entityOrClaim: String,
    val evidenceType: String, // e.g., "PERSON", "ORG", "LOC", "FACT", "TIMELINE"
    val source: String,
    val confidence: Double,
    val verificationStatus: String // "VERIFIED", "DISPUTED", "REPORTED"
)

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey
    val sourceId: String,
    val name: String,
    val feedUrl: String,
    val category: String,
    val isActive: Boolean = true,
    val trustScore: Double = 0.85,
    val lastFetchedAt: Long = 0L
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey
    val bookmarkId: String,
    val storyId: String,
    val customTag: String, // "مهم", "خوانده نشده", "فوری", or custom category
    val note: String = "",
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "system_logs")
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val logId: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceId: String? = null,
    val sourceName: String? = null,
    val newsTitle: String? = null,
    val level: String = "INFO", // "INFO", "WARN", "ERROR"
    val message: String,
    val detail: String? = null
)

data class PublicOpinion(
    val author: String,
    val sentiment: String, // "POSITIVE", "NEUTRAL", "CRITICAL"
    val text: String,
    val sourcePlatform: String,
    val engagement: String
)

data class RelatedNewsSummary(
    val title: String,
    val source: String,
    val timeAgo: String,
    val reason: String // "رویداد مشترک", "موجودیت یکسان", "پیامد موازی"
)

data class FivePartAnalysis(
    val summary: String,
    val impact: String,
    val images: List<String>,
    val publicOpinions: List<PublicOpinion>,
    val relatedNews: List<RelatedNewsSummary>
)
