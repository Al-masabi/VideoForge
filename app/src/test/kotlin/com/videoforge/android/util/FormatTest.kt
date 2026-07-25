package com.videoforge.android.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FormatTest {

    @Test
    fun `formatDuration formats minutes and seconds`() {
        assertThat(65_000L.formatDuration()).isEqualTo("01:05")
    }

    @Test
    fun `formatDuration formats hours`() {
        assertThat(3_661_000L.formatDuration()).isEqualTo("1:01:01")
    }

    @Test
    fun `formatTotalDuration formats minutes only`() {
        assertThat(90_000L.formatTotalDuration()).isEqualTo("1د")
    }

    @Test
    fun `formatTotalDuration formats hours and minutes`() {
        assertThat(5_400_000L.formatTotalDuration()).isEqualTo("1س 30د")
    }

    @Test
    fun `formatFileSize formats bytes`() {
        assertThat(0L.formatFileSize()).isEqualTo("0 B")
        assertThat(1023L.formatFileSize()).isEqualTo("1023 B")
        assertThat(1024L.formatFileSize()).isEqualTo("1.0 KB")
        assertThat(1_572_864L.formatFileSize()).isEqualTo("1.5 MB")
    }

    @Test
    fun `formatResolution returns null for invalid dimensions`() {
        assertThat(formatResolution(0, 1080)).isNull()
        assertThat(formatResolution(1920, 1080)).isEqualTo("1920x1080")
    }
}