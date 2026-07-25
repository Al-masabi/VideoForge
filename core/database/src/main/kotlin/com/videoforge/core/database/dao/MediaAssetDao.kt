package com.videoforge.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.videoforge.core.database.entity.MediaAssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaAssetDao {

    @Query("SELECT * FROM media_assets ORDER BY accessedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<MediaAssetEntity>>

    @Query("SELECT * FROM media_assets WHERE uri = :uri LIMIT 1")
    suspend fun getByUri(uri: String): MediaAssetEntity?

    @Upsert
    suspend fun upsert(entity: MediaAssetEntity)
}