package com.videoforge.android.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

fun hasMediaPermission(context: Context): Boolean {
    val primaryPermission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val primaryGranted = ContextCompat.checkSelfPermission(
        context,
        primaryPermission
    ) == PackageManager.PERMISSION_GRANTED

    if (primaryGranted) return true

    return if (Build.VERSION.SDK_INT >= 34) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        false
    }
}

fun permissionToRequest(): String {
    return if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}