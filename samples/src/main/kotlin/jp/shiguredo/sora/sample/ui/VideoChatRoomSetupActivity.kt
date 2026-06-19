package jp.shiguredo.sora.sample.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import jp.shiguredo.sora.sample.databinding.ActivityVideoChatRoomSetupBinding
import jp.shiguredo.sora.sample.option.SoraFrameSize

class VideoChatRoomSetupActivity : AppCompatActivity() {
    companion object {
        val TAG = VideoChatRoomSetupActivity::class.simpleName
    }

    private val videoCodecOptions = listOf("未指定", "VP8", "VP9", "H264", "H265", "AV1")
    private val videoEnabledOptions = listOf("有効", "無効")
    private val videoSourceOptions = listOf("カメラ", "ダミー映像")
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
    private val videoBitRateOptions =
        listOf(
            "未指定",
            "100",
            "300",
            "500",
            "800",
            "1000",
            "1500",
            "2000",
            "2500",
            "3000",
            "5000",
            "10000",
            "15000",
            "20000",
            "30000",
        )
    private val videoSizeOptions = SoraFrameSize.all.keys.toList()
    private val vp9ProfileIdOptions = listOf("未指定", "0", "1", "2", "3")
    private val av1ProfileOptions = listOf("未指定", "0", "1", "2")
    private val h264ProfileLevelIdOptions = listOf("未指定", "42e01f", "42e020", "42e034")
    private val h265ParamsEnabledOptions = listOf("無効", "有効")
    private val h265ProfileIdOptions = listOf("1 (Main)")
    private val h265LevelIdOptions = listOf("90", "120", "150")
    private val h265TierFlagOptions = listOf("0", "1")
    private val h265TxModeOptions = listOf("SRST", "MRST", "MRMT")
    private val fpsOptions = listOf("30", "10", "15", "20", "24", "60")
    private val resolutionChangeOptions = listOf("未指定", "MAINTAIN_RESOLUTION", "MAINTAIN_FRAMERATE", "BALANCED", "DISABLED")
    private val resolutionAdjustmentOptions = listOf("未指定", "16", "8", "4", "2", "無効")
    private val cameraFacingOptions = listOf("前面", "背面")
    private val clientIdOptions = listOf("なし", "端末情報", "時雨堂", "ランダム")
    private val bundleIdOptions = listOf("なし", "端末情報", "時雨堂", "ランダム")
    private val dataChannelSignalingOptions = listOf("未指定", "無効", "有効")
    private val ignoreDisconnectWebSocketOptions = listOf("未指定", "無効", "有効")
    private val audioStreamingLanguageCodeOptions = listOf("未指定", "ja-JP", "en-US")
    private val initialCameraOptions = listOf("有効", "無効")

    private lateinit var binding: ActivityVideoChatRoomSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivityVideoChatRoomSetupBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.start.setOnClickListener { startVideoChat() }

        binding.videoEnabledSelection.name.text = "映像の有無"
        binding.videoCodecSelection.name.text = "映像コーデック"
        binding.audioEnabledSelection.name.text = "音声の有無"
        binding.audioCodecSelection.name.text = "音声コーデック"
        binding.audioBitRateSelection.name.text = "音声ビットレート"
        binding.audioStereoSelection.name.text = "ステレオ音声"
        binding.roleSelection.name.text = "ロール"
        binding.videoBitRateSelection.name.text = "映像ビットレート"
        binding.videoSizeSelection.name.text = "映像サイズ"
        binding.vp9ProfileIdSelection.name.text = "VP9 プロファイル"
        binding.av1ProfileSelection.name.text = "AV1 プロファイル"
        binding.h264ProfileLevelIdSelection.name.text = "H264 プロファイル"
        binding.h265ParamsEnabledSelection.name.text = "H265 プロファイル設定"
        binding.h265ProfileIdSelection.name.text = "H265 profile_id"
        binding.h265LevelIdSelection.name.text = "H265 level_id"
        binding.h265TierFlagSelection.name.text = "H265 tier_flag"
        binding.h265TxModeSelection.name.text = "H265 tx_mode"
        binding.fpsSelection.name.text = "フレームレート"
        binding.resolutionChangeSelection.name.text = "解像度の変更"
        binding.resolutionAdjustmentSelection.name.text = "解像度の調整"
        binding.cameraFacingSelection.name.text = "カメラ"
        binding.clientIdSelection.name.text = "クライアント ID"
        binding.bundleIdSelection.name.text = "バンドル ID"
        binding.dataChannelSignalingSelection.name.text = "データチャネル"
        binding.ignoreDisconnectWebSocketSelection.name.text = "WS 切断を無視"
        binding.audioStreamingLanguageCodeSelection.name.text = "文字変換言語コード"
        binding.initialCameraSelection.name.text = "開始時カメラ"
        binding.videoSourceSelection.name.text = "映像ソース"

