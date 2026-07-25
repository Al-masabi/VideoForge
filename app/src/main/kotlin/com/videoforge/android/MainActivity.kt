package com.videoforge.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.videoforge.android.navigation.AppNavHost
import com.videoforge.android.ui.layout.AppLayout
import com.videoforge.android.ui.layout.LocalAppLayout
import com.videoforge.core.datastore.UserPreferencesRepository
import com.videoforge.core.designsystem.theme.VideoForgeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)

            val appLayout = remember(windowSizeClass) {
                AppLayout(
                    isExpandedScreen = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.EXPANDED
                )
            }

            val highContrast by userPreferencesRepository.highContrast
                .collectAsState(initial = false)

            CompositionLocalProvider(LocalAppLayout provides appLayout) {
                VideoForgeTheme(highContrast = highContrast) {
                    AppNavHost()
                }
            }
        }
    }
}