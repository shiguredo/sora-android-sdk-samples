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
    private val videoSourceOptions = listOf("カメラ", "ダミー映像")
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
    private val h265ParamsEnabledOptions = listOf("無効", "有効")
    private val h265ProfileIdOptions = listOf("1 (Main)")
    private val h265LevelIdOptions = listOf("90", "120", "150")
    private val h265TierFlagOptions = listOf("0", "1")
    private val h265TxModeOptions = listOf("SRST", "MRST", "MRMT")

    private lateinit var binding: ActivitySpotlightRoomSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivitySpotlightRoomSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.start.setOnClickListener { startSpotlightChat() }

        binding.spotlightNumberSelection.name.text = "スポットライト数"
        binding.roleSelection.name.text = "ロール"
        binding.spotlightFocusRidSelection.name.text = "フォーカス時の rid"
        binding.spotlightUnfocusRidSelection.name.text = "非フォーカス時の rid"
        binding.simulcastEnabledSelection.name.text = "サイマルキャスト"
        binding.videoCodecSelection.name.text = "映像コーデック"
        binding.videoEnabledSelection.name.text = "映像の有無"
        binding.videoSourceSelection.name.text = "映像ソース"
        binding.audioCodecSelection.name.text = "音声コーデック"
        binding.audioEnabledSelection.name.text = "音声の有無"
        binding.audioBitRateSelection.name.text = "音声ビットレート"
        binding.videoBitRateSelection.name.text = "映像ビットレート"
        binding.videoSizeSelection.name.text = "映像サイズ"
        binding.resolutionChangeSelection.name.text = "解像度の変更"
        binding.resolutionAdjustmentSelection.name.text = "解像度の調整"
        binding.fpsSelection.name.text = "フレームレート"
        binding.clientIdSelection.name.text = "クライアント ID"
        binding.bundleIdSelection.name.text = "バンドル ID"
        binding.dataChannelSignalingSelection.name.text = "データチャネル"
        binding.ignoreDisconnectWebSocketSelection.name.text = "WS 切断を無視"
        binding.h265ParamsEnabledSelection.name.text = "H265 プロファイル設定"
        binding.h265ProfileIdSelection.name.text = "H265 profile_id"
        binding.h265LevelIdSelection.name.text = "H265 level_id"
        binding.h265TierFlagSelection.name.text = "H265 tier_flag"
        binding.h265TxModeSelection.name.text = "H265 tx_mode"
        setupDropdowns(
            listOf(
                DropdownConfig(binding.spotlightNumberSelection.spinner, spotlightNumberOptions),
                DropdownConfig(binding.roleSelection.spinner, roleOptions),
                DropdownConfig(binding.spotlightFocusRidSelection.spinner, spotlightFocusRidOptions),
                DropdownConfig(binding.spotlightUnfocusRidSelection.spinner, spotlightUnfocusRidOptions),
                DropdownConfig(binding.simulcastEnabledSelection.spinner, simulcastEnabledOptions),
                DropdownConfig(binding.videoCodecSelection.spinner, videoCodecOptions),
                DropdownConfig(binding.videoEnabledSelection.spinner, videoEnabledOptions),
                DropdownConfig(binding.videoSourceSelection.spinner, videoSourceOptions),
                DropdownConfig(binding.audioCodecSelection.spinner, audioCodecOptions),
                DropdownConfig(binding.audioEnabledSelection.spinner, audioEnabledOptions),
                DropdownConfig(binding.audioBitRateSelection.spinner, audioBitRateOptions),
                DropdownConfig(binding.videoBitRateSelection.spinner, videoBitRateOptions),
                DropdownConfig(binding.videoSizeSelection.spinner, videoSizeOptions, defaultIndex = 3),
                DropdownConfig(binding.resolutionChangeSelection.spinner, resolutionChangeOptions),
                DropdownConfig(binding.resolutionAdjustmentSelection.spinner, resolutionAdjustmentOptions),
                DropdownConfig(binding.fpsSelection.spinner, fpsOptions),
                DropdownConfig(binding.clientIdSelection.spinner, clientIdOptions),
                DropdownConfig(binding.bundleIdSelection.spinner, bundleIdOptions),
                DropdownConfig(binding.dataChannelSignalingSelection.spinner, dataChannelSignalingOptions),
                DropdownConfig(binding.ignoreDisconnectWebSocketSelection.spinner, ignoreDisconnectWebSocketOptions),
                DropdownConfig(binding.h265ParamsEnabledSelection.spinner, h265ParamsEnabledOptions),
                DropdownConfig(binding.h265ProfileIdSelection.spinner, h265ProfileIdOptions),
                DropdownConfig(binding.h265LevelIdSelection.spinner, h265LevelIdOptions),
                DropdownConfig(binding.h265TierFlagSelection.spinner, h265TierFlagOptions),
                DropdownConfig(binding.h265TxModeSelection.spinner, h265TxModeOptions),
            ),
        )

        binding.h265ParamsEnabledSelection.spinner.setOnItemClickListener { _, _, _, _ ->
            updateH265ParamsGroupVisibility()
        }
        updateH265ParamsGroupVisibility()
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
        val videoSource = binding.videoSourceSelection.spinner.selectedItem()
        val videoBitRate = binding.videoBitRateSelection.spinner.selectedItem()
        val videoSize = binding.videoSizeSelection.spinner.selectedItem()
        val resolutionChange = binding.resolutionChangeSelection.spinner.selectedItem()
        val resolutionAdjusment = binding.resolutionAdjustmentSelection.spinner.selectedItem()
        val fps = binding.fpsSelection.spinner.selectedItem()
        val clientId = binding.clientIdSelection.spinner.selectedItem()
        val bundleId = binding.bundleIdSelection.spinner.selectedItem()
        val dataChannelSignaling = binding.dataChannelSignalingSelection.spinner.selectedItem()
        val ignoreDisconnectWebSocket = binding.ignoreDisconnectWebSocketSelection.spinner.selectedItem()
        val h265ParamsEnabled = binding.h265ParamsEnabledSelection.spinner.selectedItem()
        val h265ProfileId = binding.h265ProfileIdSelection.spinner.selectedItem()
        val h265LevelId = binding.h265LevelIdSelection.spinner.selectedItem()
        val h265TierFlag = binding.h265TierFlagSelection.spinner.selectedItem()
        val h265TxMode = binding.h265TxModeSelection.spinner.selectedItem()

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
        intent.putExtra("VIDEO_SOURCE", videoSource)
        intent.putExtra("VIDEO_BIT_RATE", videoBitRate)
        intent.putExtra("VIDEO_SIZE", videoSize)
        intent.putExtra("RESOLUTION_CHANGE", resolutionChange)
        intent.putExtra("RESOLUTION_ADJUSTMENT", resolutionAdjusment)
        intent.putExtra("FPS", fps)
        intent.putExtra("CLIENT_ID", clientId)
        intent.putExtra("BUNDLE_ID", bundleId)
        intent.putExtra("DATA_CHANNEL_SIGNALING", dataChannelSignaling)
        intent.putExtra("IGNORE_DISCONNECT_WEBSOCKET", ignoreDisconnectWebSocket)
        if (videoCodec == "H265") {
            intent.putExtra("H265_PARAMS_ENABLED", h265ParamsEnabled)
            intent.putExtra("H265_PROFILE_ID", h265ProfileId.substringBefore(" "))
            intent.putExtra("H265_LEVEL_ID", h265LevelId)
            intent.putExtra("H265_TIER_FLAG", h265TierFlag)
            intent.putExtra("H265_TX_MODE", h265TxMode)
        }

        startActivity(intent)
    }

    private fun updateH265ParamsGroupVisibility() {
        binding.h265ParamsGroup.visibility =
            if (binding.h265ParamsEnabledSelection.spinner.selectedItem() == "有効") {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
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
