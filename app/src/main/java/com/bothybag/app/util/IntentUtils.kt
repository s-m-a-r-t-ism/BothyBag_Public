package com.bothybag.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Opens [url] in an external app (browser, etc.) via ACTION_VIEW, guarding against
 * a missing handler or malformed/blank URL. Without this guard an unhandled link
 * throws ActivityNotFoundException and force-closes the app.
 */
fun Context.openUrlSafely(url: String?) {
    if (url.isNullOrBlank()) {
        Toast.makeText(this, "No link available", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "No app found to open this link", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
    }
}
