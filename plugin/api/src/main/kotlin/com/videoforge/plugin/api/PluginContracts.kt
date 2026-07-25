package com.videoforge.plugin.api

import android.content.Context
import android.net.Uri

data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val type: PluginType
)

enum class PluginType {
    FILTER,
    SUBTITLE,
    EXPORT,
    ANALYSIS
}

interface VideoForgePlugin {
    val manifest: PluginManifest
}

data class PluginSubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

interface SubtitlePlugin : VideoForgePlugin {
    fun transformCues(cues: List<PluginSubtitleCue>): List<PluginSubtitleCue>
}

interface AnalysisPlugin : VideoForgePlugin {
    suspend fun analyze(context: Context, uri: Uri): String
}