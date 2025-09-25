package jp.shiguredo.sora.sample.ui.util

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.WindowManager

class SoraScreenUtil {

    companion object {

        fun size(context: Context): Point {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val metrics = windowManager.currentWindowMetrics
                val b = metrics.bounds
                Point(b.width(), b.height())
            } else {
                @Suppress("DEPRECATION")
                Point(context.resources.displayMetrics.widthPixels, context.resources.displayMetrics.heightPixels)
            }
        }

        fun statusBarHeight(context: Context): Int {
            val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resId > 0) {
                return context.resources.getDimensionPixelSize(resId)
            } else {
                return 0
            }
        }
    }
}
