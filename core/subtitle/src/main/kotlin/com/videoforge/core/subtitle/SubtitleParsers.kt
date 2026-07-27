package com.videoforge.core.subtitle

object SrtParser {
    fun parse(content: String): List<SubtitleCue> {
        return parseCues(content, stripWebVttHeader = false)
    }
}

object VttParser {
    fun parse(content: String): List<SubtitleCue> {
        return parseCues(content, stripWebVttHeader = true)
    }
}

internal fun parseCues(
    content: String,
    stripWebVttHeader: Boolean
): List<SubtitleCue> {
    val cues = mutableListOf<SubtitleCue>()

    var normalized = content
        .replace("﻿", "")
        .replace("\r\n", "\n")
        .replace('\r', '\n')

    if (stripWebVttHeader) {
        val headerIndex = normalized.indexOf("WEBVTT")
        if (headerIndex >= 0) {
            normalized = normalized.substring(headerIndex + "WEBVTT".length)
        }
    }

    val lines = normalized.lines()
    val timeRegex = SubtitleTimeParser.CUE_TIME_LINE

    var i = 0
    while (i < lines.size) {
        val line = lines[i].trim()

        if (line.isEmpty()) {
            i++
            continue
        }

        val match = timeRegex.find(line)

        if (match != null) {
            val start = SubtitleTimeParser.parseTime(match.groupValues[1])
            val end = SubtitleTimeParser.parseTime(match.groupValues[2])

            if (start != null && end != null && end > start) {
                val textLines = mutableListOf<String>()
                i++

                while (i < lines.size) {
                    val textLine = lines[i]
                    if (textLine.trim().isEmpty()) break
                    if (textLine.contains("-->")) break
                    textLines.add(textLine.trimEnd())
                    i++
                }

                val text = textLines.joinToString("\n").trim()
                if (text.isNotEmpty()) {
                    cues.add(SubtitleCue(start, end, text))
                }

                continue
            }
        }

        i++
    }

    return cues
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