        setupDropdowns(
            listOf(
                DropdownConfig(binding.videoEnabledSelection.spinner, videoEnabledOptions),
                DropdownConfig(binding.videoCodecSelection.spinner, videoCodecOptions),
                DropdownConfig(binding.videoSourceSelection.spinner, videoSourceOptions),
                DropdownConfig(binding.audioEnabledSelection.spinner, audioEnabledOptions),
                DropdownConfig(binding.audioCodecSelection.spinner, audioCodecOptions),
                DropdownConfig(binding.audioBitRateSelection.spinner, audioBitRateOptions),
                DropdownConfig(binding.audioStereoSelection.spinner, audioStereoOptions),
                DropdownConfig(binding.roleSelection.spinner, roleOptions),
                DropdownConfig(binding.videoBitRateSelection.spinner, videoBitRateOptions),
                DropdownConfig(binding.videoSizeSelection.spinner, videoSizeOptions, defaultIndex = 3),
                DropdownConfig(binding.vp9ProfileIdSelection.spinner, vp9ProfileIdOptions),
                DropdownConfig(binding.av1ProfileSelection.spinner, av1ProfileOptions),
                DropdownConfig(binding.h264ProfileLevelIdSelection.spinner, h264ProfileLevelIdOptions),
                DropdownConfig(binding.h265ParamsEnabledSelection.spinner, h265ParamsEnabledOptions),
                DropdownConfig(binding.h265ProfileIdSelection.spinner, h265ProfileIdOptions),
                DropdownConfig(binding.h265LevelIdSelection.spinner, h265LevelIdOptions),
                DropdownConfig(binding.h265TierFlagSelection.spinner, h265TierFlagOptions),
                DropdownConfig(binding.h265TxModeSelection.spinner, h265TxModeOptions),
                DropdownConfig(binding.fpsSelection.spinner, fpsOptions),
                DropdownConfig(binding.resolutionChangeSelection.spinner, resolutionChangeOptions),
                DropdownConfig(binding.resolutionAdjustmentSelection.spinner, resolutionAdjustmentOptions),
                DropdownConfig(binding.cameraFacingSelection.spinner, cameraFacingOptions),
                DropdownConfig(binding.clientIdSelection.spinner, clientIdOptions),
                DropdownConfig(binding.bundleIdSelection.spinner, bundleIdOptions),
                DropdownConfig(binding.dataChannelSignalingSelection.spinner, dataChannelSignalingOptions),
                DropdownConfig(binding.ignoreDisconnectWebSocketSelection.spinner, ignoreDisconnectWebSocketOptions),
                DropdownConfig(binding.audioStreamingLanguageCodeSelection.spinner, audioStreamingLanguageCodeOptions),
                DropdownConfig(binding.initialCameraSelection.spinner, initialCameraOptions),
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
        val vp9ProfileId = binding.vp9ProfileIdSelection.spinner.selectedItem()
        val av1Profile = binding.av1ProfileSelection.spinner.selectedItem()
        val h264ProfileLevelId = binding.h264ProfileLevelIdSelection.spinner.selectedItem()
        val h265ParamsEnabled = binding.h265ParamsEnabledSelection.spinner.selectedItem()
        val h265ProfileId = binding.h265ProfileIdSelection.spinner.selectedItem()
        val h265LevelId = binding.h265LevelIdSelection.spinner.selectedItem()
        val h265TierFlag = binding.h265TierFlagSelection.spinner.selectedItem()
        val h265TxMode = binding.h265TxModeSelection.spinner.selectedItem()
        val fps = binding.fpsSelection.spinner.selectedItem()
        val resolutionChange = binding.resolutionChangeSelection.spinner.selectedItem()
        val resolutionAdjustment = binding.resolutionAdjustmentSelection.spinner.selectedItem()
        val cameraFacing = binding.cameraFacingSelection.spinner.selectedItem()
        val clientId = binding.clientIdSelection.spinner.selectedItem()
        val bundleId = binding.bundleIdSelection.spinner.selectedItem()
        val dataChannelSignaling = binding.dataChannelSignalingSelection.spinner.selectedItem()
        val ignoreDisconnectWebSocket = binding.ignoreDisconnectWebSocketSelection.spinner.selectedItem()
        val audioStreamingLanguageCode = binding.audioStreamingLanguageCodeSelection.spinner.selectedItem()
        val initialCamera = binding.initialCameraSelection.spinner.selectedItem()

        val intent = Intent(this, VideoChatRoomActivity::class.java)
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
        if (videoCodec == "VP9") {
            intent.putExtra("VP9_PROFILE_ID", vp9ProfileId)
        }
        if (videoCodec == "AV1") {
            intent.putExtra("AV1_PROFILE", av1Profile)
        }
        if (videoCodec == "H264") {
            intent.putExtra("H264_PROFILE_LEVEL_ID", h264ProfileLevelId)
        }
        if (videoCodec == "H265") {
            intent.putExtra("H265_PARAMS_ENABLED", h265ParamsEnabled)
            intent.putExtra("H265_PROFILE_ID", h265ProfileId.substringBefore(" "))
            intent.putExtra("H265_LEVEL_ID", h265LevelId)
            intent.putExtra("H265_TIER_FLAG", h265TierFlag)
            intent.putExtra("H265_TX_MODE", h265TxMode)
        }
        intent.putExtra("FPS", fps)
        intent.putExtra("RESOLUTION_CHANGE", resolutionChange)
        intent.putExtra("RESOLUTION_ADJUSTMENT", resolutionAdjustment)
        intent.putExtra("CAMERA_FACING", cameraFacing)
        intent.putExtra("CLIENT_ID", clientId)
        intent.putExtra("BUNDLE_ID", bundleId)
        intent.putExtra("DATA_CHANNEL_SIGNALING", dataChannelSignaling)
        intent.putExtra("IGNORE_DISCONNECT_WEBSOCKET", ignoreDisconnectWebSocket)
        intent.putExtra("AUDIO_STREAMING_LANGUAGE_CODE", audioStreamingLanguageCode)
        intent.putExtra("INITIAL_CAMERA", initialCamera)
        intent.putExtra("VIDEO_SOURCE", videoSource)

        startActivity(intent)
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

    private fun updateH265ParamsGroupVisibility() {
        binding.h265ParamsGroup.visibility =
            if (binding.h265ParamsEnabledSelection.spinner.selectedItem() == "有効") {
                View.VISIBLE
            } else {
                View.GONE
            }
    }
}
