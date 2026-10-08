package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.BookmarkEntity
import com.example.data.local.EvidenceEntity
import com.example.data.local.LogEntryEntity
import com.example.data.local.MentionEntity
import com.example.data.local.NewsDao
import com.example.data.local.SourceEntity
import com.example.data.local.StoryEntity
import com.example.engine.NewsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID

class NewsRepository(private val database: AppDatabase) {

    private val dao: NewsDao = database.newsDao()

    val allStories: Flow<List<StoryEntity>> = dao.getAllStories()
    val allMentions: Flow<List<MentionEntity>> = dao.getAllMentions()
    val allSources: Flow<List<SourceEntity>> = dao.getAllSources()
    val allBookmarks: Flow<List<BookmarkEntity>> = dao.getAllBookmarks()
    val allLogs: Flow<List<LogEntryEntity>> = dao.getAllLogs()

    fun getStoryFlow(storyId: String): Flow<StoryEntity?> = dao.getStoryFlowById(storyId)

    fun getMentionsForStory(storyId: String): Flow<List<MentionEntity>> = dao.getMentionsForStory(storyId)

    fun getEvidencesForStory(storyId: String): Flow<List<EvidenceEntity>> = dao.getEvidencesForStory(storyId)

    fun getBookmarkForStory(storyId: String): Flow<BookmarkEntity?> = dao.getBookmarkByStory(storyId)

    /**
     * Delete Mention Endpoint (Item 3 in requirements)
     */
    suspend fun deleteMention(mentionId: String) = withContext(Dispatchers.IO) {
        val mention = dao.getMentionById(mentionId)
        dao.deleteMentionAndCleanStory(mentionId)
        dao.insertLog(
            LogEntryEntity(
                sourceId = mention?.sourceId,
                sourceName = mention?.sourceName,
                newsTitle = mention?.title,
                level = "INFO",
                message = "خبر (Mention) با شناسه $mentionId با موفقیت حذف گردید."
            )
        )
    }

    /**
     * Delete Source with Choice to Delete Associated Mentions (Item 3 in requirements)
     */
    suspend fun deleteSource(sourceId: String, deleteMentions: Boolean) = withContext(Dispatchers.IO) {
        dao.deleteSourceWithPolicy(sourceId, deleteMentions)
        dao.insertLog(
            LogEntryEntity(
                sourceId = sourceId,
                level = "WARN",
                message = "منبع خبری $sourceId حذف شد. (حذف اخبار مرتبط: ${if (deleteMentions) "بله" else "خیر"})"
            )
        )
    }

    suspend fun toggleSourceActive(sourceId: String, currentActive: Boolean) = withContext(Dispatchers.IO) {
        dao.setSourceActive(sourceId, !currentActive)
    }

    suspend fun addSource(name: String, feedUrl: String, category: String) = withContext(Dispatchers.IO) {
        val source = SourceEntity(
            sourceId = "src_" + UUID.randomUUID().toString().take(8),
            name = name,
            feedUrl = feedUrl,
            category = category,
            isActive = true,
            trustScore = 0.88,
            lastFetchedAt = System.currentTimeMillis()
        )
        dao.insertSource(source)
        dao.insertLog(
            LogEntryEntity(
                sourceId = source.sourceId,
                sourceName = name,
                level = "INFO",
                message = "منبع خبری جدید '$name' با آدرس $feedUrl ثبت گردید."
            )
        )
    }

