package com.videoforge.android.ui.layout

import androidx.compose.runtime.staticCompositionLocalOf

data class AppLayout(
    val isExpandedScreen: Boolean
)

val LocalAppLayout = staticCompositionLocalOf {
    AppLayout(isExpandedScreen = false)
}