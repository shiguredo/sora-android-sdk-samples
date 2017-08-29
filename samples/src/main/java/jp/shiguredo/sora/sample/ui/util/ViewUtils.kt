package jp.shiguredo.sora.sample.ui.util

import android.view.ViewManager
import com.jaredrummler.materialspinner.MaterialSpinner
import com.wefika.flowlayout.FlowLayout
import org.jetbrains.anko.custom.ankoView
import org.webrtc.SurfaceViewRenderer

public inline fun ViewManager.flowLayout(theme: Int = 0) = flowLayout(theme) {}
public inline fun ViewManager.flowLayout(theme: Int = 0, init: FlowLayout.() -> Unit) =
        ankoView({ FlowLayout(it) }, theme, init)

public inline fun ViewManager.materialSpinner(theme: Int = 0) = materialSpinner(theme) {}
public inline fun ViewManager.materialSpinner(theme: Int = 0, init: MaterialSpinner.() -> Unit) =
        ankoView({ MaterialSpinner(it) }, theme, init)