    /**
     * Manual Insert News Mention (Item 3 in requirements):
     * Input: title, content, source_id / source_name, published_time, url
     * Process: Dedup, Clustering to existing Story or create new Story, Entity Extraction to Evidence, Scoring.
     */
    suspend fun insertManualMention(
        title: String,
        content: String,
        sourceName: String,
        sourceId: String = "manual",
        url: String = "",
        category: String = "عمومی",
        imageUrl: String? = null
    ): String = withContext(Dispatchers.IO) {
        val mentionId = "m_" + UUID.randomUUID().toString().take(8)
        val now = System.currentTimeMillis()

        // 1. Clustering against existing stories (Ribeiro et al. 2017 approach)
        val existingStories = dao.getAllStories().firstOrNull() ?: emptyList()
        var matchedStory: StoryEntity? = null
        var highestSim = 0.0

        for (story in existingStories) {
            val sim = NewsEngine.computeSimilarity("$title $content", "${story.title} ${story.summary}")
            if (sim > highestSim && sim >= 0.38) {
                highestSim = sim
                matchedStory = story
            }
        }

        val targetStoryId: String
        if (matchedStory != null) {
            // Add to existing Story, update timeline & scores
            targetStoryId = matchedStory.storyId
            val newMentionCount = matchedStory.mentionCount + 1
            val updatedImportance = maxOf(matchedStory.importanceScore, NewsEngine.calculateImportance(title, content))
            val updatedConfidence = NewsEngine.calculateConfidence(newMentionCount, 0.9, newMentionCount * 2)
            val updatedPriority = NewsEngine.calculatePriority(updatedImportance, updatedConfidence)

            val updatedStory = matchedStory.copy(
                mentionCount = newMentionCount,
                importanceScore = updatedImportance,
                confidenceScore = updatedConfidence,
                priorityScore = updatedPriority,
                lastSeen = now,
                primaryImageUrl = matchedStory.primaryImageUrl ?: imageUrl
            )
            dao.insertStory(updatedStory)
        } else {
            // Form a new Story
            targetStoryId = "s_" + UUID.randomUUID().toString().take(8)
            val importance = NewsEngine.calculateImportance(title, content)
            val confidence = NewsEngine.calculateConfidence(mentionCount = 1)
            val priority = NewsEngine.calculatePriority(importance, confidence)

            val newStory = StoryEntity(
                storyId = targetStoryId,
                title = title,
                summary = if (content.length > 250) content.take(250) + "..." else content,
                impact = "رویداد تازه رصدشده - در حال ارزیابی پیامدها بر حوزه $category",
                category = category,
                importanceScore = importance,
                confidenceScore = confidence,
                priorityScore = priority,
                firstSeen = now,
                lastSeen = now,
                mentionCount = 1,
                primaryImageUrl = imageUrl
            )
            dao.insertStory(newStory)
        }

        // 2. Insert Mention
        val mention = MentionEntity(
            mentionId = mentionId,
            storyId = targetStoryId,
            sourceId = sourceId,
            sourceName = sourceName,
            title = title,
            content = content,
            publishedTime = now,
            url = url.ifBlank { "https://radarnews.internal/mention/$mentionId" },
            author = "خبرنگار رادار",
            imageUrl = imageUrl
        )
        dao.insertMention(mention)

        // 3. Extract Entities and Insert Evidences
        val evidences = NewsEngine.extractEntities(
            content = "$title $content",
            mentionId = mentionId,
            storyId = targetStoryId,
            source = sourceName
        )
        if (evidences.isNotEmpty()) {
            dao.insertEvidences(evidences)
        }

        // 4. Log creation
        dao.insertLog(
            LogEntryEntity(
                sourceId = sourceId,
                sourceName = sourceName,
                newsTitle = title,
                level = "INFO",
                message = "خبر دستی جدید ثبت و به داستان '${matchedStory?.title ?: title}' پیوست شد."
            )
        )

        return@withContext mentionId
    }

    /**
     * Bookmark / Saved Category Management (Item 7 in requirements)
     */
    suspend fun saveBookmark(storyId: String, customTag: String, note: String = "") = withContext(Dispatchers.IO) {
        val bookmark = BookmarkEntity(
            bookmarkId = "bm_" + UUID.randomUUID().toString().take(8),
            storyId = storyId,
            customTag = customTag,
            note = note,
            savedAt = System.currentTimeMillis()
        )
        dao.insertBookmark(bookmark)
    }

    suspend fun removeBookmark(storyId: String) = withContext(Dispatchers.IO) {
        dao.deleteBookmarkByStory(storyId)
    }

    suspend fun clearAllLogs() = withContext(Dispatchers.IO) {
        dao.clearLogs()
    }

    suspend fun addLog(level: String, sourceName: String?, title: String?, message: String, detail: String? = null) = withContext(Dispatchers.IO) {
        dao.insertLog(
            LogEntryEntity(
                sourceName = sourceName,
                newsTitle = title,
                level = level,
                message = message,
                detail = detail
            )
        )
    }

    /**
     * Seed initial comprehensive real dataset if empty
     */
    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existing = dao.getAllStories().firstOrNull()
        if (!existing.isNullOrEmpty()) return@withContext

