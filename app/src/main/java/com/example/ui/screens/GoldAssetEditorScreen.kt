package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.market.GoldAssetHolding
import com.example.market.SquareMarketSnapshot
import com.example.ui.components.AssetsGoldEntryPanel
import com.example.ui.components.edgeFadeVerticalContentPadding
import com.example.ui.i18n.LocalStealthMode
import com.example.ui.theme.LocalAdaptiveDimens

@Composable
fun GoldAssetEditorScreen(
    editingHolding: GoldAssetHolding?,
    squareSnapshot: SquareMarketSnapshot?,
    isRefreshingSquare: Boolean,
    isArabic: Boolean,
    onBackClick: () -> Unit,
    onRequestSyncSquare: () -> Unit,
    onSaveGold: (karat: Int, grams: Double, label: String, existingId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val isStealth = LocalStealthMode.current
    val dimens = LocalAdaptiveDimens.current

    LaunchedEffect(Unit) {
        if (squareSnapshot == null) {
            onRequestSyncSquare()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("gold_asset_editor_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(edgeFadeVerticalContentPadding(extraTop = dimens.spacingMd, extraBottom = dimens.spacingMd))
                .padding(horizontal = dimens.screenPaddingH),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
        ) {
            AssetsGoldEntryPanel(
                editingHolding = editingHolding,
                squareSnapshot = squareSnapshot,
                isRefreshingSquare = isRefreshingSquare,
                isArabic = isArabic,
                isStealth = isStealth,
                onRequestSyncSquare = onRequestSyncSquare,
                onSave = { karat, grams, label, existingId ->
                    onSaveGold(karat, grams, label, existingId)
                    onBackClick()
                },
                onCancel = onBackClick
            )
            Spacer(modifier = Modifier.height(dimens.spacingXl))
        }
    }
}
