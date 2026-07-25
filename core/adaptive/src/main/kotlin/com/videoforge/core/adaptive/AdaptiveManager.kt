package com.videoforge.core.adaptive

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdaptiveManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceProfiler: DeviceProfiler
) {

    val deviceProfile: DeviceProfile by lazy {
        deviceProfiler.profile()
    }

    private val powerManager: PowerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun currentPolicy(): PerformancePolicy {
        return when (deviceProfile.deviceClass) {
            DeviceClass.LOW -> PerformancePolicy(
                threadCount = 2,
                thumbnailCount = 4,
                thumbnailWidth = 240,
                thumbnailHeight = 135,
                imageCacheBytes = 48L * 1024 * 1024,
                maxConcurrentTasks = 1,
                thermalPauseEnabled = true,
                maxVideoBitrate = 12_000_000
            )

            DeviceClass.MID -> PerformancePolicy(
                threadCount = 4,
                thumbnailCount = 8,
                thumbnailWidth = 320,
                thumbnailHeight = 180,
                imageCacheBytes = 96L * 1024 * 1024,
                maxConcurrentTasks = 1,
                thermalPauseEnabled = true,
                maxVideoBitrate = 16_000_000
            )

            DeviceClass.HIGH -> PerformancePolicy(
                threadCount = 6,
                thumbnailCount = 10,
                thumbnailWidth = 400,
                thumbnailHeight = 225,
                imageCacheBytes = 160L * 1024 * 1024,
                maxConcurrentTasks = 2,
                thermalPauseEnabled = true,
                maxVideoBitrate = 20_000_000
            )
        }
    }

    fun currentThermalStatus(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            powerManager.currentThermalStatus
        } else {
            PowerManager.THERMAL_STATUS_NONE
        }
    }

    fun currentBatteryState(): BatteryState {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val levelPct = if (level >= 0 && scale > 0) {
            level * 100 / scale
        } else {
            0
        }

        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        return BatteryState(levelPct, charging)
    }
}