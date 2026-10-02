package com.siddesh.smsalerts.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionUtils {

    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.SEND_SMS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAllRequiredPermissions(context: Context): Boolean {
        val sms = hasPermission(context, Manifest.permission.SEND_SMS)
        val location = hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return sms && location
    }

    fun isSmsGranted(context: Context): Boolean =
        hasPermission(context, Manifest.permission.SEND_SMS)

    fun hasSmsPermission(context: Context): Boolean = isSmsGranted(context)

    fun isLocationGranted(context: Context): Boolean =
        hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

    fun hasLocationPermission(context: Context): Boolean = isLocationGranted(context)
}
