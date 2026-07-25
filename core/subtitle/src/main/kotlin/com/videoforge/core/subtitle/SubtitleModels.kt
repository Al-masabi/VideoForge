package com.videoforge.core.subtitle

data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

data class ClipSegment(
    val start: Long,
    val end: Long
)