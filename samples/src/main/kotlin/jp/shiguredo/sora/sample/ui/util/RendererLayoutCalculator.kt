package jp.shiguredo.sora.sample.ui.util

import android.view.View
import android.widget.RelativeLayout

/**
 * タイルの配置と、タイル内に表示する映像矩形を計算する。
 *
 * 格子サイズは [updateContainerSize] で与えたコンテナの実測サイズを用いる。
 */
class RendererLayoutCalculator(
    width: Int = 0,
    height: Int = 0,
) {
    private var width = width
    private var height = height
    private val views = mutableListOf<View>()
    private val cellRects = mutableMapOf<View, LayoutRect>()
    private val videoSizes = mutableMapOf<View, VideoSize>()

    fun add(view: View) {
        views.add(view)
        onNumberOfViewChanged()
    }

    fun remove(view: View) {
        views.remove(view)
        cellRects.remove(view)
        videoSizes.remove(view)
        onNumberOfViewChanged()
    }

    // コンテナの実測サイズが変わった場合に格子と映像矩形を再計算する
    fun updateContainerSize(
        width: Int,
        height: Int,
    ) {
        if (width <= 0 || height <= 0) {
            return
        }
        if (this.width == width && this.height == height) {
            return
        }
        this.width = width
        this.height = height
        onNumberOfViewChanged()
    }

    // 映像の解像度を設定し、そのビューに表示する映像矩形を再計算する
    fun setVideoSize(
        view: View,
        width: Int,
        height: Int,
    ) {
        if (width <= 0 || height <= 0 || !views.contains(view)) {
            return
        }

        val current = videoSizes[view]
        if (current != null && current.width == width && current.height == height) {
            return
        }

        videoSizes[view] = VideoSize(width, height)
        layoutVideoRect(view)
    }

    // まだ解像度を受信していない場合は null を返す
    fun getVideoSize(view: View): VideoSize? = videoSizes[view]

    private fun onNumberOfViewChanged() {
        val count = views.size
        when (count) {
            0 -> {}
            1 -> {
                layout1()
            }
            2 -> {
                layout2()
            }
            3 -> {
                layout3()
            }
            4 -> {
                layout4()
            }
            5 -> {
                layout5()
            }
            6 -> {
                layout6()
            }
            7 -> {
                layout7()
            }
            8 -> {
                layout8()
            }
            9 -> {
                layout9()
            }
            10 -> {
                layout10()
            }
            11 -> {
                layout11()
            }
            12 -> {
                layout12()
            }
            else -> {}
        }
    }

    private fun layoutView(
        view: View,
        w: Int,
        h: Int,
        x: Int,
        y: Int,
    ) {
        cellRects[view] = LayoutRect(x, y, w, h)
        layoutVideoRect(view)
    }

    private fun layoutVideoRect(view: View) {
        val cell = cellRects[view] ?: return
        val videoSize = videoSizes[view]
        val rect = if (videoSize == null) cell else VideoRectCalculator.calculateInCell(cell, videoSize)
        val params = view.layoutParams as RelativeLayout.LayoutParams
        params.width = rect.width
        params.height = rect.height
        params.leftMargin = rect.left
        params.topMargin = rect.top
        view.layoutParams = params
    }

    private fun layout1() {
        layoutView(views[0], width, height, 0, 0)
    }

    private fun layout2() {
        val h2 = height / 2
        layoutView(views[0], width, h2, 0, 0)
        layoutView(views[1], width, h2, 0, h2)
    }

    private fun layout3() {
        val h2 = height / 2
        val w2 = width / 2
        layoutView(views[0], width, h2, 0, 0)
        layoutView(views[1], w2, h2, 0, h2)
        layoutView(views[2], w2, h2, w2, h2)
    }

    private fun layout4() {
        val h2 = height / 2
        val w2 = width / 2
        layoutView(views[0], w2, h2, 0, 0)
        layoutView(views[1], w2, h2, w2, 0)
        layoutView(views[2], w2, h2, 0, h2)
        layoutView(views[3], w2, h2, w2, h2)
    }

    private fun layout5() {
        val h3 = height / 3
        val w2 = width / 2
        layoutView(views[0], w2, h3, 0, 0)
        layoutView(views[1], w2, h3, w2, 0)
        layoutView(views[2], w2, h3, 0, h3)
        layoutView(views[3], w2, h3, w2, h3)
        layoutView(views[4], w2, h3, 0, h3 * 2)
    }

    private fun layout6() {
        val h3 = height / 3
        val w2 = width / 2
        layoutView(views[0], w2, h3, 0, 0)
        layoutView(views[1], w2, h3, w2, 0)
        layoutView(views[2], w2, h3, 0, h3)
        layoutView(views[3], w2, h3, w2, h3)
        layoutView(views[4], w2, h3, 0, h3 * 2)
        layoutView(views[5], w2, h3, w2, h3 * 2)
    }

    private fun layout7() {
        val h3 = height / 3
        val w3 = width / 3

        layoutView(views[0], w3, h3, 0, 0)
        layoutView(views[1], w3, h3, w3, 0)
        layoutView(views[2], w3, h3, w3 * 2, 0)

        layoutView(views[3], w3, h3, 0, h3)
        layoutView(views[4], w3, h3, w3, h3)
        layoutView(views[5], w3, h3, w3 * 2, h3)

        layoutView(views[6], w3, h3, 0, h3 * 2)
    }

    private fun layout8() {
        val h3 = height / 3
        val w3 = width / 3

        layoutView(views[0], w3, h3, 0, 0)
        layoutView(views[1], w3, h3, w3, 0)
        layoutView(views[2], w3, h3, w3 * 2, 0)

        layoutView(views[3], w3, h3, 0, h3)
        layoutView(views[4], w3, h3, w3, h3)
        layoutView(views[5], w3, h3, w3 * 2, h3)

        layoutView(views[6], w3, h3, 0, h3 * 2)
        layoutView(views[7], w3, h3, w3, h3 * 2)
    }

    private fun layout9() {
        val h3 = height / 3
        val w3 = width / 3

        layoutView(views[0], w3, h3, 0, 0)
        layoutView(views[1], w3, h3, w3, 0)
        layoutView(views[2], w3, h3, w3 * 2, 0)

        layoutView(views[3], w3, h3, 0, h3)
        layoutView(views[4], w3, h3, w3, h3)
        layoutView(views[5], w3, h3, w3 * 2, h3)

        layoutView(views[6], w3, h3, 0, h3 * 2)
        layoutView(views[7], w3, h3, w3, h3 * 2)
        layoutView(views[8], w3, h3, w3 * 2, h3 * 2)
    }

    private fun layout10() {
        val h3 = height / 3
        val w4 = width / 4
        layoutView(views[0], w4, h3, 0, 0)
        layoutView(views[1], w4, h3, w4, 0)
        layoutView(views[2], w4, h3, w4 * 2, 0)
        layoutView(views[3], w4, h3, w4 * 3, 0)
        layoutView(views[4], w4, h3, 0, h3)
        layoutView(views[5], w4, h3, w4, h3)
        layoutView(views[6], w4, h3, w4 * 2, h3)
        layoutView(views[7], w4, h3, w4 * 3, h3)
        layoutView(views[8], w4, h3, 0, h3 * 2)
        layoutView(views[9], w4, h3, w4, h3 * 2)
    }

    private fun layout11() {
        val h3 = height / 3
        val w4 = width / 4
        layoutView(views[0], w4, h3, 0, 0)
        layoutView(views[1], w4, h3, w4, 0)
        layoutView(views[2], w4, h3, w4 * 2, 0)
        layoutView(views[3], w4, h3, w4 * 3, 0)
        layoutView(views[4], w4, h3, 0, h3)
        layoutView(views[5], w4, h3, w4, h3)
        layoutView(views[6], w4, h3, w4 * 2, h3)
        layoutView(views[7], w4, h3, w4 * 3, h3)
        layoutView(views[8], w4, h3, 0, h3 * 2)
        layoutView(views[9], w4, h3, w4, h3 * 2)
        layoutView(views[10], w4, h3, w4 * 2, h3 * 2)
    }

    private fun layout12() {
        val h3 = height / 3
        val w4 = width / 4
        layoutView(views[0], w4, h3, 0, 0)
        layoutView(views[1], w4, h3, w4, 0)
        layoutView(views[2], w4, h3, w4 * 2, 0)
        layoutView(views[3], w4, h3, w4 * 3, 0)
        layoutView(views[4], w4, h3, 0, h3)
        layoutView(views[5], w4, h3, w4, h3)
        layoutView(views[6], w4, h3, w4 * 2, h3)
        layoutView(views[7], w4, h3, w4 * 3, h3)
        layoutView(views[8], w4, h3, 0, h3 * 2)
        layoutView(views[9], w4, h3, w4, h3 * 2)
        layoutView(views[10], w4, h3, w4 * 2, h3 * 2)
        layoutView(views[11], w4, h3, w4 * 3, h3 * 2)
    }
}
