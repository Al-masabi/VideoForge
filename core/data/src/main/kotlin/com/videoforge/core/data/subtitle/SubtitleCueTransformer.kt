package com.videoforge.core.data.subtitle

import com.videoforge.core.subtitle.SubtitleCue

interface SubtitleCueTransformer {
    fun transform(cues: List<SubtitleCue>): List<SubtitleCue>
}