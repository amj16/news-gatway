package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.components.StoryCard
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.CardBackground
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HighPriorityRed
import com.example.ui.theme.LowPriorityGreen
import com.example.ui.theme.MediumPriorityAmber
import com.example.ui.theme.SoftCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.NewsViewModel

/**
 * Saved & Categorized News Screen (فضای ذخیره‌سازی اخبار به عناوین مختلف: مهم، خوانده نشده، سفارشی)
 * Fulfills Requirement 7:
 * "یک فضایی رو داشته باشیم برای ذخیره سازی بعضی از اخبار به عناوین مختلف مثل (مهم ، خوانده نشده،
 * یا دسته بندی های مورد نظرخود کاربر) که امکان دیدن و گروه بندی و ... رو به کاربر بده و کاربر بتونه
 * بعدا بدون جستجوی بسیار زیاد به خبر دسترسی پیدا کنه"
 */
@Composable
fun BookmarksScreen(
    viewModel: NewsViewModel,
    onStoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bookmarks by viewModel.allBookmarks.collectAsStateWithLifecycle()
    val allStories by viewModel.allStories.collectAsStateWithLifecycle()

    var selectedTag by remember { mutableStateOf("همه") }
    val tags = listOf("همه", "مهم", "خوانده نشده", "فوری", "سفارشی")

    val storyMap = allStories.associateBy { it.storyId }
    val filteredBookmarks = if (selectedTag == "همه") {
        bookmarks
    } else {
        bookmarks.filter { it.customTag == selectedTag }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "صندوق اخبار ذخیره‌شده و برچسب‌ها",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "دسترسی سریع و گروه‌بندی اخبار بر اساس برچسب‌های مهم، خوانده‌نشده و سفارشی",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tag Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tags) { tag ->
                    val isSelected = selectedTag == tag
                    val badgeColor = when (tag) {
                        "مهم" -> MediumPriorityAmber
                        "خوانده نشده" -> SoftCyan
                        "فوری" -> HighPriorityRed
                        else -> AccentCyan
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) badgeColor else CardBackground)
                            .clickable { selectedTag = tag }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("bookmark_tag_$tag")
                    ) {
                        Text(
                            text = tag,
                            color = if (isSelected) Color.Black else TextWhite,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredBookmarks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "خبری در این دسته‌بندی ذخیره نشده است.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredBookmarks, key = { it.bookmarkId }) { bm ->
                        val story = storyMap[bm.storyId]
                        if (story != null) {
                            Column {
                                // Tag Header Banner
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp, start = 4.dp, end = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Label,
                                            contentDescription = null,
                                            tint = AccentCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "برچسب: ${bm.customTag}",
                                            color = AccentCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (bm.note.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "(${bm.note})",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Text(
                                        text = "حذف از برچسب",
                                        color = HighPriorityRed,
                                        fontSize = 11.sp,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { viewModel.toggleBookmark(bm.storyId, bm.customTag) }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                StoryCard(
                                    story = story,
                                    bookmark = bm,
                                    onStoryClick = { onStoryClick(story.storyId) },
                                    onBookmarkClick = { viewModel.toggleBookmark(story.storyId, bm.customTag) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
