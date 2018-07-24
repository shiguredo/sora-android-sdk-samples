package jp.shiguredo.sora.sample.screencast

import android.annotation.TargetApi
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager

@TargetApi(21)
class ScreencastUIContainer(
        val context: Context,
        val view:    View,
            width:   Int,
            height:  Int
) {
    private var isOnTop = true
    private var windowManager: WindowManager? =
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    init {
        val params = createLayoutParams(width, height)
        params.gravity = Gravity.TOP
        windowManager?.addView(view, params)
    }

    private fun createLayoutParams(width: Int, height: Int): WindowManager.LayoutParams {
        val windowType =
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE;
                }
        return WindowManager.LayoutParams(width, height, windowType,
                createWindowFrags(), PixelFormat.TRANSLUCENT
        )
    }

    private fun createWindowFrags(): Int {
        return  WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    }

    fun togglePosition() {
        val params = view.layoutParams as WindowManager.LayoutParams
        if (isOnTop) {
            params.gravity = Gravity.BOTTOM
        } else {
            params.gravity = Gravity.TOP
        }
        isOnTop = !isOnTop
        windowManager?.updateViewLayout(view, params)
    }

    fun clear() {
        if (view.isAttachedToWindow) {
            windowManager?.removeViewImmediate(view)
        }
        windowManager = null
    }

}

