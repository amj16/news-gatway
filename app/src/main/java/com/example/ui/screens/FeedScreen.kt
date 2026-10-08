package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.StoryEntity
import com.example.ui.components.StoryCard
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.CardBackground
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HighPriorityRed
import com.example.ui.theme.MediumPriorityAmber
import com.example.ui.theme.SoftCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.NewsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedScreen(
    viewModel: NewsViewModel,
    onStoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val stories by viewModel.filteredStories.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val bookmarks by viewModel.allBookmarks.collectAsStateWithLifecycle()

    val categories = listOf("همه", "فناوری و هوش مصنوعی", "اقتصاد و بازرگانی", "سیاسی", "انرژی و محیط زیست")
    val priorityFilters = listOf(
        "ALL" to "همه اولویت‌ها",
        "HIGH" to "فقط اولویت بالا (High)",
        "MEDIUM" to "اولویت متوسط به بالا",
        "LOW" to "همه سطوح"
    )

    val bookmarkMap = bookmarks.associateBy { it.storyId }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar
                OutlinedTextField(
                    value = filterState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_news_input"),
                    placeholder = { Text("جستجو در داستان‌ها، شواهد و مفاهیم...", color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "جستجو",
                            tint = AccentCyan
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = filterState.selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) AccentCyan else CardBackground)
                                .clickable { viewModel.updateCategory(category) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("category_filter_$category")
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) Color.Black else TextWhite,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Priority Score Filters (Importance vs Confidence Thresholds)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(priorityFilters) { (key, label) ->
                        val isSelected = filterState.minPriority == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ElectricBlue else Color.White.copy(alpha = 0.05f))
                                .clickable { viewModel.updateMinPriority(key) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("priority_filter_$key")
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of Stories (Grouped Mentions with Timeline & Scores)
                if (stories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "هیچ داستانی با این فیلترها یافت نشد.",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(stories, key = { it.storyId }) { story ->
                            StoryCard(
                                story = story,
                                bookmark = bookmarkMap[story.storyId],
                                onStoryClick = { onStoryClick(story.storyId) },
                                onBookmarkClick = {
                                    viewModel.toggleBookmark(
                                        story.storyId,
                                        bookmarkMap[story.storyId]?.customTag
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Floating Action Button for Manual News Insertion (Item 3 & 4)
            FloatingActionButton(
                onClick = { viewModel.openManualAddDialog(true) },
                containerColor = AccentCyan,
                contentColor = Color.Black,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
                    .testTag("fab_add_news")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "درج دستی خبر")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("درج دستی خبر", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
