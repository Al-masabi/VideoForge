package com.videoforge.core.data.repository

import com.videoforge.core.data.model.MediaAsset

interface MediaStoreRepository {
    suspend fun loadExternalVideos(): List<MediaAsset>
}