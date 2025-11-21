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
    private val simulcastRidOptions = listOf("未指定", "r0", "r1", "r2")
    private val clientIdOptions = listOf("なし", "端末情報", "時雨堂", "ランダム")
    private val bundleIdOptions = listOf("なし", "端末情報", "時雨堂", "ランダム")
    private val dataChannelSignalingOptions = listOf("未指定", "無効", "有効")
    private val ignoreDisconnectWebSocketOptions = listOf("未指定", "無効", "有効")

    private lateinit var binding: ActivitySimulcastSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivitySimulcastSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.start.setOnClickListener { startVideoChat() }

        binding.videoEnabledSelection.name.text = "映像の有無"
        binding.videoEnabledSelection.spinner.setDropdownItems(videoEnabledOptions)
        binding.initialCameraSelection.name.text = "開始時カメラ"
        binding.initialCameraSelection.spinner.setDropdownItems(initialCameraOptions)
        binding.videoCodecSelection.name.text = "映像コーデック"
        binding.videoCodecSelection.spinner.setDropdownItems(videoCodecOptions)
        binding.audioEnabledSelection.name.text = "音声の有無"
        binding.audioEnabledSelection.spinner.setDropdownItems(audioEnabledOptions)
        binding.audioCodecSelection.name.text = "音声コーデック"
        binding.audioCodecSelection.spinner.setDropdownItems(audioCodecOptions)
        binding.audioBitRateSelection.name.text = "音声ビットレート"
        binding.audioBitRateSelection.spinner.setDropdownItems(audioBitRateOptions)
        binding.audioStereoSelection.name.text = "ステレオ音声"
        binding.audioStereoSelection.spinner.setDropdownItems(audioStereoOptions)
        binding.roleSelection.name.text = "ロール"
        binding.roleSelection.spinner.setDropdownItems(roleOptions)
        binding.videoBitRateSelection.name.text = "映像ビットレート"
        binding.videoBitRateSelection.spinner.setDropdownItems(videoBitRateOptions, defaultIndex = 6)
        binding.videoSizeSelection.name.text = "映像サイズ"
        binding.videoSizeSelection.spinner.setDropdownItems(videoSizeOptions, defaultIndex = 5)
        binding.fpsSelection.name.text = "フレームレート"
        binding.fpsSelection.spinner.setDropdownItems(fpsOptions)
        binding.resolutionChangeSelection.name.text = "解像度の変更"
        binding.resolutionChangeSelection.spinner.setDropdownItems(resolutionChangeOptions)
        binding.resolutionAdjustmentSelection.name.text = "解像度の調整"
        binding.resolutionAdjustmentSelection.spinner.setDropdownItems(resolutionAdjustmentOptions)
        binding.simulcastRidSelection.name.text = "受信する rid"
        binding.simulcastRidSelection.spinner.setDropdownItems(simulcastRidOptions)
        binding.clientIdSelection.name.text = "クライアント ID"
        binding.clientIdSelection.spinner.setDropdownItems(clientIdOptions)
        binding.bundleIdSelection.name.text = "バンドル ID"
        binding.bundleIdSelection.spinner.setDropdownItems(bundleIdOptions)
        binding.dataChannelSignalingSelection.name.text = "データチャネル"
        binding.dataChannelSignalingSelection.spinner.setDropdownItems(dataChannelSignalingOptions)
        binding.ignoreDisconnectWebSocketSelection.name.text = "WS 切断を無視"
        binding.ignoreDisconnectWebSocketSelection.spinner.setDropdownItems(ignoreDisconnectWebSocketOptions)
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
        val audioCodec = binding.audioCodecSelection.spinner.selectedItem()
        val audioEnabled = binding.audioEnabledSelection.spinner.selectedItem()
        val audioBitRate = binding.audioBitRateSelection.spinner.selectedItem()
        val audioStereo = binding.audioStereoSelection.spinner.selectedItem()
        val videoBitRate = binding.videoBitRateSelection.spinner.selectedItem()
        val videoSize = binding.videoSizeSelection.spinner.selectedItem()
        val fps = binding.fpsSelection.spinner.selectedItem()
        val resolutionChange = binding.resolutionChangeSelection.spinner.selectedItem()
        val resolutionAdjustment = binding.resolutionAdjustmentSelection.spinner.selectedItem()
        val simulcastRid = binding.simulcastRidSelection.spinner.selectedItem()
        val clientId = binding.clientIdSelection.spinner.selectedItem()
        val bundleId = binding.bundleIdSelection.spinner.selectedItem()
        val dataChannelSignaling = binding.dataChannelSignalingSelection.spinner.selectedItem()
        val ignoreDisconnectWebSocket = binding.ignoreDisconnectWebSocketSelection.spinner.selectedItem()
        val initialCamera = binding.initialCameraSelection.spinner.selectedItem()

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
        intent.putExtra("SIMULCAST_RID", simulcastRid)
        intent.putExtra("CLIENT_ID", clientId)
        intent.putExtra("BUNDLE_ID", bundleId)
        intent.putExtra("DATA_CHANNEL_SIGNALING", dataChannelSignaling)
        intent.putExtra("IGNORE_DISCONNECT_WEBSOCKET", ignoreDisconnectWebSocket)
        intent.putExtra("INITIAL_CAMERA", initialCamera)

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
}
