/**
 * AutoCompleteTextView をドロップダウンとして使う拡張群。
 * Exposed Dropdown Menu を利用するためのヘルパー。
 * セットアップ画面のドロップダウン UI で利用する。
 */
package jp.shiguredo.sora.sample.ui

import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView

data class DropdownConfig(
    val view: AutoCompleteTextView,
    val items: List<String>,
    val defaultIndex: Int = 0,
)

/**
 * ドロップダウンメニューを初期化する
 */
fun AutoCompleteTextView.initializeDropdown(
    items: List<String>,
    defaultIndex: Int = 0,
) {
    if (items.isEmpty()) return
    setAdapter(
        ArrayAdapter(
            context,
            com.google.android.material.R.layout.mtrl_auto_complete_simple_item,
            items,
        ),
    )
    val safeIndex = defaultIndex.coerceIn(items.indices)
    // AutoCompleteTextView#setText のオーバーロード
    // 第二引数の TextWatcher への通知フラグは不要のため Off にしている
    setText(items[safeIndex], false)
}

/**
 * ドロップダウンメニューとアイテムリストを一括で初期化する
 */
fun initializeDropdowns(dropdowns: List<DropdownConfig>) {
    dropdowns.forEach { config ->
        config.view.initializeDropdown(config.items, config.defaultIndex)
    }
}

fun AutoCompleteTextView.selectedItem(): String = text?.toString().orEmpty()
