package com.videoforge.android.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.videoforge.core.datastore.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutputFileProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    suspend fun createOutputFile(
        baseName: String,
        mimeType: String = "video/mp4"
    ): Uri? {
        val treeUri = userPreferencesRepository.outputTreeUri.first()

        if (treeUri != null) {
            val created = createInTree(
                treeUri = Uri.parse(treeUri),
                baseName = baseName,
                mimeType = mimeType
            )
            if (created != null) return created
        }

        return createInMediaStore(baseName, mimeType)
    }

    private fun createInTree(
        treeUri: Uri,
        baseName: String,
        mimeType: String
    ): Uri? {
        return try {
            DocumentsContract.createDocument(
                context.contentResolver,
                treeUri,
                mimeType,
                uniqueName(baseName)
            )
        } catch (exception: Exception) {
            null
        }
    }

    private fun createInMediaStore(
        baseName: String,
        mimeType: String
    ): Uri? {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, uniqueName(baseName))
                put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/VideoForge")
            }

            context.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                values
            )
        } catch (exception: Exception) {
            null
        }
    }

    private fun uniqueName(baseName: String): String {
        val cleanBase = baseName
            .substringBeforeLast('.', "")
            .ifEmpty { "video" }

        return "${cleanBase}_${System.currentTimeMillis()}.mp4"
    }
}