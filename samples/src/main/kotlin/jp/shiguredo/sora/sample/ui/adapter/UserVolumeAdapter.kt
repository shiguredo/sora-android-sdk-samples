package jp.shiguredo.sora.sample.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.audio.StreamVolumeMonitor
import kotlin.math.roundToInt

/**
 * ユーザー別音量表示用のRecyclerViewアダプター
 */
class UserVolumeAdapter : ListAdapter<UserVolumeAdapter.UserVolumeItem, UserVolumeAdapter.ViewHolder>(DiffCallback()) {

    data class UserVolumeItem(
        val streamId: String,
        val volumeLevel: StreamVolumeMonitor.VolumeLevel? = null
    )

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userIdText: TextView = itemView.findViewById(R.id.userIdText)
        val volumeBarText: TextView = itemView.findViewById(R.id.volumeBarText)
        val volumeDbText: TextView = itemView.findViewById(R.id.volumeDbText)
        val volumeIndicator: View = itemView.findViewById(R.id.volumeIndicator)
        val volumePercentText: TextView = itemView.findViewById(R.id.volumePercentText)
        val volumePeakText: TextView = itemView.findViewById(R.id.volumePeakText)
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
        userIdText.text = "User: ${item.streamId.take(8)}"

        val volumeLevel = item.volumeLevel
        if (volumeLevel != null) {
            // 音量バーの表示（0-20の範囲でバーを表示）
            val barLength = (volumeLevel.rmsVolume * 20).roundToInt()
            val volumeBar = "█".repeat(barLength) + "░".repeat(20 - barLength)
            volumeBarText.text = volumeBar

            // dB値の表示
            volumeDbText.text = "${String.format("%.1f", volumeLevel.rmsDb)} dB"

            // パーセンテージ表示（0-100%）
            val volumePercent = (volumeLevel.rmsVolume * 100).roundToInt()
            volumePercentText.text = "${volumePercent}%"

            // ピーク音量表示
            volumePeakText.text = "Peak: ${String.format("%.1f", volumeLevel.peakDb)}"

            // 音量レベルに応じた色分け
            val color = when {
                volumeLevel.rmsVolume < 0.3f -> Color.parseColor("#4CAF50") // 緑
                volumeLevel.rmsVolume < 0.7f -> Color.parseColor("#FF9800") // オレンジ
                else -> Color.parseColor("#F44336") // 赤
            }
            volumeIndicator.setBackgroundColor(color)

            // 音量レベルに応じた透明度
            val alpha = (0.3f + volumeLevel.rmsVolume * 0.7f).coerceIn(0.3f, 1.0f)
            volumeIndicator.alpha = alpha

        } else {
            volumeBarText.text = "░".repeat(20)
            volumeDbText.text = "-- dB"
            volumePercentText.text = "--%"
            volumePeakText.text = "Peak: --"
            volumeIndicator.setBackgroundColor(Color.GRAY)
            volumeIndicator.alpha = 0.3f
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<UserVolumeItem>() {
        override fun areItemsTheSame(oldItem: UserVolumeItem, newItem: UserVolumeItem): Boolean {
            return oldItem.streamId == newItem.streamId
        }

        override fun areContentsTheSame(oldItem: UserVolumeItem, newItem: UserVolumeItem): Boolean {
            // 音量レベルの変化を検出するため、わずかな変化でも更新
            return oldItem == newItem
        }
    }
}
