package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ManualNewsDialog
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.NewsViewModel

@Composable
fun MainScreen(viewModel: NewsViewModel) {
    val selectedStoryId by viewModel.selectedStoryId.collectAsStateWithLifecycle()
    val showManualAddDialog by viewModel.showManualAddDialog.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var currentTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (selectedStoryId != null) {
            StoryDetailScreen(
                storyId = selectedStoryId!!,
                viewModel = viewModel,
                onBack = { viewModel.selectStory(null) }
            )
        } else {
            Scaffold(
                containerColor = DeepNavy,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    NavigationBar(
                        containerColor = CardBackground,
                        contentColor = TextWhite,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("main_bottom_nav")
                    ) {
                        val navItems = listOf(
                            Triple("داستان‌های خبری", Icons.Default.Newspaper, "feed"),
                            Triple("منابع و فیدها", Icons.Default.RssFeed, "sources"),
                            Triple("اخبار ذخیره‌شده", Icons.Default.Bookmark, "bookmarks"),
                            Triple("لاگ‌ها و خطاها", Icons.Default.BugReport, "logs")
                        )

                        navItems.forEachIndexed { index, (title, icon, tag) ->
                            NavigationBarItem(
                                selected = currentTabIndex == index,
                                onClick = { currentTabIndex = index },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = title,
                                        fontSize = 11.sp,
                                        fontWeight = if (currentTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DeepNavy,
                                    selectedTextColor = AccentCyan,
                                    indicatorColor = AccentCyan,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_item_$tag")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTabIndex) {
                        0 -> FeedScreen(
                            viewModel = viewModel,
                            onStoryClick = { storyId -> viewModel.selectStory(storyId) }
                        )
                        1 -> SourcesScreen(viewModel = viewModel)
                        2 -> BookmarksScreen(
                            viewModel = viewModel,
                            onStoryClick = { storyId -> viewModel.selectStory(storyId) }
                        )
                        3 -> LogsScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // Manual Add News Dialog
        if (showManualAddDialog) {
            ManualNewsDialog(
                onDismiss = { viewModel.openManualAddDialog(false) },
                onSubmit = { title, content, sourceName, url, category ->
                    viewModel.addManualNews(title, content, sourceName, url, category)
                }
            )
        }
    }
}
