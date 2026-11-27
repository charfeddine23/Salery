package com.example

import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.VaultixTheme
import com.example.viewmodel.VaultixViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            val viewModel: VaultixViewModel = viewModel()
            val userSettings by viewModel.settings.collectAsState()

            val isDarkTheme = when (userSettings.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            VaultixTheme(
                darkTheme = isDarkTheme,
                designStyle = userSettings.designStyle
            ) {
                val activeGlassColors = LiquidGlassTheme.colors
                val useLightSystemBarIcons = activeGlassColors.bgStart.luminance() >= 0.5f
                val view = LocalView.current
                var lastAppliedLightIcons by remember { mutableStateOf<Boolean?>(null) }

                if (!view.isInEditMode && lastAppliedLightIcons != useLightSystemBarIcons) {
                    SideEffect {
                        lastAppliedLightIcons = useLightSystemBarIcons
                        WindowCompat.getInsetsController(window, view).apply {
                            isAppearanceLightStatusBars = useLightSystemBarIcons
                            isAppearanceLightNavigationBars = useLightSystemBarIcons
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    MainAppScreen(viewModel = viewModel)
                }
            }
        }
    }
}


