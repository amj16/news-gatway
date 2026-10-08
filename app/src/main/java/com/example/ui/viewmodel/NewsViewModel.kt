package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.BookmarkEntity
import com.example.data.local.EvidenceEntity
import com.example.data.local.LogEntryEntity
import com.example.data.local.MentionEntity
import com.example.data.local.SourceEntity
import com.example.data.local.StoryEntity
import com.example.data.repository.NewsRepository
import com.example.engine.NewsEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NewsFilterState(
    val searchQuery: String = "",
    val selectedCategory: String = "همه",
    val minPriority: String = "ALL", // "ALL", "HIGH", "MEDIUM", "LOW"
    val bookmarkFilterTag: String = "ALL" // "ALL", "مهم", "خوانده نشده", "فوری"
)

class NewsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NewsRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = NewsRepository(db)
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    val allStories: StateFlow<List<StoryEntity>> = repository.allStories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMentions: StateFlow<List<MentionEntity>> = repository.allMentions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSources: StateFlow<List<SourceEntity>> = repository.allSources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookmarks: StateFlow<List<BookmarkEntity>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<LogEntryEntity>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filterState = MutableStateFlow(NewsFilterState())
    val filterState: StateFlow<NewsFilterState> = _filterState.asStateFlow()

    // Filtered Stories
    val filteredStories: StateFlow<List<StoryEntity>> = combine(allStories, _filterState) { stories, filter ->
        stories.filter { story ->
            val matchQuery = filter.searchQuery.isBlank() ||
                    story.title.contains(filter.searchQuery, ignoreCase = true) ||
                    story.summary.contains(filter.searchQuery, ignoreCase = true) ||
                    story.category.contains(filter.searchQuery, ignoreCase = true)

            val matchCategory = filter.selectedCategory == "همه" || story.category.contains(filter.selectedCategory)

            val matchPriority = when (filter.minPriority) {
                "HIGH" -> story.priorityScore >= NewsEngine.THRESHOLD_HIGH
                "MEDIUM" -> story.priorityScore >= NewsEngine.THRESHOLD_MEDIUM
                "LOW" -> story.priorityScore >= NewsEngine.THRESHOLD_LOW
                else -> true
            }

            matchQuery && matchCategory && matchPriority
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Story for Detail/5-Part Analysis
    private val _selectedStoryId = MutableStateFlow<String?>(null)
    val selectedStoryId: StateFlow<String?> = _selectedStoryId.asStateFlow()

    // Dialog & UI Action States
    private val _showManualAddDialog = MutableStateFlow(false)
    val showManualAddDialog: StateFlow<Boolean> = _showManualAddDialog.asStateFlow()

    private val _sourceToDelete = MutableStateFlow<SourceEntity?>(null)
    val sourceToDelete: StateFlow<SourceEntity?> = _sourceToDelete.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun updateSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun updateCategory(category: String) {
        _filterState.value = _filterState.value.copy(selectedCategory = category)
    }

    fun updateMinPriority(priority: String) {
        _filterState.value = _filterState.value.copy(minPriority = priority)
    }

    fun selectStory(storyId: String?) {
        _selectedStoryId.value = storyId
    }

    fun openManualAddDialog(show: Boolean) {
        _showManualAddDialog.value = show
    }

    fun confirmDeleteSource(source: SourceEntity?) {
        _sourceToDelete.value = source
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun deleteMention(mentionId: String) {
        viewModelScope.launch {
            repository.deleteMention(mentionId)
            _statusMessage.value = "خبر (Mention) با موفقیت حذف شد."
        }
    }

    fun executeDeleteSource(sourceId: String, deleteMentions: Boolean) {
        viewModelScope.launch {
            repository.deleteSource(sourceId, deleteMentions)
            _sourceToDelete.value = null
            _statusMessage.value = "منبع خبری با موفقیت حذف شد."
        }
    }

    fun toggleSourceStatus(source: SourceEntity) {
        viewModelScope.launch {
            repository.toggleSourceActive(source.sourceId, source.isActive)
        }
    }

    fun addNewSource(name: String, feedUrl: String, category: String) {
        viewModelScope.launch {
            repository.addSource(name, feedUrl, category)
            _statusMessage.value = "منبع خبری جدید '$name' افزوده شد."
        }
    }

    fun addManualNews(
        title: String,
        content: String,
        sourceName: String,
        url: String,
        category: String
    ) {
        viewModelScope.launch {
            repository.insertManualMention(
                title = title,
                content = content,
                sourceName = sourceName,
                url = url,
                category = category
            )
            _showManualAddDialog.value = false
            _statusMessage.value = "خبر جدید با موفقیت تحلیل، خوشه‌بندی و افزوده شد."
        }
    }

    fun toggleBookmark(storyId: String, currentTag: String?, customTag: String = "مهم") {
        viewModelScope.launch {
            if (currentTag != null) {
                repository.removeBookmark(storyId)
                _statusMessage.value = "خبر از نشان‌شده‌ها خارج شد."
            } else {
                repository.saveBookmark(storyId, customTag)
                _statusMessage.value = "خبر در دسته '$customTag' ذخیره شد."
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
            _statusMessage.value = "تاریخچه لاگ‌ها پاکسازی شد."
        }
    }

    fun getStoryMentions(storyId: String) = repository.getMentionsForStory(storyId)
    fun getStoryEvidences(storyId: String) = repository.getEvidencesForStory(storyId)
    fun getStoryBookmark(storyId: String) = repository.getBookmarkForStory(storyId)
}
