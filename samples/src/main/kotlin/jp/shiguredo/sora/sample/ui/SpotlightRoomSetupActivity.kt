package jp.shiguredo.sora.sample.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import jp.shiguredo.sora.sample.databinding.ActivitySpotlightRoomSetupBinding
import jp.shiguredo.sora.sample.option.SoraFrameSize

class SpotlightRoomSetupActivity : AppCompatActivity() {
    companion object {
        private val TAG = SpotlightRoomSetupActivity::class.simpleName
    }

    private val spotlightNumberOptions = listOf("未指定", "1", "2", "3", "4", "5", "6", "7", "8")
    private val videoCodecOptions = listOf("未指定", "VP8", "VP9", "H264", "H265", "AV1")
    private val audioCodecOptions = listOf("未指定", "OPUS")
    private val audioBitRateOptions =
        listOf(
            "未指定",
            "8",
            "16",
            "24",
            "32",
            "64",
            "96",
            "128",
            "256",
        )
    private val videoEnabledOptions = listOf("有効", "無効")
    private val audioEnabledOptions = listOf("有効", "無効")
    private val roleOptions = listOf("SENDRECV", "SENDONLY", "RECVONLY")
    private val spotlightFocusRidOptions = listOf("未指定", "none", "r0", "r1", "r2")
    private val spotlightUnfocusRidOptions = listOf("未指定", "none", "r0", "r1", "r2")
    private val simulcastEnabledOptions = listOf("有効", "無効")
    private val videoBitRateOptions =
        listOf(
            "500",
            "200",
            "700",
            "1200",
            "2500",
            "4000",
            "5000",
            "10000",
            "15000",
            "20000",
            "30000",
        )
    private val videoSizeOptions = SoraFrameSize.landscape.keys.toList()
    private val resolutionChangeOptions = listOf("未指定", "MAINTAIN_RESOLUTION", "MAINTAIN_FRAMERATE", "BALANCED", "DISABLED")
    private val resolutionAdjustmentOptions = listOf("未指定", "16", "8", "4", "2", "無効")
    private val fpsOptions = listOf("30", "10", "15", "20", "24", "60")
    private val clientIdOptions = listOf("なし", "端末情報", "時雨堂", "ランダム")
    private val bundleIdOptions = listOf("なし", "端末情報", "時雨堂", "ランダム")
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
        binding.spotlightNumberSelection.spinner.setDropdownItems(spotlightNumberOptions)
        binding.roleSelection.name.text = "ロール"
        binding.roleSelection.spinner.setDropdownItems(roleOptions)
        binding.spotlightFocusRidSelection.name.text = "フォーカス時の rid"
        binding.spotlightFocusRidSelection.spinner.setDropdownItems(spotlightFocusRidOptions)
        binding.spotlightUnfocusRidSelection.name.text = "非フォーカス時の rid"
        binding.spotlightUnfocusRidSelection.spinner.setDropdownItems(spotlightUnfocusRidOptions)
        binding.simulcastEnabledSelection.name.text = "サイマルキャスト"
        binding.simulcastEnabledSelection.spinner.setDropdownItems(simulcastEnabledOptions)
        binding.videoCodecSelection.name.text = "映像コーデック"
        binding.videoCodecSelection.spinner.setDropdownItems(videoCodecOptions)
        binding.videoEnabledSelection.name.text = "映像の有無"
        binding.videoEnabledSelection.spinner.setDropdownItems(videoEnabledOptions)
        binding.audioCodecSelection.name.text = "音声コーデック"
        binding.audioCodecSelection.spinner.setDropdownItems(audioCodecOptions)
        binding.audioEnabledSelection.name.text = "音声の有無"
        binding.audioEnabledSelection.spinner.setDropdownItems(audioEnabledOptions)
        binding.audioBitRateSelection.name.text = "音声ビットレート"
        binding.audioBitRateSelection.spinner.setDropdownItems(audioBitRateOptions)
        binding.videoBitRateSelection.name.text = "映像ビットレート"
        binding.videoBitRateSelection.spinner.setDropdownItems(videoBitRateOptions)
        binding.videoSizeSelection.name.text = "映像サイズ"
        binding.videoSizeSelection.spinner.setDropdownItems(videoSizeOptions, defaultIndex = 3)
        binding.resolutionChangeSelection.name.text = "解像度の変更"
        binding.resolutionChangeSelection.spinner.setDropdownItems(resolutionChangeOptions)
        binding.resolutionAdjustmentSelection.name.text = "解像度の調整"
        binding.resolutionAdjustmentSelection.spinner.setDropdownItems(resolutionAdjustmentOptions)
        binding.fpsSelection.name.text = "フレームレート"
        binding.fpsSelection.spinner.setDropdownItems(fpsOptions)
        binding.clientIdSelection.name.text = "クライアント ID"
        binding.clientIdSelection.spinner.setDropdownItems(clientIdOptions)
        binding.bundleIdSelection.name.text = "バンドル ID"
        binding.bundleIdSelection.spinner.setDropdownItems(bundleIdOptions)
        binding.dataChannelSignalingSelection.name.text = "データチャネル"
        binding.dataChannelSignalingSelection.spinner.setDropdownItems(dataChannelSignalingOptions)
        binding.ignoreDisconnectWebSocketSelection.name.text = "WS 切断を無視"
        binding.ignoreDisconnectWebSocketSelection.spinner.setDropdownItems(ignoreDisconnectWebSocketOptions)
    }

    private fun startSpotlightChat() {
        val channelName = binding.channelNameInput!!.text.toString()
        if (channelName.isEmpty()) {
            showInputError()
            return
        }

        val spotlightNumber = binding.spotlightNumberSelection.spinner.selectedItem()
        val role = binding.roleSelection.spinner.selectedItem()
        var spotlightFocusRid = binding.spotlightFocusRidSelection.spinner.selectedItem()
        var spotlightUnfocusRid = binding.spotlightUnfocusRidSelection.spinner.selectedItem()
        val simulcastEnabled = binding.simulcastEnabledSelection.spinner.selectedItem()
        val videoCodec = binding.videoCodecSelection.spinner.selectedItem()
        val audioCodec = binding.audioCodecSelection.spinner.selectedItem()
        val audioBitRate = binding.audioBitRateSelection.spinner.selectedItem()
        val audioEnabled = binding.audioEnabledSelection.spinner.selectedItem()
        val videoEnabled = binding.videoEnabledSelection.spinner.selectedItem()
        val videoBitRate = binding.videoBitRateSelection.spinner.selectedItem()
        val videoSize = binding.videoSizeSelection.spinner.selectedItem()
        val resolutionChange = binding.resolutionChangeSelection.spinner.selectedItem()
        val resolutionAdjusment = binding.resolutionAdjustmentSelection.spinner.selectedItem()
        val fps = binding.fpsSelection.spinner.selectedItem()
        val clientId = binding.clientIdSelection.spinner.selectedItem()
        val bundleId = binding.bundleIdSelection.spinner.selectedItem()
        val dataChannelSignaling = binding.dataChannelSignalingSelection.spinner.selectedItem()
        val ignoreDisconnectWebSocket = binding.ignoreDisconnectWebSocketSelection.spinner.selectedItem()

        val intent = Intent(this, SimulcastActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channelName)
        intent.putExtra("SPOTLIGHT", "有効")
        intent.putExtra("SPOTLIGHT_NUMBER", spotlightNumber)
        intent.putExtra("SPOTLIGHT_FOCUS_RID", spotlightFocusRid)
        intent.putExtra("SPOTLIGHT_UNFOCUS_RID", spotlightUnfocusRid)
        intent.putExtra("SIMULCAST_ENABLED", simulcastEnabled)
        intent.putExtra("ROLE", role)
        intent.putExtra("VIDEO_CODEC", videoCodec)
        intent.putExtra("AUDIO_CODEC", audioCodec)
        intent.putExtra("AUDIO_BIT_RATE", audioBitRate)
        intent.putExtra("AUDIO_ENABLED", audioEnabled)
        intent.putExtra("VIDEO_ENABLED", videoEnabled)
        intent.putExtra("VIDEO_BIT_RATE", videoBitRate)
        intent.putExtra("VIDEO_SIZE", videoSize)
        intent.putExtra("RESOLUTION_CHANGE", resolutionChange)
        intent.putExtra("RESOLUTION_ADJUSTMENT", resolutionAdjusment)
        intent.putExtra("FPS", fps)
        intent.putExtra("CLIENT_ID", clientId)
        intent.putExtra("BUNDLE_ID", bundleId)
        intent.putExtra("DATA_CHANNEL_SIGNALING", dataChannelSignaling)
        intent.putExtra("IGNORE_DISCONNECT_WEBSOCKET", ignoreDisconnectWebSocket)

        startActivity(intent)
    }

    private fun showInputError() {
        Snackbar
            .make(
                binding.rootLayout,
                "チャネル名を適切に入力してください",
                Snackbar.LENGTH_LONG,
            ).setAction("OK") { }
            .show()
    }
}
