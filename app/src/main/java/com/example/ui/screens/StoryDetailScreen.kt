package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.local.StoryEntity
import com.example.engine.NewsEngine
import com.example.ui.components.FivePartAnalysisView
import com.example.ui.components.MentionItemCard
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryDetailScreen(
    storyId: String,
    viewModel: NewsViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val allStories by viewModel.allStories.collectAsStateWithLifecycle()
    val story = allStories.firstOrNull { it.storyId == storyId }

    val mentions by viewModel.getStoryMentions(storyId).collectAsStateWithLifecycle(initialValue = emptyList())
    val evidences by viewModel.getStoryEvidences(storyId).collectAsStateWithLifecycle(initialValue = emptyList())
    val bookmark by viewModel.getStoryBookmark(storyId).collectAsStateWithLifecycle(initialValue = null)

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("خلاصه‌سازی کاربردی ۵ بخشی", "خط زمانی اظهارها (Timeline)", "مستندات و شواهد")

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "تحلیل رویداد و داستان خبری",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                                tint = AccentCyan
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                viewModel.toggleBookmark(
                                    storyId = storyId,
                                    currentTag = bookmark?.customTag
                                )
                            },
                            modifier = Modifier.testTag("detail_bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (bookmark != null) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "نشان کردن",
                                tint = if (bookmark != null) AccentCyan else TextMuted
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            if (story == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentCyan)
                }
            } else {
                val priorityColor = when {
                    story.priorityScore >= NewsEngine.THRESHOLD_HIGH -> HighPriorityRed
                    story.priorityScore >= NewsEngine.THRESHOLD_MEDIUM -> MediumPriorityAmber
                    else -> LowPriorityGreen
                }

                // 5-Part Analysis prepared dynamically via NewsEngine
                val fivePart = remember(story.storyId, mentions.size) {
                    val combinedText = mentions.joinToString(" ") { it.content }.ifBlank { story.summary }
                    NewsEngine.generateFivePartAnalysis(
                        title = story.title,
                        content = combinedText,
                        category = story.category,
                        existingImageUrl = story.primaryImageUrl
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Story Header Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(priorityColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = NewsEngine.getPriorityLevelLabel(story.priorityScore),
                                        color = priorityColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = SoftCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${mentions.size} منبع پوشش‌دهنده",
                                        color = SoftCyan,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = story.title,
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 24.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Score Matrix: Importance vs Confidence vs Priority
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ScoreChip(
                                    label = "اهمیت رویداد",
                                    score = story.importanceScore,
                                    color = MediumPriorityAmber
                                )
                                ScoreChip(
                                    label = "اطمینان و صحت",
                                    score = story.confidenceScore,
                                    color = LowPriorityGreen
                                )
                                ScoreChip(
                                    label = "اولویت نهایی پیگیری",
                                    score = story.priorityScore,
                                    color = AccentCyan
                                )
                            }
                        }
                    }

                    // Navigation Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = CardBackground,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = AccentCyan
                            )
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        color = if (selectedTab == index) AccentCyan else TextMuted,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.testTag("detail_tab_$index")
                            )
                        }
                    }

                    // Tab Content
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (selectedTab) {
                            0 -> {
                                item {
                                    FivePartAnalysisView(
                                        analysis = fivePart,
                                        evidences = evidences
                                    )
                                }
                            }
                            1 -> {
                                // Mentions Timeline (Item 2: تولید Timeline داستان)
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = AccentCyan
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "خط زمانی انتشار اخبار مرتبط (Story Timeline)",
                                            color = AccentCyan,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                items(mentions, key = { it.mentionId }) { mention ->
                                    MentionItemCard(
                                        mention = mention,
                                        onDeleteClick = { viewModel.deleteMention(mention.mentionId) }
                                    )
                                }
                            }
                            2 -> {
                                // Evidences list
                                item {
                                    Text(
                                        text = "فهرست حقایق استخراج‌شده و اسناد Cross-Check:",
                                        color = AccentCyan,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                items(evidences, key = { it.evidenceId }) { ev ->
                                    EvidenceDetailCard(evidence = ev)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreChip(label: String, score: Double, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, color = TextMuted, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${(score * 100).toInt()}%",
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EvidenceDetailCard(evidence: com.example.data.local.EvidenceEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = evidence.evidenceType,
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = evidence.entityOrClaim,
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "منبع ادعا: ${evidence.source} • میزان اعتماد: ${(evidence.confidence * 100).toInt()}%",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (evidence.verificationStatus == "VERIFIED") LowPriorityGreen.copy(alpha = 0.2f)
                        else MediumPriorityAmber.copy(alpha = 0.2f)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (evidence.verificationStatus == "VERIFIED") "تأییدشده" else "گزارش‌شده",
                    color = if (evidence.verificationStatus == "VERIFIED") LowPriorityGreen else MediumPriorityAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
