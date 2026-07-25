package com.videoforge.android.util

fun Long.formatDuration(): String {
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

fun Long.formatTotalDuration(): String {
    val totalMinutes = this / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return if (hours > 0) {
        "${hours}س ${minutes}د"
    } else {
        "${minutes}د"
    }
}

fun Long.formatTimecode(): String {
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = this % 1000

    return "%02d:%02d.%03d".format(minutes, seconds, millis)
}

fun formatResolution(width: Int, height: Int): String? {
    return if (width > 0 && height > 0) {
        "${width}x${height}"
    } else {
        null
    }
}

fun Long.formatFileSize(): String {
    if (this <= 0L) return "0 B"

    val units = arrayOf("B", "KB", "MB", "GB", "TB")

    var size = this.toDouble()
    var unitIndex = 0

    while (size >= 1024.0 && unitIndex < units.lastIndex) {
        size /= 1024.0
        unitIndex++
    }

    return if (unitIndex == 0) {
        "${size.toLong()} ${units[unitIndex]}"
    } else {
        String.format("%.1f %s", size, units[unitIndex])
    }
}