package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {

    // --- Story Operations ---
    @Query("SELECT * FROM stories ORDER BY lastSeen DESC")
    fun getAllStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE storyId = :storyId")
    suspend fun getStoryById(storyId: String): StoryEntity?

    @Query("SELECT * FROM stories WHERE storyId = :storyId")
    fun getStoryFlowById(storyId: String): Flow<StoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<StoryEntity>)

    @Query("DELETE FROM stories WHERE storyId = :storyId")
    suspend fun deleteStory(storyId: String)

    // --- Mention Operations ---
    @Query("SELECT * FROM mentions ORDER BY publishedTime DESC")
    fun getAllMentions(): Flow<List<MentionEntity>>

    @Query("SELECT * FROM mentions WHERE storyId = :storyId ORDER BY publishedTime ASC")
    fun getMentionsForStory(storyId: String): Flow<List<MentionEntity>>

    @Query("SELECT * FROM mentions WHERE storyId = :storyId ORDER BY publishedTime ASC")
    suspend fun getMentionsListForStory(storyId: String): List<MentionEntity>

    @Query("SELECT * FROM mentions WHERE mentionId = :mentionId")
    suspend fun getMentionById(mentionId: String): MentionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMention(mention: MentionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMentions(mentions: List<MentionEntity>)

    @Query("DELETE FROM mentions WHERE mentionId = :mentionId")
    suspend fun deleteMention(mentionId: String)

    @Query("DELETE FROM mentions WHERE sourceId = :sourceId")
    suspend fun deleteMentionsBySource(sourceId: String)

    @Query("SELECT COUNT(*) FROM mentions WHERE storyId = :storyId")
    suspend fun countMentionsForStory(storyId: String): Int

    // --- Evidence Operations ---
    @Query("SELECT * FROM evidences WHERE storyId = :storyId")
    fun getEvidencesForStory(storyId: String): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidences WHERE mentionId = :mentionId")
    suspend fun getEvidencesForMention(mentionId: String): List<EvidenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidences(evidences: List<EvidenceEntity>)

    @Query("DELETE FROM evidences WHERE mentionId = :mentionId")
    suspend fun deleteEvidencesByMention(mentionId: String)

    @Query("DELETE FROM evidences WHERE storyId = :storyId")
    suspend fun deleteEvidencesByStory(storyId: String)

    // --- Source Operations ---
    @Query("SELECT * FROM sources ORDER BY name ASC")
    fun getAllSources(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources WHERE isActive = 1")
    suspend fun getActiveSourcesList(): List<SourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: SourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSources(sources: List<SourceEntity>)

    @Query("DELETE FROM sources WHERE sourceId = :sourceId")
    suspend fun deleteSource(sourceId: String)

    @Query("UPDATE sources SET isActive = :isActive WHERE sourceId = :sourceId")
    suspend fun setSourceActive(sourceId: String, isActive: Boolean)

    // --- Bookmark Operations ---
    @Query("SELECT * FROM bookmarks ORDER BY savedAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE storyId = :storyId")
    fun getBookmarkByStory(storyId: String): Flow<BookmarkEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE storyId = :storyId")
    suspend fun deleteBookmarkByStory(storyId: String)

    @Query("DELETE FROM bookmarks WHERE bookmarkId = :bookmarkId")
    suspend fun deleteBookmark(bookmarkId: String)

    // --- Log Operations ---
    @Query("SELECT * FROM system_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<LogEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntryEntity)

    @Query("DELETE FROM system_logs")
    suspend fun clearLogs()

    // --- Complex Composite Actions ---
    @Transaction
    suspend fun deleteMentionAndCleanStory(mentionId: String) {
        val mention = getMentionById(mentionId) ?: return
        deleteEvidencesByMention(mentionId)
        deleteMention(mentionId)

        val remaining = countMentionsForStory(mention.storyId)
        if (remaining == 0) {
            deleteStory(mention.storyId)
            deleteBookmarkByStory(mention.storyId)
            deleteEvidencesByStory(mention.storyId)
        } else {
            val story = getStoryById(mention.storyId)
            if (story != null) {
                insertStory(story.copy(mentionCount = remaining))
            }
        }
    }

    @Transaction
    suspend fun deleteSourceWithPolicy(sourceId: String, deleteMentions: Boolean) {
        deleteSource(sourceId)
        if (deleteMentions) {
            deleteMentionsBySource(sourceId)
        }
    }
}