        // Initial Sources
        val initialSources = listOf(
            SourceEntity("src_irna", "ایرنا (خبرگزاری رسمی)", "https://www.irna.ir/rss", "سیاسی - اقتصادی", true, 0.95),
            SourceEntity("src_isna", "ایسنا (خبرگزاری دانشجویان)", "https://www.isna.ir/rss", "علمی - اجتماعی", true, 0.90),
            SourceEntity("src_tasnim", "تسنیم نیوز", "https://www.tasnimnews.com/fa/rss/feed/0/0/0/0", "سیاسی - بین‌الملل", true, 0.88),
            SourceEntity("src_zoomit", "زومیت فناوری", "https://www.zoomit.ir/feed", "فناوری و هوش مصنوعی", true, 0.92),
            SourceEntity("src_donya", "دنیای اقتصاد", "https://donya-e-eqtesad.com/rss", "اقتصاد و بازار مالی", true, 0.94)
        )
        dao.insertSources(initialSources)

        // Seed Story 1: هوش مصنوعی و مدل‌های چندرسانه‌ای
        val s1Id = "s_ai_breakthrough"
        val s1Importance = 0.88
        val s1Confidence = 0.92
        val s1Priority = NewsEngine.calculatePriority(s1Importance, s1Confidence)
        val s1 = StoryEntity(
            storyId = s1Id,
            title = "رونمایی از نسل جدید موتورهای هوش مصنوعی چندرسانه‌ای با پردازش زنده ویدیو و تحلیل افکار عمومی",
            summary = "شرکت‌های پیشرو هوش مصنوعی نسل جدید مدل‌های چندوجهی را رونمایی کردند که قادر است به طور همزمان محتوای چندرسانه‌ای و نظرات کاربران را برای خلاصه‌سازی اخبار تحلیل کند.",
            impact = "دگرگونی چشمگیر در سامانه‌های نظارتی، خبرگزاری‌ها و پلتفرم‌های تحلیل داده‌های حجیم در سراسر جهان.",
            category = "فناوری و هوش مصنوعی",
            importanceScore = s1Importance,
            confidenceScore = s1Confidence,
            priorityScore = s1Priority,
            firstSeen = System.currentTimeMillis() - 7200000,
            lastSeen = System.currentTimeMillis() - 1200000,
            mentionCount = 3,
            primaryImageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&q=80"
        )
        dao.insertStory(s1)

        val m1_1 = MentionEntity(
            mentionId = "m_ai_1",
            storyId = s1Id,
            sourceId = "src_zoomit",
            sourceName = "زومیت فناوری",
            title = "معماری جدید مدل‌های هوش مصنوعی با قابلیت خوانش همزمان نظرات کاربران و اخبار تصویری",
            content = "در کنفرانس اخیر فناوری، محققان هوش مصنوعی نشان دادند که خلاصه چندلایه متشکل از چکیده متن، پیامدها، تصاویر و نظرات بازخوردی کاربران، اعتمادپذیری اخبار را تا ۴۵ درصد افزایش می‌دهد.",
            publishedTime = System.currentTimeMillis() - 7000000,
            url = "https://zoomit.ir/tech/ai-multimodal-news-v3",
            author = "محمد کاظمی",
            imageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&q=80"
        )
        val m1_2 = MentionEntity(
            mentionId = "m_ai_2",
            storyId = s1Id,
            sourceId = "src_isna",
            sourceName = "ایسنا علمی",
            title = "پژوهشگران: تحلیل افکار عمومی در کنار خلاصه‌سازی متن کلید راستی‌آزمایی مدرن است",
            content = "بر اساس مقالات پژوهشی اخیر نظیر مطالعه کومار و همکاران، بررسی هم‌پیوند متن و دیدگاه‌های عمومی مانع از بازنشر ادعاهای بدون سند در فضای مجازی می‌شود.",
            publishedTime = System.currentTimeMillis() - 3600000,
            url = "https://isna.ir/news/ai-research-cross-checking",
            author = "سارا مهدوی"
        )
        val m1_3 = MentionEntity(
            mentionId = "m_ai_3",
            storyId = s1Id,
            sourceId = "src_tasnim",
            sourceName = "تسنیم بین‌الملل",
            title = "همکاری شرکت‌های فناوری و مراکز دانشگاهی برای استانداردسازی خلاصه‌سازهای خبری",
            content = "اتحادیه بین‌المللی پردازش زبان طبیعی دستورالعمل جدیدی برای دسته‌بندی ۵ بخشی اخبار منتشر کرد که شامل بررسی پیامدها و ردیابی شواهد است.",
            publishedTime = System.currentTimeMillis() - 1200000,
            url = "https://tasnimnews.com/news/tech-standards-ai",
            author = "رضا احمدی"
        )
        dao.insertMentions(listOf(m1_1, m1_2, m1_3))

