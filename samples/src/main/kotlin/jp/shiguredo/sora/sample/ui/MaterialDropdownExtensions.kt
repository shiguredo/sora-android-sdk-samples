/**
* AutoCompleteTextView をドロップダウンとして使う拡張群
* Exposed Dropdown Menu を利用するためのヘルパー
* 接続設定のドロップダウン UI に利用する
*/
package jp.shiguredo.sora.sample.ui

import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView

fun AutoCompleteTextView.setDropdownItems(
    items: List<String>,
    defaultIndex: Int = 0,
) {
    if (items.isEmpty()) return
    setAdapter(ArrayAdapter(context, android.R.layout.simple_list_item_1, items))
    val safeIndex = defaultIndex.coerceIn(items.indices)
    setText(items[safeIndex], false)
}

fun AutoCompleteTextView.selectedItem(): String = text?.toString().orEmpty()
