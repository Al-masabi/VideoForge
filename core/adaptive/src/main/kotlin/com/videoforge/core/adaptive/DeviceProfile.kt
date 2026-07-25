package com.videoforge.core.adaptive

enum class DeviceClass {
    LOW,
    MID,
    HIGH
}

data class DeviceProfile(
    val cpuAbi: String,
    val cpuCores: Int,
    val totalRamBytes: Long,
    val lowRamDevice: Boolean,
    val freeStorageBytes: Long,
    val deviceClass: DeviceClass
)

data class BatteryState(
    val level: Int,
    val charging: Boolean
)

data class PerformancePolicy(
    val threadCount: Int,
    val thumbnailCount: Int,
    val thumbnailWidth: Int,
    val thumbnailHeight: Int,
    val imageCacheBytes: Long,
    val maxConcurrentTasks: Int,
    val thermalPauseEnabled: Boolean,
    val maxVideoBitrate: Int
)