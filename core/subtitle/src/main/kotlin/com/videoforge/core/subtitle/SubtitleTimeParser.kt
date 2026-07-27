package com.videoforge.core.subtitle

object SubtitleTimeParser {

    private const val TIME_TOKEN =
        """\d{1,2}:\d{2}:\d{2}(?:[.,]\d{1,3})?|\d{1,2}:\d{2}(?:[.,]\d{1,3})?"""

    val CUE_TIME_LINE: Regex = Regex("""($TIME_TOKEN)\s*-->\s*($TIME_TOKEN)""")

    fun parseTime(time: String): Long? {
        val normalized = time.trim().replace(',', '.')
        if (normalized.isEmpty()) return null

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
        val match = CUE_TIME_LINE.find(line) ?: return null
        val start = parseTime(match.groupValues[1]) ?: return null
        val end = parseTime(match.groupValues[2]) ?: return null
        if (end <= start) return null
        return start to end
    }
}
