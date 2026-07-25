package com.videoforge.core.data.repository

import android.net.Uri
import com.videoforge.core.data.model.MediaAsset
import com.videoforge.core.database.dao.MediaAssetDao
import com.videoforge.core.database.entity.MediaAssetEntity
import com.videoforge.core.media.MediaMetadataExtractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val mediaAssetDao: MediaAssetDao,
    private val mediaMetadataExtractor: MediaMetadataExtractor
) : MediaRepository {

    override fun observeRecentAssets(): Flow<List<MediaAsset>> {
        return mediaAssetDao.observeRecent(RECENT_LIMIT).map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }

    override suspend fun importMedia(uri: Uri): Result<MediaAsset> {
        return mediaMetadataExtractor.extract(uri).map { metadata ->
            val entity = MediaAssetEntity(
                uri = uri.toString(),
                displayName = metadata.displayName,
                mimeType = metadata.mimeType,
                sizeBytes = metadata.sizeBytes,
                durationMs = metadata.durationMs,
                width = metadata.width,
                height = metadata.height,
                rotation = metadata.rotation,
                hasAudio = metadata.hasAudio,
                hasVideo = metadata.hasVideo,
                accessedAt = System.currentTimeMillis()
            )

            mediaAssetDao.upsert(entity)

            entity.toDomain()
        }
    }

    private fun MediaAssetEntity.toDomain(): MediaAsset {
        return MediaAsset(
            uri = uri,
            displayName = displayName,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            durationMs = durationMs,
            width = width,
            height = height,
            rotation = rotation,
            hasAudio = hasAudio,
            hasVideo = hasVideo,
            accessedAt = accessedAt
        )
    }

    companion object {
        private const val RECENT_LIMIT = 50
    }
}