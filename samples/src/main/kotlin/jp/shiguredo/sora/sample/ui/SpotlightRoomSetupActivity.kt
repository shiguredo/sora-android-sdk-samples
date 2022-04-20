package jp.shiguredo.sora.sample.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.jaredrummler.materialspinner.MaterialSpinner
import jp.shiguredo.sora.sample.databinding.ActivitySpotlightRoomSetupBinding
import jp.shiguredo.sora.sample.option.SoraFrameSize
import jp.shiguredo.sora.sdk.channel.option.SoraVideoOption

class SpotlightRoomSetupActivity : AppCompatActivity() {

    companion object {
        private val TAG = SpotlightRoomSetupActivity::class.simpleName
    }

    private val spotlightNumberOptions = listOf("未指定", "1", "2", "3", "4", "5", "6", "7", "8")
    private val videoCodecOptions = listOf("VP8", "H264")
    private val audioCodecOptions = listOf("OPUS")
    private val audioBitRateOptions = listOf(
        "未指定", "8", "16", "24", "32",
        "64", "96", "128", "256"
    )
    private val videoEnabledOptions = listOf("有効", "無効")
    private val audioEnabledOptions = listOf("有効", "無効")
    private val roleOptions = listOf("SENDRECV", "SENDONLY", "RECVONLY")
    private val spotlightFocusRidOptions = listOf("未指定", "none", "r0", "r1", "r2")
    private val spotlightUnfocusRidOptions = listOf("未指定", "none", "r0", "r1", "r2")
    private val videoBitRateOptions = listOf(
        "500", "200", "700", "1200", "2500", "4000", "5000",
        "10000", "15000", "20000", "30000"
    )
    private val videoSizeOptions = SoraFrameSize.landscape.filter {
        // FHD より大きいものを取り除く
        it.value.y <= SoraVideoOption.FrameSize.Landscape.FHD.y
    }.keys.toList()
    private val fpsOptions = listOf("30", "10", "15", "20", "24", "60")
    private val dataChannelSignalingOptions = listOf("未指定", "無効", "有効")
    private val ignoreDisconnectWebSocketOptions = listOf("未指定", "無効", "有効")

    private lateinit var binding: ActivitySpotlightRoomSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivitySpotlightRoomSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.start.setOnClickListener { startSpotlightChat() }

        binding.spotlightNumberSelection.name.text = "スポットライト数"
        binding.spotlightNumberSelection.spinner.setItems(spotlightNumberOptions)
        binding.roleSelection.name.text = "ロール"
        binding.roleSelection.spinner.setItems(roleOptions)
        binding.spotlightFocusRidSelection.name.text = "フォーカス時の rid"
        binding.spotlightFocusRidSelection.spinner.setItems(spotlightFocusRidOptions)
        binding.spotlightUnfocusRidSelection.name.text = "非フォーカス時の rid"
        binding.spotlightUnfocusRidSelection.spinner.setItems(spotlightUnfocusRidOptions)
        binding.videoCodecSelection.name.text = "映像コーデック"
        binding.videoCodecSelection.spinner.setItems(videoCodecOptions)
        binding.videoEnabledSelection.name.text = "映像の有無"
        binding.videoEnabledSelection.spinner.setItems(videoEnabledOptions)
        binding.audioCodecSelection.name.text = "音声コーデック"
        binding.audioCodecSelection.spinner.setItems(audioCodecOptions)
        binding.audioEnabledSelection.name.text = "音声の有無"
        binding.audioEnabledSelection.spinner.setItems(audioEnabledOptions)
        binding.audioBitRateSelection.name.text = "音声ビットレート"
        binding.audioBitRateSelection.spinner.setItems(audioBitRateOptions)
        binding.videoBitRateSelection.name.text = "映像ビットレート"
        binding.videoBitRateSelection.spinner.setItems(videoBitRateOptions)
        binding.videoSizeSelection.name.text = "映像サイズ"
        binding.videoSizeSelection.spinner.setItems(videoSizeOptions)
        binding.videoSizeSelection.spinner.selectedIndex = 3
        binding.fpsSelection.name.text = "フレームレート"
        binding.fpsSelection.spinner.setItems(fpsOptions)
        binding.dataChannelSignalingSelection.name.text = "データチャネル"
        binding.dataChannelSignalingSelection.spinner.setItems(dataChannelSignalingOptions)
        binding.ignoreDisconnectWebSocketSelection.name.text = "WS 切断を無視"
        binding.ignoreDisconnectWebSocketSelection.spinner.setItems(ignoreDisconnectWebSocketOptions)
    }

    private fun startSpotlightChat() {
        val channelName = binding.channelNameInput!!.text.toString()
        if (channelName.isEmpty()) {
            showInputError()
            return
        }

        val spotlightNumber = selectedItem(binding.spotlightNumberSelection.spinner)
        val role = selectedItem(binding.roleSelection.spinner)
        var spotlightFocusRid = selectedItem(binding.spotlightFocusRidSelection.spinner)
        var spotlightUnfocusRid = selectedItem(binding.spotlightUnfocusRidSelection.spinner)
        val videoCodec = selectedItem(binding.videoCodecSelection.spinner)
        val audioCodec = selectedItem(binding.audioCodecSelection.spinner)
        val audioBitRate = selectedItem(binding.audioBitRateSelection.spinner)
        val audioEnabled = selectedItem(binding.audioEnabledSelection.spinner)
        val videoEnabled = selectedItem(binding.videoEnabledSelection.spinner)
        val videoBitRate = selectedItem(binding.videoBitRateSelection.spinner)
        val videoSize = selectedItem(binding.videoSizeSelection.spinner)
        val fps = selectedItem(binding.fpsSelection.spinner)
        val dataChannelSignaling = selectedItem(binding.dataChannelSignalingSelection.spinner)
        val ignoreDisconnectWebSocket = selectedItem(binding.ignoreDisconnectWebSocketSelection.spinner)

        val intent = Intent(this, SimulcastActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channelName)
        intent.putExtra("SPOTLIGHT", "有効")
        intent.putExtra("SPOTLIGHT_NUMBER", spotlightNumber)
        intent.putExtra("SPOTLIGHT_FOCUS_RID", spotlightFocusRid)
        intent.putExtra("SPOTLIGHT_UNFOCUS_RID", spotlightUnfocusRid)
        intent.putExtra("ROLE", role)
        intent.putExtra("VIDEO_CODEC", videoCodec)
        intent.putExtra("AUDIO_CODEC", audioCodec)
        intent.putExtra("AUDIO_BIT_RATE", audioBitRate)
        intent.putExtra("AUDIO_ENABLED", audioEnabled)
        intent.putExtra("VIDEO_ENABLED", videoEnabled)
        intent.putExtra("VIDEO_BIT_RATE", videoBitRate)
        intent.putExtra("VIDEO_SIZE", videoSize)
        intent.putExtra("FPS", fps)
        intent.putExtra("DATA_CHANNEL_SIGNALING", dataChannelSignaling)
        intent.putExtra("IGNORE_DISCONNECT_WEBSOCKET", ignoreDisconnectWebSocket)

        startActivity(intent)
    }

    private fun selectedItem(spinner: MaterialSpinner): String {
        return spinner.getItems<String>()[spinner.selectedIndex]
    }

    private fun showInputError() {
        Snackbar.make(
            binding.rootLayout,
            "チャネル名を適切に入力してください",
            Snackbar.LENGTH_LONG
        )
            .setAction("OK") { }
            .show()
    }
}
