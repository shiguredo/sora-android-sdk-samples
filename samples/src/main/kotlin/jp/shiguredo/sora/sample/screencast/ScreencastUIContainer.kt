package jp.shiguredo.sora.sample.screencast

import android.annotation.TargetApi
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import jp.shiguredo.sora.sdk.util.SoraLogger

@TargetApi(21)
class ScreencastUIContainer(
    val context: Context,
    val view: View,
    width: Int = WindowManager.LayoutParams.MATCH_PARENT,
    height: Int = WindowManager.LayoutParams.WRAP_CONTENT,
) {
    companion object {
        private val TAG = ScreencastUIContainer::class.simpleName
    }

    private var isOnTop = true
    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    var isAdded: Boolean = false
        private set

    init {
        val params = createLayoutParams(width, height)
        params.gravity = Gravity.TOP
        try {
            windowManager.addView(view, params)
            isAdded = true
        } catch (e: WindowManager.BadTokenException) {
            SoraLogger.w(TAG, "failed to add overlay view", e)
        }
    }

    private fun createLayoutParams(
        width: Int,
        height: Int,
    ): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            createWindowFrags(),
            PixelFormat.TRANSLUCENT,
        )

    private fun createWindowFrags(): Int =
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH

    fun togglePosition() {
        val params = view.layoutParams as WindowManager.LayoutParams
        if (isOnTop) {
            params.gravity = Gravity.BOTTOM
        } else {
            params.gravity = Gravity.TOP
        }
        isOnTop = !isOnTop
        windowManager.updateViewLayout(view, params)
    }

    fun clear() {
        if (isAdded) {
            try {
                windowManager.removeViewImmediate(view)
            } catch (e: IllegalArgumentException) {
                SoraLogger.w(TAG, "failed to remove overlay view", e)
            }
        }
        isAdded = false
    }
}
