package com.videoforge.core.data.editor

import android.net.Uri
import com.videoforge.core.database.dao.ClipDao
import com.videoforge.core.database.dao.HistoryDao
import com.videoforge.core.database.dao.MarkerDao
import com.videoforge.core.database.dao.MediaAssetDao
import com.videoforge.core.database.dao.TimelineDao
import com.videoforge.core.database.entity.ClipEntity
import com.videoforge.core.database.entity.EditHistoryEntity
import com.videoforge.core.database.entity.MarkerEntity
import com.videoforge.core.database.entity.TimelineEntity
import com.videoforge.core.media.MediaMetadataExtractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EditorRepositoryImpl @Inject constructor(
    private val timelineDao: TimelineDao,
    private val clipDao: ClipDao,
    private val markerDao: MarkerDao,
    private val historyDao: HistoryDao,
    private val mediaAssetDao: MediaAssetDao,
    private val mediaMetadataExtractor: MediaMetadataExtractor
) : EditorRepository {

    override suspend fun getOrCreateTimeline(assetUri: String): String {
        timelineDao.getByAssetUri(assetUri)?.let { existing ->
            return existing.id
        }

        val durationMs = mediaAssetDao.getByUri(assetUri)
            ?.durationMs
            ?.takeIf { it > 0L }
            ?: mediaMetadataExtractor.extract(Uri.parse(assetUri))
                .getOrNull()
                ?.durationMs
                ?.takeIf { it > 0L }
            ?: 1L

        val now = System.currentTimeMillis()
        val timelineId = UUID.randomUUID().toString()
        val clipId = UUID.randomUUID().toString()

        val timeline = TimelineEntity(
            id = timelineId,
            assetUri = assetUri,
            name = Uri.parse(assetUri).lastPathSegment.orEmpty(),
            createdAt = now,
            updatedAt = now,
            historyIndex = 0
        )

        val clip = ClipEntity(
            id = clipId,
            timelineId = timelineId,
            assetUri = assetUri,
            sourceInMs = 0L,
            sourceOutMs = durationMs,
            ordinal = 0
        )

        timelineDao.upsert(timeline)

        markerDao.deleteByTimeline(timelineId)
        clipDao.deleteByTimeline(timelineId)
        clipDao.insertAll(listOf(clip))

        historyDao.deleteByTimeline(timelineId)
        historyDao.insert(
            EditHistoryEntity(
                id = UUID.randomUUID().toString(),
                timelineId = timelineId,
                sequence = 0,
                clipsJson = clipsToJson(listOf(clip)),
                markersJson = markersToJson(emptyList()),
                createdAt = now
            )
        )

        return timelineId
    }

    override fun observeEditorState(timelineId: String): Flow<EditorState> {
        return combine(
            timelineDao.observeById(timelineId),
            clipDao.observeByTimeline(timelineId),
            markerDao.observeByTimeline(timelineId),
            historyDao.observeCount(timelineId)
        ) { timeline, clips, markers, historyCount ->
            EditorState(
                timeline = timeline?.let {
                    TimelineInfo(
                        id = it.id,
                        assetUri = it.assetUri,
                        name = it.name,
                        historyIndex = it.historyIndex
                    )
                },
                clips = clips.map { clip ->
                    EditorClip(
                        id = clip.id,
                        assetUri = clip.assetUri,
                        sourceInMs = clip.sourceInMs,
                        sourceOutMs = clip.sourceOutMs,
                        ordinal = clip.ordinal
                    )
                },
                markers = markers.map { marker ->
                    EditorMarker(
                        id = marker.id,
                        clipId = marker.clipId,
                        offsetMs = marker.offsetMs,
                        label = marker.label
                    )
                },
                historyIndex = timeline?.historyIndex ?: 0,
                historyCount = historyCount
            )
        }
    }

    override suspend fun splitClip(
        timelineId: String,
        clipId: String,
        offsetMs: Long
    ) {
        val clips = clipDao.getByTimeline(timelineId)
        val markers = markerDao.getByTimeline(timelineId)

        val clip = clips.find { it.id == clipId } ?: return
        val duration = clip.sourceOutMs - clip.sourceInMs

        if (offsetMs <= 0L || offsetMs >= duration) return

        val splitSourcePosition = clip.sourceInMs + offsetMs

        val firstPart = clip.copy(sourceOutMs = splitSourcePosition)

        val secondPartId = UUID.randomUUID().toString()

        val secondPart = ClipEntity(
            id = secondPartId,
            timelineId = timelineId,
            assetUri = clip.assetUri,
            sourceInMs = splitSourcePosition,
            sourceOutMs = clip.sourceOutMs,
            ordinal = clip.ordinal + 1
        )

        val newClips = clips.flatMap { current ->
            if (current.id == clipId) {
                listOf(firstPart, secondPart)
            } else {
                listOf(current)
            }
        }

        val newMarkers = markers.map { marker ->
            if (marker.clipId == clipId) {
                if (marker.offsetMs < offsetMs) {
                    marker
                } else {
                    marker.copy(
                        clipId = secondPartId,
                        offsetMs = marker.offsetMs - offsetMs
                    )
                }
            } else {
                marker
            }
        }

        commitEdit(timelineId, newClips, newMarkers)
    }

    override suspend fun trimClipStart(
        timelineId: String,
        clipId: String,
        offsetMs: Long
    ) {
        val clips = clipDao.getByTimeline(timelineId)
        val markers = markerDao.getByTimeline(timelineId)

        val clip = clips.find { it.id == clipId } ?: return
        val duration = clip.sourceOutMs - clip.sourceInMs

        if (offsetMs <= 0L || offsetMs >= duration) return

        val trimmedClip = clip.copy(sourceInMs = clip.sourceInMs + offsetMs)

        val newClips = clips.map { current ->
            if (current.id == clipId) trimmedClip else current
        }

        val newMarkers = markers
            .filter { marker ->
                marker.clipId != clipId || marker.offsetMs >= offsetMs
            }
            .map { marker ->
                if (marker.clipId == clipId) {
                    marker.copy(offsetMs = marker.offsetMs - offsetMs)
                } else {
                    marker
                }
            }

        commitEdit(timelineId, newClips, newMarkers)
    }

    override suspend fun trimClipEnd(
        timelineId: String,
        clipId: String,
        offsetMs: Long
    ) {
        val clips = clipDao.getByTimeline(timelineId)
        val markers = markerDao.getByTimeline(timelineId)

        val clip = clips.find { it.id == clipId } ?: return
        val duration = clip.sourceOutMs - clip.sourceInMs

        if (offsetMs <= 0L || offsetMs >= duration) return

        val trimmedClip = clip.copy(sourceOutMs = clip.sourceInMs + offsetMs)

        val newClips = clips.map { current ->
            if (current.id == clipId) trimmedClip else current
        }

        val newMarkers = markers.filter { marker ->
            marker.clipId != clipId || marker.offsetMs <= offsetMs
        }

        commitEdit(timelineId, newClips, newMarkers)
    }

    override suspend fun deleteClip(
        timelineId: String,
        clipId: String
    ) {
        val clips = clipDao.getByTimeline(timelineId)
        val markers = markerDao.getByTimeline(timelineId)

        if (clips.size <= 1) return

        val newClips = clips.filterNot { it.id == clipId }
        val newMarkers = markers.filterNot { it.clipId == clipId }

        commitEdit(timelineId, newClips, newMarkers)
    }

    override suspend fun deleteTimelineRange(
        timelineId: String,
        rangeStartMs: Long,
        rangeEndMs: Long
    ) {
        val clips = clipDao.getByTimeline(timelineId)
        val markers = markerDao.getByTimeline(timelineId)

        if (clips.isEmpty()) return

        val totalDuration = clips.sumOf { it.sourceOutMs - it.sourceInMs }
        val rangeStart = rangeStartMs.coerceIn(0L, totalDuration)
        val rangeEnd = rangeEndMs.coerceIn(0L, totalDuration)
        if (rangeEnd <= rangeStart) return

        data class CutInfo(
            val firstPartId: String?,
            val secondPartId: String?,
            val localStart: Long,
            val localEnd: Long,
            val removedLen: Long
        )

        val cutByClip = mutableMapOf<String, CutInfo>()
        val newClips = mutableListOf<ClipEntity>()

        var cursor = 0L
        for (clip in clips) {
            val clipStart = cursor
            val duration = clip.sourceOutMs - clip.sourceInMs
            cursor += duration
            val clipEnd = clipStart + duration

            val interStart = maxOf(rangeStart, clipStart)
            val interEnd = minOf(rangeEnd, clipEnd)

            if (interEnd <= interStart) {
                newClips.add(clip)
                continue
            }

            val localStart = (interStart - clipStart).coerceIn(0L, duration)
            val localEnd = (interEnd - clipStart).coerceIn(0L, duration)
            val removedLen = localEnd - localStart
            val hasFirst = localStart > 0L
            val hasSecond = localEnd < duration

            if (!hasFirst && !hasSecond) {
                cutByClip[clip.id] = CutInfo(null, null, localStart, localEnd, removedLen)
                continue
            }

            val firstPartId = if (hasFirst) clip.id else null
            val secondPartId = if (hasSecond) UUID.randomUUID().toString() else null

            if (hasFirst) {
                newClips.add(clip.copy(sourceOutMs = clip.sourceInMs + localStart))
            }
            if (hasSecond) {
                newClips.add(
                    ClipEntity(
                        id = secondPartId!!,
                        timelineId = timelineId,
                        assetUri = clip.assetUri,
                        sourceInMs = clip.sourceInMs + localEnd,
                        sourceOutMs = clip.sourceOutMs,
                        ordinal = 0
                    )
                )
            }

            cutByClip[clip.id] = CutInfo(firstPartId, secondPartId, localStart, localEnd, removedLen)
        }

        if (newClips.isEmpty()) return

        val newMarkers = markers.mapNotNull { marker ->
            val cut = cutByClip[marker.clipId] ?: return@mapNotNull marker
            val m = marker.offsetMs
            when {
                cut.firstPartId != null && m <= cut.localStart -> marker.copy(clipId = cut.firstPartId)
                cut.secondPartId != null && m >= cut.localEnd -> marker.copy(
                    clipId = cut.secondPartId,
                    offsetMs = m - cut.removedLen
                )
                else -> null
            }
        }

        commitEdit(timelineId, newClips, newMarkers)
    }

    override suspend fun addMarker(
        timelineId: String,
        clipId: String,
        offsetMs: Long,
        label: String
    ) {
        val clips = clipDao.getByTimeline(timelineId)
        val markers = markerDao.getByTimeline(timelineId)

        val clip = clips.find { it.id == clipId } ?: return
        val duration = clip.sourceOutMs - clip.sourceInMs

        val safeOffset = offsetMs.coerceIn(0L, duration)

        val marker = MarkerEntity(
            id = UUID.randomUUID().toString(),
            clipId = clipId,
            offsetMs = safeOffset,
            label = label
        )

        commitEdit(timelineId, clips, markers + marker)
    }

    override suspend fun deleteMarker(
        timelineId: String,
        markerId: String
    ) {
        val clips = clipDao.getByTimeline(timelineId)
        val markers = markerDao.getByTimeline(timelineId)

        val newMarkers = markers.filterNot { it.id == markerId }

        commitEdit(timelineId, clips, newMarkers)
    }

    override suspend fun undo(timelineId: String) {
        val timeline = timelineDao.getById(timelineId) ?: return

        if (timeline.historyIndex <= 0) return

        val targetSequence = timeline.historyIndex - 1
        val snapshot = historyDao.getBySequence(timelineId, targetSequence) ?: return

        val clips = parseClips(snapshot.clipsJson)
        val markers = parseMarkers(snapshot.markersJson)

        replaceStateNormalized(timelineId, clips, markers)

        timelineDao.updateHistoryIndex(
            timelineId = timelineId,
            historyIndex = targetSequence,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun redo(timelineId: String) {
        val timeline = timelineDao.getById(timelineId) ?: return

        val maxSequence = historyDao.getMaxSequence(timelineId) ?: 0

        if (timeline.historyIndex >= maxSequence) return

        val targetSequence = timeline.historyIndex + 1
        val snapshot = historyDao.getBySequence(timelineId, targetSequence) ?: return

        val clips = parseClips(snapshot.clipsJson)
        val markers = parseMarkers(snapshot.markersJson)

        replaceStateNormalized(timelineId, clips, markers)

        timelineDao.updateHistoryIndex(
            timelineId = timelineId,
            historyIndex = targetSequence,
            updatedAt = System.currentTimeMillis()
        )
    }

    private suspend fun commitEdit(
        timelineId: String,
        clips: List<ClipEntity>,
        markers: List<MarkerEntity>
    ) {
        val timeline = timelineDao.getById(timelineId) ?: return

        val normalizedClips = normalizeClips(clips)
        val normalizedMarkers = normalizeMarkers(markers, normalizedClips)

        replaceStateNormalized(timelineId, normalizedClips, normalizedMarkers)

        historyDao.deleteAfterSequence(timelineId, timeline.historyIndex)

        val nextSequence = timeline.historyIndex + 1
        val now = System.currentTimeMillis()

        historyDao.insert(
            EditHistoryEntity(
                id = UUID.randomUUID().toString(),
                timelineId = timelineId,
                sequence = nextSequence,
                clipsJson = clipsToJson(normalizedClips),
                markersJson = markersToJson(normalizedMarkers),
                createdAt = now
            )
        )

        timelineDao.updateHistoryIndex(
            timelineId = timelineId,
            historyIndex = nextSequence,
            updatedAt = now
        )
    }

    private suspend fun replaceStateNormalized(
        timelineId: String,
        clips: List<ClipEntity>,
        markers: List<MarkerEntity>
    ) {
        markerDao.deleteByTimeline(timelineId)
        clipDao.deleteByTimeline(timelineId)

        if (clips.isNotEmpty()) {
            clipDao.insertAll(clips)
        }

        if (markers.isNotEmpty()) {
            markerDao.insertAll(markers)
        }
    }

    private fun normalizeClips(clips: List<ClipEntity>): List<ClipEntity> {
        return clips
            .sortedBy { it.ordinal }
            .mapIndexed { index, clip ->
                clip.copy(ordinal = index)
            }
    }

    private fun normalizeMarkers(
        markers: List<MarkerEntity>,
        clips: List<ClipEntity>
    ): List<MarkerEntity> {
        val durationsByClipId = clips.associate { clip ->
            clip.id to (clip.sourceOutMs - clip.sourceInMs)
        }

        return markers
            .filter { durationsByClipId.containsKey(it.clipId) }
            .map { marker ->
                val maxOffset = durationsByClipId[marker.clipId] ?: 0L
                marker.copy(offsetMs = marker.offsetMs.coerceIn(0L, maxOffset))
            }
    }

    private fun clipsToJson(clips: List<ClipEntity>): String {
        val array = JSONArray()

        clips.forEach { clip ->
            val obj = JSONObject()
            obj.put("id", clip.id)
            obj.put("timelineId", clip.timelineId)
            obj.put("assetUri", clip.assetUri)
            obj.put("sourceInMs", clip.sourceInMs)
            obj.put("sourceOutMs", clip.sourceOutMs)
            obj.put("ordinal", clip.ordinal)
            array.put(obj)
        }

        return array.toString()
    }

    private fun markersToJson(markers: List<MarkerEntity>): String {
        val array = JSONArray()

        markers.forEach { marker ->
            val obj = JSONObject()
            obj.put("id", marker.id)
            obj.put("clipId", marker.clipId)
            obj.put("offsetMs", marker.offsetMs)
            obj.put("label", marker.label)
            array.put(obj)
        }

        return array.toString()
    }

    private fun parseClips(json: String): List<ClipEntity> {
        if (json.isBlank()) return emptyList()

        val array = JSONArray(json)

        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)

            ClipEntity(
                id = obj.getString("id"),
                timelineId = obj.getString("timelineId"),
                assetUri = obj.getString("assetUri"),
                sourceInMs = obj.getLong("sourceInMs"),
                sourceOutMs = obj.getLong("sourceOutMs"),
                ordinal = obj.getInt("ordinal")
            )
        }
    }

    private fun parseMarkers(json: String): List<MarkerEntity> {
        if (json.isBlank()) return emptyList()

        val array = JSONArray(json)

        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)

            MarkerEntity(
                id = obj.getString("id"),
                clipId = obj.getString("clipId"),
                offsetMs = obj.getLong("offsetMs"),
                label = obj.getString("label")
            )
        }
    }
}
