package com.videoforge.core.subtitle

object SubtitleSyncEngine {

    fun mapCuesToTimeline(
        cues: List<SubtitleCue>,
        clips: List<ClipSegment>
    ): List<SubtitleCue> {
        val result = mutableListOf<SubtitleCue>()

        for (cue in cues) {
            for (clip in clips) {
                val start = maxOf(cue.startMs, clip.start)
                val end = minOf(cue.endMs, clip.end)

                if (end > start) {
                    result.add(SubtitleCue(start, end, cue.text))
                }
            }
        }

        return result.sortedBy { it.startMs }
    }
}