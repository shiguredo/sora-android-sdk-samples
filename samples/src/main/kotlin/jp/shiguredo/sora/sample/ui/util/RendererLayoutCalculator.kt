package jp.shiguredo.sora.sample.ui.util

import android.view.View
import android.widget.RelativeLayout

class RendererLayoutCalculator(
    val width: Int,
    val height: Int,
) {
    private var views = mutableListOf<View>()

    fun add(view: View) {
        views.add(view)
        onNumberOfViewChanged()
    }

    fun remove(view: View) {
        views.remove(view)
        onNumberOfViewChanged()
    }

    fun onNumberOfViewChanged() {
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
        val params = view.layoutParams as RelativeLayout.LayoutParams
        params.width = w
        params.height = h
        params.leftMargin = x
        params.topMargin = y
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
