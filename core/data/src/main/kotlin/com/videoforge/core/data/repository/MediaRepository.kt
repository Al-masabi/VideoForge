package com.videoforge.core.data.repository

import android.net.Uri
import com.videoforge.core.data.model.MediaAsset
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun observeRecentAssets(): Flow<List<MediaAsset>>
    suspend fun importMedia(uri: Uri): Result<MediaAsset>
}