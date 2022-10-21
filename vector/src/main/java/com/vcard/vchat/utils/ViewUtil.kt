package com.vcard.vchat.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.View.MeasureSpec
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.core.content.ContextCompat
import timber.log.Timber

object ViewUtil {

    fun loadBitmapFromView(view: View): Bitmap {
        view.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        val bitmap = Bitmap.createBitmap(
                view.measuredWidth, view.measuredHeight,
                Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        view.draw(canvas)
        return bitmap
    }

    fun drawTextToBitmap(
            gContext: Context,
            bitmap: Bitmap,
            gText: String
    ): Bitmap {
        val resources = gContext.resources
        val scale = resources.displayMetrics.density
        var bitmapConfig = bitmap.config
        // set default bitmap config if none
        if (bitmapConfig == null) {
            bitmapConfig = Bitmap.Config.ARGB_8888
        }
        // resource bitmaps are immutable,
        // so we need to convert it to mutable one
        val bitmapCopy = bitmap.copy(bitmapConfig, true)
        val canvas = Canvas(bitmapCopy)
        // new antialised Paint
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // text color - #3D3D3D
        paint.color = Color.rgb(61, 61, 61)
        // text size in pixels
        paint.textSize = (14 * scale)
        // text shadow
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE)

        // draw text to the Canvas
        val bounds = Rect()
        paint.getTextBounds(gText, 0, gText.length, bounds)

        //higher horizontal: goes to the right more
        //higher vertical: goes up
        val horizontalSpacing = (bitmap.width - bounds.width()) / 2

        //if we want to put the text above
        //val verticalSpacing = 456

        //if we want to put the text below
        val verticalSpacing = 0

        val y = bitmap.height - verticalSpacing
        canvas.drawText(gText, horizontalSpacing.toFloat(), y.toFloat(), paint)

        return bitmapCopy
    }
}
