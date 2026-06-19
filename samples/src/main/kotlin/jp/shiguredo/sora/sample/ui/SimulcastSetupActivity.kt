package jp.shiguredo.sora.sample.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import jp.shiguredo.sora.sample.databinding.ActivitySimulcastSetupBinding
import jp.shiguredo.sora.sample.option.SoraFrameSize

class SimulcastSetupActivity : AppCompatActivity() {
    companion object {
        val TAG = SimulcastSetupActivity::class.simpleName
    }

    private val videoCodecOptions = listOf("未指定", "VP8", "VP9", "H264", "H265", "AV1")
    private val videoEnabledOptions = listOf("有効", "無効")
    private val videoSourceOptions = listOf("カメラ", "ダミー映像")
    private val initialCameraOptions = listOf("有効", "無効")
    private val audioCodecOptions = listOf("未指定", "OPUS")
    private val audioEnabledOptions = listOf("有効", "無効")
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
    private val audioStereoOptions = listOf("モノラル", "ステレオ")
    private val roleOptions = listOf("SENDRECV", "SENDONLY", "RECVONLY")
    private val videoBitRateOptions = listOf("200", "500", "700", "1200", "2500", "4000", "5000", "10000", "15000", "20000", "30000")

    private val videoSizeOptions = SoraFrameSize.landscape.keys.toList()
    private val fpsOptions = listOf("30", "10", "15", "20", "24", "60")
    private val resolutionChangeOptions = listOf("未指定", "MAINTAIN_RESOLUTION", "MAINTAIN_FRAMERATE", "BALANCED", "DISABLED")
    private val resolutionAdjustmentOptions = listOf("未指定", "16", "8", "4", "2", "無効")
    private val simulcastRequestRidOptions = listOf("未指定", "none", "r0", "r1", "r2")
    private val clientIdOptions = listOf("なし", "端末情報", "時雨堂", "ランダム")
    private val bundleIdOptions = listOf("なし", "端末情報", "時雨堂", "ランダム")
    private val dataChannelSignalingOptions = listOf("未指定", "無効", "有効")
    private val ignoreDisconnectWebSocketOptions = listOf("未指定", "無効", "有効")
    private val h265ParamsEnabledOptions = listOf("無効", "有効")
    private val h265ProfileIdOptions = listOf("1 (Main)")
    private val h265LevelIdOptions = listOf("90", "120", "150")
    private val h265TierFlagOptions = listOf("0", "1")
    private val h265TxModeOptions = listOf("SRST", "MRST", "MRMT")

    private lateinit var binding: ActivitySimulcastSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivitySimulcastSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.start.setOnClickListener { startVideoChat() }

        binding.videoEnabledSelection.name.text = "映像の有無"
        binding.initialCameraSelection.name.text = "開始時カメラ"
        binding.videoCodecSelection.name.text = "映像コーデック"
        binding.videoSourceSelection.name.text = "映像ソース"
        binding.audioEnabledSelection.name.text = "音声の有無"
        binding.audioCodecSelection.name.text = "音声コーデック"
        binding.audioBitRateSelection.name.text = "音声ビットレート"
        binding.audioStereoSelection.name.text = "ステレオ音声"
        binding.roleSelection.name.text = "ロール"
        binding.videoBitRateSelection.name.text = "映像ビットレート"
        binding.videoSizeSelection.name.text = "映像サイズ"
        binding.fpsSelection.name.text = "フレームレート"
        binding.resolutionChangeSelection.name.text = "解像度の変更"
        binding.resolutionAdjustmentSelection.name.text = "解像度の調整"
        binding.simulcastRequestRidSelection.name.text = "受信する rid"
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
                DropdownConfig(binding.videoEnabledSelection.spinner, videoEnabledOptions),
                DropdownConfig(binding.videoSourceSelection.spinner, videoSourceOptions),
                DropdownConfig(binding.initialCameraSelection.spinner, initialCameraOptions),
                DropdownConfig(binding.videoCodecSelection.spinner, videoCodecOptions),
                DropdownConfig(binding.audioEnabledSelection.spinner, audioEnabledOptions),
                DropdownConfig(binding.audioCodecSelection.spinner, audioCodecOptions),
                DropdownConfig(binding.audioBitRateSelection.spinner, audioBitRateOptions),
                DropdownConfig(binding.audioStereoSelection.spinner, audioStereoOptions),
                DropdownConfig(binding.roleSelection.spinner, roleOptions),
                DropdownConfig(binding.videoBitRateSelection.spinner, videoBitRateOptions, defaultIndex = 6),
                DropdownConfig(binding.videoSizeSelection.spinner, videoSizeOptions, defaultIndex = 5),
                DropdownConfig(binding.fpsSelection.spinner, fpsOptions),
                DropdownConfig(binding.resolutionChangeSelection.spinner, resolutionChangeOptions),
                DropdownConfig(binding.resolutionAdjustmentSelection.spinner, resolutionAdjustmentOptions),
                DropdownConfig(binding.simulcastRequestRidSelection.spinner, simulcastRequestRidOptions),
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

