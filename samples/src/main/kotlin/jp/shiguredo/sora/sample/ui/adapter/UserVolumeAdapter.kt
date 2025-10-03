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
        val volumePeakText: TextView = itemView.findViewById(R.id.volumePeakText)
        val rtpLevelText: TextView = itemView.findViewById(R.id.rtpLevelText) // RTP Audio Level用を追加
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
            // RTP Audio Level (0-127) をメインの表示として使用
            val rtpLevel = volumeLevel.rtpLevel // 0..127
            val rtpRatio = ((127 - rtpLevel) / 127f).coerceIn(0f, 1f) // 0が最大音量、127が無音なので反転

            // バー: 20 コマ分を rtpRatio で埋める
            val barLength = (rtpRatio * 20).roundToInt().coerceIn(0, 20)
            val volumeBar = "█".repeat(barLength) + "░".repeat(20 - barLength)
            volumeBarText.text = volumeBar

            // RTP Audio Level (0-127) の表示
            rtpLevelText.text = "RTP Level: $rtpLevel / 127"

            // dB値の表示 (RMS)
            volumeDbText.text = "${String.format("%.1f", volumeLevel.rmsDb)} dB"

            // Peak dB
            volumePeakText.text = "Peak: ${String.format("%.1f", volumeLevel.peakDb)}"

            // 色分け (rtpRatio 基準)
            val color = when {
                rtpRatio < 0.3f -> Color.parseColor("#4CAF50") // 緑 (低音量)
                rtpRatio < 0.7f -> Color.parseColor("#FF9800") // オレンジ (中音量)
                else -> Color.parseColor("#F44336") // 赤 (高音量)
            }
            volumeIndicator.setBackgroundColor(color)

            // 透明度 (視覚的強調) - rtpRatio ベース
            val alpha = (0.3f + rtpRatio * 0.7f).coerceIn(0.3f, 1.0f)
            volumeIndicator.alpha = alpha
        } else {
            // データなし
            volumeBarText.text = "░".repeat(20)
            volumeDbText.text = "-- dB"
            volumePeakText.text = "Peak: --"
            rtpLevelText.text = "RTP Level: -- / 127"
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
