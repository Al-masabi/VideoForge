package com.videoforge.android.plugin.builtin

import com.videoforge.plugin.api.PluginManifest
import com.videoforge.plugin.api.PluginSubtitleCue
import com.videoforge.plugin.api.PluginType
import com.videoforge.plugin.api.SubtitlePlugin

class SubtitleShiftPlugin : SubtitlePlugin {

    override val manifest = PluginManifest(
        id = ID,
        name = "إزاحة الترجمة",
        version = "1.0.0",
        description = "إزاحة كل أسطر الترجمة بمقدار 500ms للأمام",
        type = PluginType.SUBTITLE
    )

    override fun transformCues(cues: List<PluginSubtitleCue>): List<PluginSubtitleCue> {
        return cues.map { cue ->
            cue.copy(
                startMs = cue.startMs + SHIFT_MS,
                endMs = cue.endMs + SHIFT_MS
            )
        }
    }

    companion object {
        const val ID = "builtin.subtitle.shift"
        const val SHIFT_MS = 500L
    }
}