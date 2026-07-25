package com.videoforge.android.plugin.builtin

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.videoforge.plugin.api.AnalysisPlugin
import com.videoforge.plugin.api.PluginManifest
import com.videoforge.plugin.api.PluginType

class VideoInfoReportPlugin : AnalysisPlugin {

    override val manifest = PluginManifest(
        id = ID,
        name = "فاحص الوسائط",
        version = "1.0.0",
        description = "توليد تقرير كامل عن خصائص ملف الفيديو",
        type = PluginType.ANALYSIS
    )

    override suspend fun analyze(context: Context, uri: Uri): String {
        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(context, uri)

            buildString {
                appendLine("الملف: ${uri.lastPathSegment ?: "-"}")
                appendLine("المدة: ${retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: "-"} ms")
                appendLine("العرض: ${retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) ?: "-"}")
                appendLine("الارتفاع: ${retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT) ?: "-"}")
                appendLine("الدوران: ${retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION) ?: "-"}")
                appendLine("معدل البت: ${retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE) ?: "-"}")
                appendLine("صوت: ${retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) ?: "-"}")
                appendLine("فيديو: ${retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) ?: "-"}")
                appendLine("MIME: ${context.contentResolver.getType(uri) ?: "-"}")
            }
        } catch (exception: Exception) {
            "تعذر تحليل الملف: ${exception.message}"
        } finally {
            retriever.release()
        }
    }

    companion object {
        const val ID = "builtin.analysis.info"
    }
}