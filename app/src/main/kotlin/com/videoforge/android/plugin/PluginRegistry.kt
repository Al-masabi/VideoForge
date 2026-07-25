package com.videoforge.android.plugin

import android.content.Context
import android.net.Uri
import com.videoforge.android.plugin.builtin.SubtitleShiftPlugin
import com.videoforge.android.plugin.builtin.VideoInfoReportPlugin
import com.videoforge.core.datastore.UserPreferencesRepository
import com.videoforge.plugin.api.AnalysisPlugin
import com.videoforge.plugin.api.PluginType
import com.videoforge.plugin.api.SubtitlePlugin
import com.videoforge.plugin.api.VideoForgePlugin
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class PluginInfo(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val type: PluginType
)

@Singleton
class PluginRegistry @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    private val plugins: List<VideoForgePlugin> = listOf(
        SubtitleShiftPlugin(),
        VideoInfoReportPlugin()
    )

    private val pluginsById: Map<String, VideoForgePlugin> = plugins.associateBy {
        it.manifest.id
    }

    val enabledIdsFlow: Flow<Set<String>> = userPreferencesRepository.enabledPluginIds

    fun infos(): List<PluginInfo> {
        return plugins.map { plugin ->
            PluginInfo(
                id = plugin.manifest.id,
                name = plugin.manifest.name,
                version = plugin.manifest.version,
                description = plugin.manifest.description,
                type = plugin.manifest.type
            )
        }
    }

    suspend fun setEnabled(id: String, enabled: Boolean) {
        val current = userPreferencesRepository.enabledPluginIds.first()

        val updated = if (enabled) {
            current + id
        } else {
            current - id
        }

        userPreferencesRepository.setEnabledPluginIds(updated)
    }

    fun subtitlePlugins(): List<SubtitlePlugin> {
        return plugins.filterIsInstance<SubtitlePlugin>()
    }

    suspend fun runAnalysis(
        pluginId: String,
        context: Context,
        uri: Uri
    ): String? {
        val plugin = pluginsById[pluginId] as? AnalysisPlugin ?: return null

        return plugin.analyze(context, uri)
    }
}