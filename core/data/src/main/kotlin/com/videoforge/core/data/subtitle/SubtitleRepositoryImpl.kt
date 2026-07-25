package com.videoforge.core.data.subtitle

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.videoforge.core.database.dao.SubtitleCueDao
import com.videoforge.core.database.dao.SubtitleTrackDao
import com.videoforge.core.database.entity.SubtitleCueEntity
import com.videoforge.core.database.entity.SubtitleTrackEntity
import com.videoforge.core.subtitle.SubtitleCue
import com.videoforge.core.subtitle.SubtitleParserRegistry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitleRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subtitleTrackDao: SubtitleTrackDao,
    private val subtitleCueDao: SubtitleCueDao,
    private val subtitleCueTransformers: Set<@JvmSuppressWildcards SubtitleCueTransformer>
) : SubtitleRepository {

    override fun observeTracks(timelineId: String): Flow<List<SubtitleTrackInfo>> {
        return subtitleTrackDao.observeByTimeline(timelineId).map { entities ->
            entities.map { entity ->
                SubtitleTrackInfo(
                    id = entity.id,
                    timelineId = entity.timelineId,
                    uri = entity.uri,
                    displayName = entity.displayName,
                    language = entity.language,
                    mimeType = entity.mimeType,
                    charset = entity.charset,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    override fun observeCues(timelineId: String): Flow<List<SubtitleCue>> {
        return subtitleCueDao.observeByTimeline(timelineId).map { entities ->
            entities.map { entity ->
                SubtitleCue(
                    startMs = entity.startMs,
                    endMs = entity.endMs,
                    text = entity.text
                )
            }
        }
    }

    override suspend fun importSubtitle(
        timelineId: String,
        uri: Uri
    ): Result<SubtitleTrackInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val displayName = queryDisplayName(uri) ?: uri.lastPathSegment ?: "subtitle"

            val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes()
            } ?: throw IllegalStateException("Cannot read subtitle file")

            val (content, charset) = decodeBytes(bytes)

            val mimeType = context.contentResolver.getType(uri)

            val parsedCues = SubtitleParserRegistry.parse(displayName, mimeType, content)

            if (parsedCues.isEmpty()) {
                throw IllegalStateException("No cues found in subtitle file")
            }

            val transformedCues = subtitleCueTransformers.fold(parsedCues) { acc, transformer ->
                transformer.transform(acc)
            }

            val trackId = UUID.randomUUID().toString()

            subtitleTrackDao.upsert(
                SubtitleTrackEntity(
                    id = trackId,
                    timelineId = timelineId,
                    uri = uri.toString(),
                    displayName = displayName,
                    language = "",
                    mimeType = mimeType,
                    charset = charset.name(),
                    createdAt = System.currentTimeMillis()
                )
            )

            subtitleCueDao.deleteByTrack(trackId)

            subtitleCueDao.insertAll(
                transformedCues.mapIndexed { index, cue ->
                    SubtitleCueEntity(
                        id = UUID.randomUUID().toString(),
                        trackId = trackId,
                        startMs = cue.startMs,
                        endMs = cue.endMs,
                        text = cue.text,
                        ordinal = index
                    )
                }
            )

            SubtitleTrackInfo(
                id = trackId,
                timelineId = timelineId,
                uri = uri.toString(),
                displayName = displayName,
                language = "",
                mimeType = mimeType,
                charset = charset.name(),
                createdAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun deleteTrack(trackId: String) {
        subtitleCueDao.deleteByTrack(trackId)
        subtitleTrackDao.deleteTrack(trackId)
    }

    private fun queryDisplayName(uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) cursor.getString(index) else null
            } else {
                null
            }
        }
    }

    private fun decodeBytes(bytes: ByteArray): Pair<String, Charset> {
        if (
            bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) {
            return String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8) to StandardCharsets.UTF_8
        }

        if (isValidUtf8(bytes)) {
            return String(bytes, StandardCharsets.UTF_8) to StandardCharsets.UTF_8
        }

        val windows1256 = try {
            Charset.forName("windows-1256")
        } catch (exception: Exception) {
            StandardCharsets.ISO_8859_1
        }

        return String(bytes, windows1256) to windows1256
    }

    private fun isValidUtf8(bytes: ByteArray): Boolean {
        val decoder = StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)

        return try {
            decoder.decode(ByteBuffer.wrap(bytes))
            true
        } catch (exception: Exception) {
            false
        }
    }
}