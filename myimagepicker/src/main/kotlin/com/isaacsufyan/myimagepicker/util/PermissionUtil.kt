package com.isaacsufyan.myimagepicker.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionUtil {

    fun isPermissionGranted(context: Context, permission: String): Boolean {
        val selfPermission = ContextCompat.checkSelfPermission(context, permission)
        return selfPermission == PackageManager.PERMISSION_GRANTED
    }

    fun isPermissionGranted(context: Context, permissions: Array<String>): Boolean {
        return permissions.filter {
            isPermissionGranted(context, it)
        }.size == permissions.size
    }

    fun isPermissionInManifest(context: Context, permission: String): Boolean {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val permissions = packageInfo.requestedPermissions

        if (permissions.isNullOrEmpty())
            return false

        for (perm in permissions) {
            if (perm == permission)
                return true
        }

        return false
    }
}
