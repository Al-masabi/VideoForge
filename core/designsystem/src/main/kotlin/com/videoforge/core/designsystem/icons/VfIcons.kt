package com.videoforge.core.designsystem.icons

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Oval
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object VfIcons {

    val Film: ImageVector by lazy {
        ImageVector.Builder(
            name = "Film",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fillRule = PathFillType.EvenOdd) {
                addRoundRect(
                    RoundRect(2f, 3f, 22f, 21f, CornerRadius(2.5f, 2.5f))
                )

                listOf(5.4f, 10.8f, 16.2f).forEach { y ->
                    addRect(Rect(4.2f, y, 6.4f, y + 2.2f))
                    addRect(Rect(17.6f, y, 19.8f, y + 2.2f))
                }

                moveTo(10.4f, 9.4f)
                lineTo(15.4f, 12f)
                lineTo(10.4f, 14.6f)
                close()
            }
        }.build()
    }

    val Scissors: ImageVector by lazy {
        ImageVector.Builder(
            name = "Scissors",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fillRule = PathFillType.EvenOdd) {
                addOval(Oval(2.6f, 3.6f, 9.4f, 10.4f))
                addOval(Oval(4.4f, 5.4f, 7.6f, 8.6f))
                addOval(Oval(2.6f, 13.6f, 9.4f, 20.4f))
                addOval(Oval(4.4f, 15.4f, 7.6f, 18.6f))
            }
            path {
                moveTo(8.7f, 8.9f)
                lineTo(20.4f, 3.4f)
                lineTo(21.4f, 5.3f)
                lineTo(10.6f, 10.4f)
                close()
            }
            path {
                moveTo(8.7f, 15.1f)
                lineTo(20.4f, 20.6f)
                lineTo(21.4f, 18.7f)
                lineTo(10.6f, 13.6f)
                close()
            }
        }.build()
    }

    val Compress: ImageVector by lazy {
        ImageVector.Builder(
            name = "Compress",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fillRule = PathFillType.EvenOdd) {
                addRoundRect(
                    RoundRect(4.5f, 2f, 19.5f, 22f, CornerRadius(2.5f, 2.5f))
                )

                moveTo(10.9f, 5.8f)
                lineTo(13.1f, 5.8f)
                lineTo(13.1f, 11.4f)
                lineTo(15.9f, 11.4f)
                lineTo(12f, 16.2f)
                lineTo(8.1f, 11.4f)
                lineTo(10.9f, 11.4f)
                close()
            }
        }.build()
    }

    val Waveform: ImageVector by lazy {
        ImageVector.Builder(
            name = "Waveform",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path {
                addRoundRect(RoundRect(2.6f, 9.5f, 4.8f, 14.5f, CornerRadius(1.1f, 1.1f)))
                addRoundRect(RoundRect(7f, 6f, 9.2f, 18f, CornerRadius(1.1f, 1.1f)))
                addRoundRect(RoundRect(11.4f, 3f, 13.6f, 21f, CornerRadius(1.1f, 1.1f)))
                addRoundRect(RoundRect(15.8f, 7f, 18f, 17f, CornerRadius(1.1f, 1.1f)))
                addRoundRect(RoundRect(20.2f, 10f, 22.4f, 14f, CornerRadius(1.1f, 1.1f)))
            }
        }.build()
    }

    val Subtitle: ImageVector by lazy {
        ImageVector.Builder(
            name = "Subtitle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fillRule = PathFillType.EvenOdd) {
                addRoundRect(
                    RoundRect(2f, 4.5f, 22f, 19.5f, CornerRadius(2.5f, 2.5f))
                )
                addRect(Rect(5f, 10.4f, 15f, 12.4f))
                addRect(Rect(5f, 14f, 19f, 16f))
            }
        }.build()
    }

    val Bolt: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bolt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path {
                moveTo(13.2f, 2f)
                lineTo(4.6f, 13.6f)
                lineTo(10.4f, 13.6f)
                lineTo(8.8f, 22f)
                lineTo(19.4f, 9.4f)
                lineTo(12.6f, 9.4f)
                close()
            }
        }.build()
    }

    val Clock: ImageVector by lazy {
        ImageVector.Builder(
            name = "Clock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fillRule = PathFillType.EvenOdd) {
                addOval(Oval(2.5f, 2.5f, 21.5f, 21.5f))
                addOval(Oval(4.3f, 4.3f, 19.7f, 19.7f))
            }
            path {
                moveTo(11.1f, 6.6f)
                lineTo(12.9f, 6.6f)
                lineTo(12.9f, 12.4f)
                lineTo(16.6f, 14.6f)
                lineTo(15.7f, 16.2f)
                lineTo(11.1f, 13.4f)
                close()
            }
        }.build()
    }

    val Layers: ImageVector by lazy {
        ImageVector.Builder(
            name = "Layers",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path {
                moveTo(12f, 2.6f)
                lineTo(21f, 7.6f)
                lineTo(12f, 12.6f)
                lineTo(3f, 7.6f)
                close()
            }
            path {
                moveTo(3f, 11.4f)
                lineTo(12f, 16.4f)
                lineTo(21f, 11.4f)
                lineTo(21f, 14f)
                lineTo(12f, 19f)
                lineTo(3f, 14f)
                close()
            }
            path {
                moveTo(3f, 15.6f)
                lineTo(12f, 20.6f)
                lineTo(21f, 15.6f)
                lineTo(21f, 18.2f)
                lineTo(12f, 23.2f)
                lineTo(3f, 18.2f)
                close()
            }
        }.build()
    }
}