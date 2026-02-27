package jp.shiguredo.sora.sample.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import jp.shiguredo.sora.sample.databinding.ActivityRpcChatSetupBinding
import jp.shiguredo.sora.sample.option.SoraFrameSize

class RpcChatSetupActivity : AppCompatActivity() {
    companion object {
        private val TAG = RpcChatSetupActivity::class.simpleName
    }

    // Simulcast settings
    private val simulcastRequestRidOptions = listOf("未指定", "none", "r0", "r1", "r2")

    // Spotlight settings
    private val spotlightEnabledOptions = listOf("有効", "無効")
    private val spotlightNumberOptions = listOf("未指定", "1", "2", "3", "4", "5", "6", "7", "8")
    private val spotlightFocusRidOptions = listOf("未指定", "none", "r0", "r1", "r2")
    private val spotlightUnfocusRidOptions = listOf("未指定", "none", "r0", "r1", "r2")

    // Common settings
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
    private val videoBitRateOptions =
        listOf(
            "200",
            "500",
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
    private val fpsOptions = listOf("30", "10", "15", "20", "24", "60")
    private val resolutionChangeOptions = listOf("未指定", "MAINTAIN_RESOLUTION", "MAINTAIN_FRAMERATE", "BALANCED", "DISABLED")
    private val resolutionAdjustmentOptions = listOf("未指定", "16", "8", "4", "2", "無効")
    private val clientIdOptions = listOf("なし", "端末情報", "時雨堂", "ランダム")
    private val bundleIdOptions = listOf("なし", "端末情報", "時雨堂", "ランダム")
    private val dataChannelSignalingOptions = listOf("未指定", "無効", "有効")
    private val ignoreDisconnectWebSocketOptions = listOf("未指定", "無効", "有効")
    private val initialCameraOptions = listOf("有効", "無効")

    private lateinit var binding: ActivityRpcChatSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivityRpcChatSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.start.setOnClickListener { startRpcChat() }

        // Simulcast settings
        binding.simulcastRequestRidSelection.name.text = "受信する rid (Simulcast)"

        // Spotlight settings
        binding.spotlightEnabledSelection.name.text = "スポットライト機能"
        binding.spotlightNumberSelection.name.text = "スポットライト数"
        binding.spotlightFocusRidSelection.name.text = "フォーカス時の rid"
        binding.spotlightUnfocusRidSelection.name.text = "非フォーカス時の rid"

        // Common settings
        binding.roleSelection.name.text = "ロール"
        binding.videoCodecSelection.name.text = "映像コーデック"
        binding.videoEnabledSelection.name.text = "映像の有無"
        binding.audioCodecSelection.name.text = "音声コーデック"
        binding.audioEnabledSelection.name.text = "音声の有無"
        binding.audioBitRateSelection.name.text = "音声ビットレート"
        binding.videoBitRateSelection.name.text = "映像ビットレート"
        binding.videoSizeSelection.name.text = "映像サイズ"
        binding.fpsSelection.name.text = "フレームレート"
        binding.resolutionChangeSelection.name.text = "解像度の変更"
        binding.resolutionAdjustmentSelection.name.text = "解像度の調整"
        binding.clientIdSelection.name.text = "クライアント ID"
        binding.bundleIdSelection.name.text = "バンドル ID"
        binding.dataChannelSignalingSelection.name.text = "データチャネル (シグナリング)"
        binding.ignoreDisconnectWebSocketSelection.name.text = "WS 切断を無視"
        binding.initialCameraSelection.name.text = "開始時カメラ"

        setupDropdowns(
            listOf(
                DropdownConfig(binding.simulcastRequestRidSelection.spinner, simulcastRequestRidOptions),
                DropdownConfig(binding.spotlightEnabledSelection.spinner, spotlightEnabledOptions),
                DropdownConfig(binding.spotlightNumberSelection.spinner, spotlightNumberOptions),
                DropdownConfig(binding.spotlightFocusRidSelection.spinner, spotlightFocusRidOptions),
                DropdownConfig(binding.spotlightUnfocusRidSelection.spinner, spotlightUnfocusRidOptions),
                DropdownConfig(binding.roleSelection.spinner, roleOptions),
                DropdownConfig(binding.videoCodecSelection.spinner, videoCodecOptions),
                DropdownConfig(binding.videoEnabledSelection.spinner, videoEnabledOptions),
                DropdownConfig(binding.audioCodecSelection.spinner, audioCodecOptions),
                DropdownConfig(binding.audioEnabledSelection.spinner, audioEnabledOptions),
                DropdownConfig(binding.audioBitRateSelection.spinner, audioBitRateOptions),
                DropdownConfig(binding.videoBitRateSelection.spinner, videoBitRateOptions, defaultIndex = 1),
                DropdownConfig(binding.videoSizeSelection.spinner, videoSizeOptions, defaultIndex = 5),
                DropdownConfig(binding.fpsSelection.spinner, fpsOptions),
                DropdownConfig(binding.resolutionChangeSelection.spinner, resolutionChangeOptions),
                DropdownConfig(binding.resolutionAdjustmentSelection.spinner, resolutionAdjustmentOptions),
                DropdownConfig(binding.clientIdSelection.spinner, clientIdOptions),
                DropdownConfig(binding.bundleIdSelection.spinner, bundleIdOptions),
                DropdownConfig(binding.dataChannelSignalingSelection.spinner, dataChannelSignalingOptions, defaultIndex = 2),
                DropdownConfig(binding.ignoreDisconnectWebSocketSelection.spinner, ignoreDisconnectWebSocketOptions),
                DropdownConfig(binding.initialCameraSelection.spinner, initialCameraOptions),
            ),
        )
    }

