package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.GlassBackground
import com.example.ui.components.PatternLockView
import com.example.ui.components.PatternState
import com.example.ui.i18n.APP_NAME
import com.example.ui.i18n.APP_VERSION
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.DEVELOPER_NAME
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.RoseNeon

@Composable
fun LockScreen(
    onUnlockAttempt: (List<Int>) -> Boolean,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors

    var patternState by remember { mutableStateOf(PatternState.INPUT) }
    var statusText by remember { mutableStateOf(strings.drawPattern) }

    GlassBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Official Salery Brand Icon
            Image(
                painter = painterResource(id = R.drawable.salery_brand_icon),
                contentDescription = APP_NAME,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App Name (always Latin)
            Text(
                text = APP_NAME,
                color = colors.textPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Text(
                text = "Liquid Glass Security",
                color = colors.primaryNeon,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Instruction / Error Text
            Text(
                text = statusText,
                color = when (patternState) {
                    PatternState.INPUT -> colors.textSecondary
                    PatternState.SUCCESS -> EmeraldNeon
                    PatternState.ERROR -> RoseNeon
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 3x3 Pattern Gesture Lock
            PatternLockView(
                modifier = Modifier.size(290.dp),
                patternState = patternState,
                onPatternComplete = { pattern ->
                    val success = onUnlockAttempt(pattern)
                    if (success) {
                        patternState = PatternState.SUCCESS
                        statusText = "Unlocked"
                    } else {
                        patternState = PatternState.ERROR
                        statusText = strings.incorrectPattern
                    }
                }
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Developer Credit Watermark
            Text(
                text = "Developer: $DEVELOPER_NAME",
                color = colors.textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Version $APP_VERSION",
                color = colors.textMuted.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
    }
}
