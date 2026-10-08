package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.EvidenceEntity
import com.example.data.local.FivePartAnalysis
import com.example.data.local.PublicOpinion
import com.example.data.local.RelatedNewsSummary
import com.example.data.local.StoryEntity
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.CardBackground
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HighPriorityRed
import com.example.ui.theme.LowPriorityGreen
import com.example.ui.theme.MediumPriorityAmber
import com.example.ui.theme.SoftCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.theme.VerifiedBadgeColor

/**
 * 5-Part Practical News Summary Component (خلاصه‌سازی کاربردی خبر):
 * 1. خلاصه متن خبر (چکیده یک پاراگرافی)
 * 2. پیامدها و تأثیرات (ارزیابی احتمالات و اثر بر حوزه‌ها)
 * 3. تصاویر مرتبط (حداکثر ۳ تصویر با عناوین معتبر)
 * 4. نظرات عمومی (دیدگاه‌های پربازدید و تحلیل احساسات)
 * 5. اخبار مرتبط (رویداد مشترک، کلمات کلیدی، نویسنده)
 * + بخش شواهد و حقایق استخراج‌شده (Cross-check Evidence)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FivePartAnalysisView(
    analysis: FivePartAnalysis,
    evidences: List<EvidenceEntity>,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .testTag("five_part_analysis_view"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "خلاصه‌سازی کاربردی خبر (تحلیل چندلایه)",
                    color = AccentCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 1. خلاصه متن خبر
            SectionCard(
                title = "۱. خلاصه متن خبر",
                icon = Icons.Default.Info,
                accentColor = AccentCyan
            ) {
                Text(
                    text = analysis.summary,
                    color = TextWhite,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.testTag("analysis_part_summary")
                )
            }

            // 2. پیامدها و تأثیرات
            SectionCard(
                title = "۲. پیامدها و تأثیرات محتمل",
                icon = Icons.Default.Warning,
                accentColor = MediumPriorityAmber
            ) {
                Text(
                    text = analysis.impact,
                    color = TextWhite,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.testTag("analysis_part_impact")
                )
            }

            // 3. تصاویر مرتبط (حداکثر ۳ تصویر)
            SectionCard(
                title = "۳. تصاویر مرتبط (ارتباط چندرسانه‌ای)",
                icon = Icons.Default.Image,
                accentColor = SoftCyan
            ) {
                Column(modifier = Modifier.testTag("analysis_part_images")) {
                    Text(
                        text = "تصاویر منتخب مرتبط با سوژه، رویداد و زمینه‌های خبری (حداکثر ۳ تصویر):",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(analysis.images) { imgUrl ->
                            Box(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.3f))
                            ) {
                                AsyncImage(
                                    model = imgUrl,
                                    contentDescription = "تصویر خبر",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                            }
                        }
                    }
                }
            }

            // 4. نظرات عمومی و دیدگاه کاربران
            SectionCard(
                title = "۴. نظرات عمومی و بازتاب در شبکه‌های اجتماعی",
                icon = Icons.Default.ChatBubbleOutline,
                accentColor = ElectricBlue
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("analysis_part_opinions")
                ) {
                    analysis.publicOpinions.forEach { opinion ->
                        PublicOpinionRow(opinion = opinion)
                    }
                }
            }

            // 5. اخبار مرتبط
            SectionCard(
                title = "۵. اخبار مرتبط و سیر وقایع موازی",
                icon = Icons.Default.Link,
                accentColor = VerifiedBadgeColor
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("analysis_part_related")
                ) {
                    analysis.relatedNews.forEach { related ->
                        RelatedNewsRow(item = related)
                    }
                }
            }

            // جدول شواهد و حقایق استخراج شده (Evidence & Cross-check)
            if (evidences.isNotEmpty()) {
                SectionCard(
                    title = "شواهد و حقایق استخراج‌شده (Cross-Check & Entities)",
                    icon = Icons.Default.CheckCircle,
                    accentColor = LowPriorityGreen
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("evidences_section")
                    ) {
                        Text(
                            text = "موجودیت‌های استخراج‌شده برای تطبیق میان‌رسانه‌ای و اعتبارسنجی ادعاها:",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            evidences.forEach { ev ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "[${ev.evidenceType}]",
                                            color = AccentCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = ev.entityOrClaim,
                                            color = TextWhite,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(${ev.source})",
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
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
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun PublicOpinionRow(opinion: PublicOpinion) {
    val sentimentColor = when (opinion.sentiment) {
        "POSITIVE" -> LowPriorityGreen
        "CRITICAL" -> HighPriorityRed
        else -> MediumPriorityAmber
    }

    val sentimentLabel = when (opinion.sentiment) {
        "POSITIVE" -> "مثبت / موافق"
        "CRITICAL" -> "انتقادی / مخالف"
        else -> "خنثی / میانه"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = opinion.author,
                        color = AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${opinion.sourcePlatform}",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(sentimentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = sentimentLabel,
                        color = sentimentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "«${opinion.text}»",
                color = TextWhite,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = opinion.engagement,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun RelatedNewsRow(item: RelatedNewsSummary) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.title,
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.source} • ${item.timeAgo}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SoftCyan.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.reason,
                        color = SoftCyan,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