    private fun startRpcChat() {
        val channelName = binding.channelNameInput.text.toString()
        if (channelName.isEmpty()) {
            showInputError()
            return
        }

        val role = binding.roleSelection.spinner.selectedItem()
        val videoCodec = binding.videoCodecSelection.spinner.selectedItem()
        val videoEnabled = binding.videoEnabledSelection.spinner.selectedItem()
        val audioCodec = binding.audioCodecSelection.spinner.selectedItem()
        val audioEnabled = binding.audioEnabledSelection.spinner.selectedItem()
        val audioBitRate = binding.audioBitRateSelection.spinner.selectedItem()
        val videoBitRate = binding.videoBitRateSelection.spinner.selectedItem()
        val videoSize = binding.videoSizeSelection.spinner.selectedItem()
        val fps = binding.fpsSelection.spinner.selectedItem()
        val resolutionChange = binding.resolutionChangeSelection.spinner.selectedItem()
        val resolutionAdjustment = binding.resolutionAdjustmentSelection.spinner.selectedItem()
        val simulcastRequestRid = binding.simulcastRequestRidSelection.spinner.selectedItem()
        val spotlightEnabled =
            when (binding.spotlightEnabledSelection.spinner.selectedItem()) {
                "有効" -> true
                "無効" -> false
                else -> false
            }
        val spotlightNumber = binding.spotlightNumberSelection.spinner.selectedItem()
        val spotlightFocusRid = binding.spotlightFocusRidSelection.spinner.selectedItem()
        val spotlightUnfocusRid = binding.spotlightUnfocusRidSelection.spinner.selectedItem()
        val clientId = binding.clientIdSelection.spinner.selectedItem()
        val bundleId = binding.bundleIdSelection.spinner.selectedItem()
        val dataChannelSignaling = binding.dataChannelSignalingSelection.spinner.selectedItem()
        val ignoreDisconnectWebSocket = binding.ignoreDisconnectWebSocketSelection.spinner.selectedItem()
        val initialCamera = binding.initialCameraSelection.spinner.selectedItem()

        val intent = Intent(this, RpcChatActivity::class.java)
        intent.putExtra(RpcChatActivity.EXTRA_CHANNEL_NAME, channelName)
        intent.putExtra(RpcChatActivity.EXTRA_ROLE, role)
        intent.putExtra(RpcChatActivity.EXTRA_VIDEO_CODEC, videoCodec)
        intent.putExtra(RpcChatActivity.EXTRA_VIDEO_ENABLED, videoEnabled)
        intent.putExtra(RpcChatActivity.EXTRA_AUDIO_CODEC, audioCodec)
        intent.putExtra(RpcChatActivity.EXTRA_AUDIO_ENABLED, audioEnabled)
        intent.putExtra(RpcChatActivity.EXTRA_AUDIO_BIT_RATE, audioBitRate)
        intent.putExtra(RpcChatActivity.EXTRA_VIDEO_BIT_RATE, videoBitRate)
        intent.putExtra(RpcChatActivity.EXTRA_VIDEO_SIZE, videoSize)
        intent.putExtra(RpcChatActivity.EXTRA_FPS, fps)
        intent.putExtra(RpcChatActivity.EXTRA_RESOLUTION_CHANGE, resolutionChange)
        intent.putExtra(RpcChatActivity.EXTRA_RESOLUTION_ADJUSTMENT, resolutionAdjustment)
        intent.putExtra(RpcChatActivity.EXTRA_SIMULCAST_REQUEST_RID, simulcastRequestRid)
        intent.putExtra(RpcChatActivity.EXTRA_SPOTLIGHT_ENABLED, spotlightEnabled)
        intent.putExtra(RpcChatActivity.EXTRA_SPOTLIGHT_NUMBER, spotlightNumber)
        intent.putExtra(RpcChatActivity.EXTRA_SPOTLIGHT_FOCUS_RID, spotlightFocusRid)
        intent.putExtra(RpcChatActivity.EXTRA_SPOTLIGHT_UNFOCUS_RID, spotlightUnfocusRid)
        intent.putExtra(RpcChatActivity.EXTRA_CLIENT_ID, clientId)
        intent.putExtra(RpcChatActivity.EXTRA_BUNDLE_ID, bundleId)
        intent.putExtra(RpcChatActivity.EXTRA_DATA_CHANNEL_SIGNALING, dataChannelSignaling)
        intent.putExtra(RpcChatActivity.EXTRA_IGNORE_DISCONNECT_WEBSOCKET, ignoreDisconnectWebSocket)
        intent.putExtra(RpcChatActivity.EXTRA_INITIAL_CAMERA, initialCamera)
        intent.putExtra(RpcChatActivity.EXTRA_RPC_ENABLED, true)

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