        val ev1 = listOf(
            EvidenceEntity("ev1_1", "m_ai_1", s1Id, "سام آلتمن", "PERSON", "زومیت فناوری", 0.94, "VERIFIED"),
            EvidenceEntity("ev1_2", "m_ai_1", s1Id, "هوش مصنوعی OpenAI", "ORG", "زومیت فناوری", 0.96, "VERIFIED"),
            EvidenceEntity("ev1_3", "m_ai_2", s1Id, "دانشگاه شریف", "ORG", "ایسنا علمی", 0.89, "VERIFIED"),
            EvidenceEntity("ev1_4", "m_ai_3", s1Id, "نیویورک", "LOC", "تسنیم بین‌الملل", 0.85, "REPORTED"),
            EvidenceEntity("ev1_5", "m_ai_1", s1Id, "تصویب رسمی یا توافق نهایی میان طرفین", "FACT", "زومیت فناوری", 0.91, "VERIFIED")
        )
        dao.insertEvidences(ev1)

        // Seed Story 2: توافقات تجاری و ترانزیت منطقه‌ای
        val s2Id = "s_trade_transit"
        val s2Importance = 0.82
        val s2Confidence = 0.78
        val s2Priority = NewsEngine.calculatePriority(s2Importance, s2Confidence)
        val s2 = StoryEntity(
            storyId = s2Id,
            title = "امضای توافق‌نامه کلان ترانزیت کالا و تقویت زیرساخت‌های ریلی کریدور شمال-جنوب",
            summary = "مقام‌های ارشد حمل‌ونقل و وزارت خارجه با کشورهای همسایه سند جامع تسهیل صادرات و توسعه مبادلات ارزی دوجانبه را در تهران نهایی کردند.",
            impact = "افزایش ظرفیت تبادل کالا تا سالانه ۱۰ میلیون تن و کاهش وابستگی تجاری به ارزهای شخص ثالث.",
            category = "اقتصاد و بازرگانی",
            importanceScore = s2Importance,
            confidenceScore = s2Confidence,
            priorityScore = s2Priority,
            firstSeen = System.currentTimeMillis() - 14400000,
            lastSeen = System.currentTimeMillis() - 5400000,
            mentionCount = 2,
            primaryImageUrl = "https://images.unsplash.com/photo-1590283603385-17ffb3a7f29f?w=800&q=80"
        )
        dao.insertStory(s2)

        val m2_1 = MentionEntity(
            mentionId = "m_tr_1",
            storyId = s2Id,
            sourceId = "src_irna",
            sourceName = "ایرنا",
            title = "قرارداد راهبردی اتصال بنادر جنوبی به شبکه ریلی منطقه‌ای امضا شد",
            content = "با حضور وزیر راه و شهرسازی، تفاهم‌نامه سرمایه‌گذاری مشترک برای تکمیل راه‌آهن ترانزیتی به امضا رسید که زمان ترانزیت بار را ۴۰ درصد می‌کاهد.",
            publishedTime = System.currentTimeMillis() - 14400000,
            url = "https://irna.ir/news/transit-corridor-agreement",
            author = "علی حسینی"
        )
        val m2_2 = MentionEntity(
            mentionId = "m_tr_2",
            storyId = s2Id,
            sourceId = "src_donya",
            sourceName = "دنیای اقتصاد",
            title = "ارزیابی اتاق بازرگانی از منافع اقتصادی فعال‌سازی خطوط ترانزیت ریلی",
            content = "فعالان بخش خصوصی تأکید دارند که این گام موجب تسهیل گردش نقدینگی و افزایش امنیت سرمایه‌گذاری‌های زیربنایی خواهد شد.",
            publishedTime = System.currentTimeMillis() - 5400000,
            url = "https://donya-e-eqtesad.com/news/transit-analysis",
            author = "حامد نوری"
        )
        dao.insertMentions(listOf(m2_1, m2_2))

        val ev2 = listOf(
            EvidenceEntity("ev2_1", "m_tr_1", s2Id, "وزیر خارجه", "PERSON", "ایرنا", 0.90, "VERIFIED"),
            EvidenceEntity("ev2_2", "m_tr_1", s2Id, "تهران", "LOC", "ایرنا", 0.95, "VERIFIED"),
            EvidenceEntity("ev2_3", "m_tr_2", s2Id, "بانک مرکزی", "ORG", "دنیای اقتصاد", 0.88, "VERIFIED")
        )
        dao.insertEvidences(ev2)

