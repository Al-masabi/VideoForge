package com.videoforge.core.adaptive

import android.app.ActivityManager
import android.content.Context
import android.media.MediaCodecList
import android.os.Build
import android.os.Environment
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceProfiler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun profile(): DeviceProfile {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRam = memoryInfo.totalMem
        val lowRam = activityManager.isLowRamDevice
        val cpuCores = Runtime.getRuntime().availableProcessors()
        val cpuAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"

        val stat = StatFs(Environment.getDataDirectory().absolutePath)
        val freeStorage = stat.availableBytes

        val deviceClass = classify(totalRam, lowRam)

        return DeviceProfile(
            cpuAbi = cpuAbi,
            cpuCores = cpuCores,
            totalRamBytes = totalRam,
            lowRamDevice = lowRam,
            freeStorageBytes = freeStorage,
            deviceClass = deviceClass
        )
    }

    private fun classify(totalRam: Long, lowRam: Boolean): DeviceClass {
        if (lowRam || totalRam < LOW_RAM_THRESHOLD) return DeviceClass.LOW
        if (totalRam < HIGH_RAM_THRESHOLD) return DeviceClass.MID
        return DeviceClass.HIGH
    }

    fun supportsEncoder(mime: String): Boolean {
        return MediaCodecList(MediaCodecList.ALL_CODECS)
            .codecInfos
            .any { info ->
                info.isEncoder && info.supportedTypes.any {
                    it.equals(mime, ignoreCase = true)
                }
            }
    }

    companion object {
        private const val LOW_RAM_THRESHOLD = 3_500_000_000L
        private const val HIGH_RAM_THRESHOLD = 7_500_000_000L
    }
}