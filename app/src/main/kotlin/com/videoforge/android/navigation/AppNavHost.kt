package com.videoforge.android.navigation

import android.net.Uri
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.videoforge.android.ui.batch.BatchScreen
import com.videoforge.android.ui.compress.CompressScreen
import com.videoforge.android.ui.diagnostics.DiagnosticsScreen
import com.videoforge.android.ui.editor.EditorScreen
import com.videoforge.android.ui.filepicker.FilePickerScreen
import com.videoforge.android.ui.home.HomeScreen
import com.videoforge.android.ui.logs.LogsScreen
import com.videoforge.android.ui.player.PlayerScreen
import com.videoforge.android.ui.plugins.PluginsScreen
import com.videoforge.android.ui.settings.SettingsScreen
import com.videoforge.android.ui.shared.LocalAnimatedVisibilityScope
import com.videoforge.android.ui.shared.LocalSharedTransitionScope

object AppRoutes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val LOGS = "logs"
    const val FILE_PICKER = "file_picker"
    const val PLAYER = "player/{uri}"
    const val EDITOR = "editor/{uri}"
    const val COMPRESS = "compress"
    const val BATCH = "batch"
    const val DIAGNOSTICS = "diagnostics"
    const val PLUGINS = "plugins"

    fun playerRoute(uri: String): String {
        return "player/${Uri.encode(uri)}"
    }

    fun editorRoute(uri: String): String {
        return "editor/${Uri.encode(uri)}"
    }
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController = navController,
                startDestination = AppRoutes.HOME,
                enterTransition = {
                    fadeIn(tween(240)) + slideInHorizontally(tween(280)) { it / 14 }
                },
                exitTransition = {
                    fadeOut(tween(160))
                },
                popEnterTransition = {
                    fadeIn(tween(240))
                },
                popExitTransition = {
                    fadeOut(tween(180)) + slideOutHorizontally(tween(240)) { it / 14 }
                }
            ) {
                composable(AppRoutes.HOME) {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        HomeScreen(
                            onNavigateToProjects = { navController.navigate(AppRoutes.FILE_PICKER) },
                            onNavigateToFilePicker = { navController.navigate(AppRoutes.FILE_PICKER) },
                            onNavigateToCompress = { navController.navigate(AppRoutes.COMPRESS) },
                            onNavigateToBatch = { navController.navigate(AppRoutes.BATCH) },
                            onNavigateToSettings = { navController.navigate(AppRoutes.SETTINGS) },
                            onNavigateToLogs = { navController.navigate(AppRoutes.LOGS) },
                            onOpenVideo = { uri -> navController.navigate(AppRoutes.playerRoute(uri)) }
                        )
                    }
                }

                composable(AppRoutes.SETTINGS) {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToDiagnostics = { navController.navigate(AppRoutes.DIAGNOSTICS) },
                            onNavigateToPlugins = { navController.navigate(AppRoutes.PLUGINS) }
                        )
                    }
                }

                composable(AppRoutes.LOGS) {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        LogsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable(AppRoutes.FILE_PICKER) {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        FilePickerScreen(
                            onBack = { navController.popBackStack() },
                            onOpenVideo = { uri -> navController.navigate(AppRoutes.playerRoute(uri)) }
                        )
                    }
                }

                composable(
                    route = AppRoutes.PLAYER,
                    arguments = listOf(
                        navArgument("uri") { type = NavType.StringType }
                    )
                ) {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        PlayerScreen(
                            onBack = { navController.popBackStack() },
                            onOpenEditor = { uri -> navController.navigate(AppRoutes.editorRoute(uri)) }
                        )
                    }
                }

                composable(
                    route = AppRoutes.EDITOR,
                    arguments = listOf(
                        navArgument("uri") { type = NavType.StringType }
                    )
                ) {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        EditorScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable(AppRoutes.COMPRESS) {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        CompressScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable(AppRoutes.BATCH) {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        BatchScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable(AppRoutes.DIAGNOSTICS) {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        DiagnosticsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable(AppRoutes.PLUGINS) {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        PluginsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}