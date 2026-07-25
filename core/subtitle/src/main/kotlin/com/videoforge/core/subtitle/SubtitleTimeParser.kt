package com.videoforge.core.subtitle

object SubtitleTimeParser {

    fun parseTime(time: String): Long? {
        val normalized = time.trim().replace(',', '.')
        val parts = normalized.split(':')

        return when (parts.size) {
            3 -> {
                val hours = parts[0].toLongOrNull() ?: return null
                val minutes = parts[1].toLongOrNull() ?: return null
                val seconds = parts[2].toDoubleOrNull() ?: return null

                (hours * 3600 + minutes * 60) * 1000 + (seconds * 1000).toLong()
            }

            2 -> {
                val minutes = parts[0].toLongOrNull() ?: return null
                val seconds = parts[1].toDoubleOrNull() ?: return null

                minutes * 60 * 1000 + (seconds * 1000).toLong()
            }

            else -> null
        }
    }

    fun parseTimeLine(line: String): Pair<Long, Long>? {
        val parts = line.split("-->")

        if (parts.size != 2) return null

        val start = parseTime(parts[0]) ?: return null
        val end = parseTime(parts[1].substringBefore(' ').trim()) ?: return null

        if (end <= start) return null

        return start to end
    }
}