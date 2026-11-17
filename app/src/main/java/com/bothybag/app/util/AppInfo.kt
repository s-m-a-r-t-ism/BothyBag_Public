package com.bothybag.app.util

import android.content.Context

/**
 * The app's version name as set in app/build.gradle.kts (for example "1.3"),
 * so the version shown in the app can never drift from the real one.
 */
fun Context.appVersionName(): String {
    return try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: ""
    } catch (e: Exception) {
        ""
    }
}
