package com.siddesh.smsalerts.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Centralized Permission Manager.
 * Checks, verifies, and manages runtime permissions contextually.
 */
object PermissionManager {

    const val PERMISSION_READ_CONTACTS = Manifest.permission.READ_CONTACTS
    const val PERMISSION_SEND_SMS = Manifest.permission.SEND_SMS
    const val PERMISSION_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
    const val PERMISSION_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
    const val PERMISSION_RECORD_AUDIO = Manifest.permission.RECORD_AUDIO
    val PERMISSION_POST_NOTIFICATIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        ""
    }

    fun hasPermission(context: Context, permission: String): Boolean {
        if (permission.isBlank()) return true
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasContactsPermission(context: Context): Boolean =
        hasPermission(context, PERMISSION_READ_CONTACTS)

    fun hasSmsPermission(context: Context): Boolean =
        hasPermission(context, PERMISSION_SEND_SMS)

    fun hasLocationPermission(context: Context): Boolean =
        hasPermission(context, PERMISSION_FINE_LOCATION) || hasPermission(context, PERMISSION_COARSE_LOCATION)

    fun hasRecordAudioPermission(context: Context): Boolean =
        hasPermission(context, PERMISSION_RECORD_AUDIO)

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    }

    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        if (permission.isBlank()) return false
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