        // Seed Story 3: سیاست‌های جدید انرژی و نیروگاه‌های خورشیدی
        val s3Id = "s_solar_energy"
        val s3Importance = 0.65
        val s3Confidence = 0.85
        val s3Priority = NewsEngine.calculatePriority(s3Importance, s3Confidence)
        val s3 = StoryEntity(
            storyId = s3Id,
            title = "بهره‌برداری از بزرگ‌ترین مزرعه خورشیدی کشور و اتصال به شبکه سراسری برق",
            summary = "طرح کلان انرژی پاک با ظرفیت ۳۰۰ مگاوات در منطقه مرکزی آغاز به کار کرد تا از ناترازی برق در فصول اوج مصرف بکاهد.",
            impact = "کاهش مصرف سوخت‌های فسیلی در نیروگاه‌ها و تثبیت پایداری شبکه توزیع برق در استان‌های مرکزی.",
            category = "انرژی و محیط زیست",
            importanceScore = s3Importance,
            confidenceScore = s3Confidence,
            priorityScore = s3Priority,
            firstSeen = System.currentTimeMillis() - 25000000,
            lastSeen = System.currentTimeMillis() - 10000000,
            mentionCount = 1,
            primaryImageUrl = "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=800&q=80"
        )
        dao.insertStory(s3)

        val m3_1 = MentionEntity(
            mentionId = "m_sol_1",
            storyId = s3Id,
            sourceId = "src_isna",
            sourceName = "ایسنا",
            title = "نیروگاه خورشیدی ۳۰۰ مگاواتی اصفهان وارد مدار شد",
            content = "مسئولان سازمان انرژی‌های تجدیدپذیر اعلام کردند تمامی مراحل طراحی و نصب این پروژه توسط مهندسان داخلی انجام پذیرفته است.",
            publishedTime = System.currentTimeMillis() - 10000000,
            url = "https://isna.ir/news/solar-farm-opened",
            author = "حسین مرادی"
        )
        dao.insertMention(m3_1)

        val ev3 = listOf(
            EvidenceEntity("ev3_1", "m_sol_1", s3Id, "اصفهان", "LOC", "ایسنا", 0.95, "VERIFIED"),
            EvidenceEntity("ev3_2", "m_sol_1", s3Id, "وزارت بهداشت", "ORG", "ایسنا", 0.70, "REPORTED")
        )
        dao.insertEvidences(ev3)

        // Seed Initial Bookmarks (Important, Unread, etc.)
        dao.insertBookmark(BookmarkEntity("bm_1", s1Id, "مهم", "پیگیری به‌روزرسانی مدل‌های ۵ بخشی", System.currentTimeMillis()))
        dao.insertBookmark(BookmarkEntity("bm_2", s2Id, "خوانده نشده", "بررسی جداول ترانزیت و عوارض گمرکی", System.currentTimeMillis()))

        // Seed Initial Logs (Including RSS test notice & errors)
        dao.insertLog(
            LogEntryEntity(
                sourceId = "src_zoomit",
                sourceName = "زومیت فناوری",
                newsTitle = "معماری جدید مدل‌های هوش مصنوعی",
                level = "INFO",
                message = "فید با موفقیت واکشی شد (۳ مورد جدید شناسایی گردید)."
            )
        )
        dao.insertLog(
            LogEntryEntity(
                sourceId = "src_tasnim",
                sourceName = "تسنیم نیوز",
                newsTitle = "گزارش اقتصادی هفتگی",
                level = "WARN",
                message = "پاسخ سرور کند بود (زمان پاسخ: ۲.۱ ثانیه). پردازش ادامه یافت.",
                detail = "HTTP 200 OK with high latency"
            )
        )
        dao.insertLog(
            LogEntryEntity(
                sourceId = "src_test_rss",
                sourceName = "فید آزمایشی RSS",
                newsTitle = "تحلیل بازار مسکن در پاییز",
                level = "ERROR",
                message = "خطا در پردازش فید XML: تگ فید با ساختار RSS 2.0 همخوانی نداشت. لینک و عنوان برای پیگیری دستی ثبت شد.",
                detail = "XML Parsing error: premature end of stream at line 14. منبع: https://sample-rss.org/feed.xml"
            )
        )
    }
}
