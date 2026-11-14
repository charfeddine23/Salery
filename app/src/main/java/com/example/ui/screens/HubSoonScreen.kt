package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.LocalAdaptiveDimens
import com.example.ui.theme.LocalAdaptiveTypography

private data class RoadmapFeature(
    val icon: ImageVector,
    val accent: Color,
    val titleEn: String,
    val titleAr: String,
    val descEn: String,
    val descAr: String,
    val progress: Float,
    val badgeEn: String,
    val badgeAr: String,
    val opensVisaPreview: Boolean = false
)

@Composable
fun HubSoonScreen(
    onOpenVisaPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val dimens = LocalAdaptiveDimens.current
    val typo = LocalAdaptiveTypography.current

    val roadmapItems = listOf(
        RoadmapFeature(
            icon = Icons.Default.CreditCard,
            accent = CyanNeon,
            titleEn = "Visa International Card",
            titleAr = "بطاقة فيزا الدولية",
            descEn = "Multi-currency USD/EUR virtual & physical debit integration. Tap to preview.",
            descAr = "محافظ متعددة العملات (دولار/يورو) للدفع الدولي والاشتراكات. اضغط للمعاينة.",
            progress = 0.85f,
            badgeEn = "IN PROGRESS • 85%",
            badgeAr = "قيد التطوير • 85%",
            opensVisaPreview = true
        ),
        RoadmapFeature(
            icon = Icons.Default.Nfc,
            accent = EmeraldNeon,
            titleEn = "NFC Tap-to-Read Edahabia",
            titleAr = "قراءة البطاقة عبر NFC",
            descEn = "Instant card metadata & contactless balance reader using your phone NFC chip.",
            descAr = "قراءة بيانات البطاقة الذهبية تلقائياً بمجرد تقريبها من مستشعر NFC.",
            progress = 0.65f,
            badgeEn = "BETA • 65%",
            badgeAr = "تجريبي • 65%"
        ),
        RoadmapFeature(
            icon = Icons.Default.Sync,
            accent = Color(0xFF818CF8),
            titleEn = "BaridiMob SMS & Alert Sync",
            titleAr = "مزامنة إشعارات بريدي موب",
            descEn = "Automatically categorize incoming transfers and card payments into your ledger.",
            descAr = "تصنيف تلقائي للتحويلات ومصاريف البطاقة عند استلام إشعارات الدفع.",
            progress = 0.55f,
            badgeEn = "NEXT • 55%",
            badgeAr = "القادم • 55%"
        ),
        RoadmapFeature(
            icon = Icons.Default.DocumentScanner,
            accent = AmberNeon,
            titleEn = "Receipt & Invoice Scanner",
            titleAr = "ماسح الفواتير والإيصالات",
            descEn = "Scan postal or store receipts with camera to log expenses in one tap.",
            descAr = "تصوير فواتير المحلات أو البريد وتسجيل المصروف تلقائياً بضغطة واحدة.",
            progress = 0.40f,
            badgeEn = "PLANNED • 40%",
            badgeAr = "مخطط • 40%"
        ),
        RoadmapFeature(
            icon = Icons.Default.Group,
            accent = Color(0xFFF472B6),
            titleEn = "Family Shared Wallets",
            titleAr = "محافظ العائلة المشتركة",
            descEn = "Share household expense budgets and monthly goals cleanly across devices.",
            descAr = "مشاركة ميزانية المصاريف العائلية والأهداف المالية بأمان وخصوصية.",
            progress = 0.25f,
            badgeEn = "SOON • 25%",
            badgeAr = "قريباً • 25%"
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dimens.screenPaddingH),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
    ) {
        item {
            Spacer(modifier = Modifier.height(dimens.spacingSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Soon",
                        color = colors.textPrimary,
                        fontSize = typo.title,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isArabic) "المميزات والإضافات القادمة في SaleryPulse"
                        else "Upcoming capabilities & future modules in SaleryPulse",
                        color = colors.textMuted,
                        fontSize = typo.caption
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(CyanNeon.copy(alpha = 0.22f), Color(0xFF6366F1).copy(alpha = 0.22f))
                            )
                        )
                        .border(1.dp, CyanNeon.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isArabic) "قريباً" else "ROADMAP",
                            color = CyanNeon,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        items(roadmapItems) { item ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = colors.glassCard,
                borderColor = item.accent.copy(alpha = 0.35f),
                onClick = if (item.opensVisaPreview) onOpenVisaPreview else null
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(item.accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = item.accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isArabic) item.titleAr else item.titleEn,
                                    color = colors.textPrimary,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (item.opensVisaPreview) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = item.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isArabic) item.descAr else item.descEn,
                                color = colors.textMuted,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { item.progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(50)),
                            color = item.accent,
                            trackColor = colors.glassSurface
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isArabic) item.badgeAr else item.badgeEn,
                            color = item.accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}
