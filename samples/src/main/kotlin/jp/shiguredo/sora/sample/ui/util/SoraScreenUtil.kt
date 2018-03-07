package jp.shiguredo.sora.sample.ui.util

import android.content.Context
import android.graphics.Point
import android.view.WindowManager

class SoraScreenUtil {

    companion object {

        fun size(context: Context): Point {
            val windowManager =
                    context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val size = Point()
            windowManager.defaultDisplay.getSize(size)
            return size
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

