package jp.shiguredo.sora.sample.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.audio.VolumeMonitoringSink
import kotlin.math.roundToInt

/**
 * ユーザー別音量表示用のRecyclerViewアダプター（音量バーのみ）
 * AudioSinkベースの実際のトラック音量を使用
 */
class UserVolumeAdapter : ListAdapter<UserVolumeAdapter.UserVolumeItem, UserVolumeAdapter.ViewHolder>(DiffCallback()) {

    data class UserVolumeItem(
        val trackId: String,
        val volumeLevel: VolumeMonitoringSink.VolumeLevel? = null
    )

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val volumeBarText: TextView = itemView.findViewById(R.id.volumeBarText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_volume, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    private fun ViewHolder.bind(item: UserVolumeItem) {
        val volumeLevel = item.volumeLevel
        if (volumeLevel != null) {
            // JSのWebRTC実装と同様の仕様でピーク振幅を使用
            val peakAmplitude = volumeLevel.peakVolume // 0..1
            val volumeRatio = peakAmplitude.coerceIn(0f, 1f)

            // バー: 20 コマ分を volumeRatio で埋める
            val barLength = (volumeRatio * 20).roundToInt().coerceIn(0, 20)
            val volumeBar = "█".repeat(barLength) + "░".repeat(20 - barLength)
            volumeBarText.text = volumeBar
        } else {
            // データなし
            volumeBarText.text = "░".repeat(20)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<UserVolumeItem>() {
        override fun areItemsTheSame(oldItem: UserVolumeItem, newItem: UserVolumeItem): Boolean {
            return oldItem.trackId == newItem.trackId
        }

        override fun areContentsTheSame(oldItem: UserVolumeItem, newItem: UserVolumeItem): Boolean {
            // 音量レベルの変化を検出するため、わずかな変化でも更新
            return oldItem == newItem
        }
    }
}
