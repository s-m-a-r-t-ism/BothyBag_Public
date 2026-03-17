package com.bothybag.app.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.bothybag.app.R

object BothyMarkerUtils {

    private val markerCache = mutableMapOf<String, Drawable>()

    fun getMarkerIcon(context: Context, isMba: Boolean, isClosed: Boolean, isVisited: Boolean): Drawable {
        val key = "${isMba}_${isClosed}_${isVisited}"
        markerCache[key]?.let { return it }

        val baseResId = when {
            isClosed -> R.drawable.ic_bothy_closed
            isMba -> R.drawable.ic_bothy_mba
            else -> R.drawable.ic_bothy_other
        }

        val baseDrawable = ContextCompat.getDrawable(context, baseResId)!!
        val size = (48 * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        baseDrawable.setBounds(0, 0, size, size)
        baseDrawable.draw(canvas)

        // Add visited overlay: green ring
        if (isVisited && !isClosed) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF4CAF50.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 4f * context.resources.displayMetrics.density
            }
            val radius = size / 2f - paint.strokeWidth
            canvas.drawCircle(size / 2f, size / 2f, radius, paint)

            // Small checkmark in bottom-right
            val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF4CAF50.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 3f * context.resources.displayMetrics.density
                strokeCap = Paint.Cap.ROUND
            }
            val cx = size * 0.75f
            val cy = size * 0.75f
            val s = size * 0.1f
            canvas.drawLine(cx - s, cy, cx - s * 0.3f, cy + s * 0.7f, checkPaint)
            canvas.drawLine(cx - s * 0.3f, cy + s * 0.7f, cx + s, cy - s * 0.5f, checkPaint)
        }

        val drawable = BitmapDrawable(context.resources, bitmap)
        markerCache[key] = drawable
        return drawable
    }

    fun clearCache() {
        markerCache.clear()
    }
}
