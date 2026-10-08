package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.data.local.SourceEntity
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.CardBackground
import com.example.ui.theme.HighPriorityRed
import com.example.ui.theme.LowPriorityGreen
import com.example.ui.theme.SoftCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.NewsViewModel

@Composable
fun SourcesScreen(
    viewModel: NewsViewModel,
    modifier: Modifier = Modifier
) {
    val sources by viewModel.allSources.collectAsStateWithLifecycle()
    val sourceToDelete by viewModel.sourceToDelete.collectAsStateWithLifecycle()

    var showAddSourceDialog by remember { mutableStateOf(false) }
    var deleteAssociatedMentions by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "مدیریت منابع خبری و فیدها",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "کنترل وضعیت فعال/غیرفعال و حذف هوشمند با تصمیم‌گیری درباره اخبار وابسته",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sources List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sources, key = { it.sourceId }) { source ->
                        SourceItemCard(
                            source = source,
                            onToggleStatus = { viewModel.toggleSourceStatus(source) },
                            onDeleteClick = { viewModel.confirmDeleteSource(source) }
                        )
                    }
                }
            }

            // FAB for adding new source
            FloatingActionButton(
                onClick = { showAddSourceDialog = true },
                containerColor = AccentCyan,
                contentColor = Color.Black,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
                    .testTag("fab_add_source")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "افزودن منبع")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("منبع جدید", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Delete Confirmation Dialog with Choice (Item 3 & 4)
            if (sourceToDelete != null) {
                val target = sourceToDelete!!
                AlertDialog(
                    onDismissRequest = { viewModel.confirmDeleteSource(null) },
                    containerColor = CardBackground,
                    title = {
                        Text(
                            text = "تأیید حذف منبع «${target.name}»",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "آیا از حذف این منبع خبری مطمئن هستید؟",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(8.dp)
                            ) {
                                Checkbox(
                                    checked = deleteAssociatedMentions,
                                    onCheckedChange = { deleteAssociatedMentions = it },
                                    colors = CheckboxDefaults.colors(checkedColor = HighPriorityRed)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "حذف تمام اخبار ثبت‌شده وابسته به این منبع (Mentions)",
                                    color = if (deleteAssociatedMentions) HighPriorityRed else TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = if (deleteAssociatedMentions) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.executeDeleteSource(target.sourceId, deleteAssociatedMentions)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HighPriorityRed),
                            modifier = Modifier.testTag("confirm_delete_source_btn")
                        ) {
                            Text("حذف نهایی", color = Color.White)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { viewModel.confirmDeleteSource(null) }
                        ) {
                            Text("انصراف", color = TextWhite)
                        }
                    }
                )
            }

            // Add Source Dialog
            if (showAddSourceDialog) {
                AddSourceDialog(
                    onDismiss = { showAddSourceDialog = false },
                    onConfirm = { name, feedUrl, category ->
                        viewModel.addNewSource(name, feedUrl, category)
                        showAddSourceDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SourceItemCard(
    source: SourceEntity,
    onToggleStatus: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("source_item_${source.sourceId}"),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = source.name,
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (source.isActive) LowPriorityGreen.copy(alpha = 0.15f)
                                else HighPriorityRed.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (source.isActive) "فعال" else "غیرفعال",
                            color = if (source.isActive) LowPriorityGreen else HighPriorityRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = source.feedUrl,
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "دسته‌بندی: ${source.category}",
                        color = SoftCyan,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "اعتماد: ${(source.trustScore * 100).toInt()}%",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Switch Active / Inactive
                Switch(
                    checked = source.isActive,
                    onCheckedChange = { onToggleStatus() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentCyan,
                        checkedTrackColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.testTag("toggle_source_${source.sourceId}")
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Delete Source Button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.testTag("delete_source_btn_${source.sourceId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف منبع",
                        tint = HighPriorityRed
                    )
                }
            }
        }
    }
}

@Composable
private fun AddSourceDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, feedUrl: String, category: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("عمومی") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text("افزودن منبع خبری جدید", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام منبع (مانند ایرنا یا رویترز)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("آدرس فید یا سایت (URL)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("دسته‌بندی موضوعی") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, url, category)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
            ) {
                Text("ثبت منبع", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("انصراف", color = TextWhite)
            }
        }
    )
}