    private fun startVideoChat() {
        val channelName = binding.channelNameInput.text.toString()
        if (channelName.isEmpty()) {
            showInputError()
            return
        }

        val role = binding.roleSelection.spinner.selectedItem()
        val videoCodec = binding.videoCodecSelection.spinner.selectedItem()
        val videoEnabled = binding.videoEnabledSelection.spinner.selectedItem()
        val videoSource = binding.videoSourceSelection.spinner.selectedItem()
        val audioCodec = binding.audioCodecSelection.spinner.selectedItem()
        val audioEnabled = binding.audioEnabledSelection.spinner.selectedItem()
        val audioBitRate = binding.audioBitRateSelection.spinner.selectedItem()
        val audioStereo = binding.audioStereoSelection.spinner.selectedItem()
        val videoBitRate = binding.videoBitRateSelection.spinner.selectedItem()
        val videoSize = binding.videoSizeSelection.spinner.selectedItem()
        val fps = binding.fpsSelection.spinner.selectedItem()
        val resolutionChange = binding.resolutionChangeSelection.spinner.selectedItem()
        val resolutionAdjustment = binding.resolutionAdjustmentSelection.spinner.selectedItem()
        val simulcastRequestRid = binding.simulcastRequestRidSelection.spinner.selectedItem()
        val clientId = binding.clientIdSelection.spinner.selectedItem()
        val bundleId = binding.bundleIdSelection.spinner.selectedItem()
        val dataChannelSignaling = binding.dataChannelSignalingSelection.spinner.selectedItem()
        val ignoreDisconnectWebSocket = binding.ignoreDisconnectWebSocketSelection.spinner.selectedItem()
        val initialCamera = binding.initialCameraSelection.spinner.selectedItem()
        val h265ParamsEnabled = binding.h265ParamsEnabledSelection.spinner.selectedItem()
        val h265ProfileId = binding.h265ProfileIdSelection.spinner.selectedItem()
        val h265LevelId = binding.h265LevelIdSelection.spinner.selectedItem()
        val h265TierFlag = binding.h265TierFlagSelection.spinner.selectedItem()
        val h265TxMode = binding.h265TxModeSelection.spinner.selectedItem()

        val intent = Intent(this, SimulcastActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channelName)
        intent.putExtra("ROLE", role)
        intent.putExtra("VIDEO_CODEC", videoCodec)
        intent.putExtra("VIDEO_ENABLED", videoEnabled)
        intent.putExtra("AUDIO_CODEC", audioCodec)
        intent.putExtra("AUDIO_ENABLED", audioEnabled)
        intent.putExtra("AUDIO_BIT_RATE", audioBitRate)
        intent.putExtra("AUDIO_STEREO", audioStereo)
        intent.putExtra("VIDEO_BIT_RATE", videoBitRate)
        intent.putExtra("VIDEO_SIZE", videoSize)
        intent.putExtra("SIMULCAST", true)
        intent.putExtra("FPS", fps)
        intent.putExtra("RESOLUTION_CHANGE", resolutionChange)
        intent.putExtra("RESOLUTION_ADJUSTMENT", resolutionAdjustment)
        intent.putExtra("SIMULCAST_REQUEST_RID", simulcastRequestRid)
        intent.putExtra("CLIENT_ID", clientId)
        intent.putExtra("BUNDLE_ID", bundleId)
        intent.putExtra("DATA_CHANNEL_SIGNALING", dataChannelSignaling)
        intent.putExtra("IGNORE_DISCONNECT_WEBSOCKET", ignoreDisconnectWebSocket)
        intent.putExtra("INITIAL_CAMERA", initialCamera)
        intent.putExtra("VIDEO_SOURCE", videoSource)
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
                "チャネル名 を適切に入力してください",
                Snackbar.LENGTH_LONG,
            ).setAction("OK") { }
            .show()
    }
}
