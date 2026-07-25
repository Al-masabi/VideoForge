package com.videoforge.android.plugin

import com.videoforge.core.data.subtitle.SubtitleCueTransformer
import com.videoforge.core.subtitle.SubtitleCue
import com.videoforge.plugin.api.PluginSubtitleCue
import javax.inject.Inject

class PluginSubtitleTransformer @Inject constructor(
    private val pluginRegistry: PluginRegistry
) : SubtitleCueTransformer {

    override fun transform(cues: List<SubtitleCue>): List<SubtitleCue> {
        val subtitlePlugins = pluginRegistry.subtitlePlugins()

        if (subtitlePlugins.isEmpty()) return cues

        var pluginCues = cues.map { cue ->
            PluginSubtitleCue(
                startMs = cue.startMs,
                endMs = cue.endMs,
                text = cue.text
            )
        }

        subtitlePlugins.forEach { plugin ->
            pluginCues = plugin.transformCues(pluginCues)
        }

        return pluginCues.map { cue ->
            SubtitleCue(
                startMs = cue.startMs,
                endMs = cue.endMs,
                text = cue.text
            )
        }
    }
}