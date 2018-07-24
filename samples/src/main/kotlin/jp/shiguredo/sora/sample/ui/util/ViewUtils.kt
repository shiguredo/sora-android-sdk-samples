package jp.shiguredo.sora.sample.ui.util

import android.view.ViewManager
import com.jaredrummler.materialspinner.MaterialSpinner
import org.jetbrains.anko.custom.ankoView

public inline fun ViewManager.materialSpinner(theme: Int = 0, init: MaterialSpinner.() -> Unit) =
        ankoView({ MaterialSpinner(it) }, theme, init)

