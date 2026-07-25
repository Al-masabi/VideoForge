package com.videoforge.core.subtitle

object SrtParser {

    fun parse(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val blocks = content.replace("\r\n", "\n").split(Regex("\n\\s*\n"))

        for (block in blocks) {
            val lines = block.lines().filter { it.isNotBlank() }
            if (lines.isEmpty()) continue

            val timeLineIndex = lines.indexOfFirst { it.contains("-->") }
            if (timeLineIndex == -1) continue

            val times = SubtitleTimeParser.parseTimeLine(lines[timeLineIndex]) ?: continue

            val text = lines.drop(timeLineIndex + 1).joinToString("\n").trim()

            if (text.isNotEmpty()) {
                cues.add(SubtitleCue(times.first, times.second, text))
            }
        }

        return cues
    }
}

object VttParser {

    fun parse(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()

        val cleaned = content
            .replace("\r\n", "\n")
            .removePrefix("WEBVTT")
            .substringAfter("\n")

        val blocks = cleaned.split(Regex("\n\\s*\n"))

        for (block in blocks) {
            val lines = block.lines().filter { it.isNotBlank() }
            if (lines.isEmpty()) continue

            val timeLineIndex = lines.indexOfFirst { it.contains("-->") }
            if (timeLineIndex == -1) continue

            val times = SubtitleTimeParser.parseTimeLine(lines[timeLineIndex]) ?: continue

            val text = lines.drop(timeLineIndex + 1).joinToString("\n").trim()

            if (text.isNotEmpty()) {
                cues.add(SubtitleCue(times.first, times.second, text))
            }
        }

        return cues
    }
}

object SubtitleParserRegistry {

    fun parse(
        fileName: String,
        mimeType: String?,
        content: String
    ): List<SubtitleCue> {
        val extension = fileName.substringAfterLast('.', "").lowercase()

        return when {
            extension == "vtt" || mimeType?.contains("vtt") == true -> {
                VttParser.parse(content)
            }

            else -> {
                SrtParser.parse(content)
            }
        }
    }
}