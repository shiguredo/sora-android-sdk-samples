package jp.shiguredo.sora.sample.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.jaredrummler.materialspinner.MaterialSpinner
import jp.shiguredo.sora.sample.databinding.ActivityVideoChatRoomSetupBinding

class VideoChatRoomSetupActivity : AppCompatActivity() {

    companion object {
        val TAG = VideoChatRoomSetupActivity::class.simpleName
    }

    private val videoCodecOptions = listOf("VP9", "VP8", "H264")
    private val videoEnabledOptions = listOf("有効", "無効")
    private val audioCodecOptions = listOf("OPUS")
    private val audioEnabledOptions = listOf("有効", "無効")
    private val audioBitRateOptions = listOf(
        "未指定", "8", "16", "24", "32",
        "64", "96", "128", "256"
    )
    private val audioStereoOptions = listOf("モノラル", "ステレオ")
    private val roleOptions = listOf("SENDRECV", "SENDONLY", "RECVONLY")
    private val multistreamOptions = listOf("有効", "無効")
    private val videoBitRateOptions = listOf(
        "未指定", "100", "300", "500", "800", "1000", "1500",
        "2000", "2500", "3000", "5000", "10000", "15000", "20000", "30000"
    )
    private val videoSizeOptions = listOf(
        // Portrait
        "VGA", "QQVGA", "QCIF", "HQVGA", "QVGA", "HD", "FHD",
        "Res1920x3840", "UHD2160x3840", "UHD2160x4096",
        // Landscape
        "Res3840x1920", "UHD3840x2160"
    )
    private val fpsOptions = listOf("30", "10", "15", "20", "24", "60")
    private val resolutionChangeOptions = listOf("可変", "固定")
    private val cameraFacingOptions = listOf("前面", "背面")
    private val clientIdOptions = listOf("なし", "端末情報", "時雨堂", "ランダム")
    private val bundleIdOptions = listOf("なし", "端末情報", "時雨堂", "ランダム")
    private val dataChannelSignalingOptions = listOf("未指定", "無効", "有効")
    private val ignoreDisconnectWebSocketOptions = listOf("未指定", "無効", "有効")

    private lateinit var binding: ActivityVideoChatRoomSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivityVideoChatRoomSetupBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.start.setOnClickListener { startVideoChat() }

        binding.videoEnabledSelection.name.text = "映像の有無"
        binding.videoEnabledSelection.spinner.setItems(videoEnabledOptions)
        binding.videoCodecSelection.name.text = "映像コーデック"
        binding.videoCodecSelection.spinner.setItems(videoCodecOptions)
        binding.audioEnabledSelection.name.text = "音声の有無"
        binding.audioEnabledSelection.spinner.setItems(audioEnabledOptions)
        binding.audioCodecSelection.name.text = "音声コーデック"
        binding.audioCodecSelection.spinner.setItems(audioCodecOptions)
        binding.audioBitRateSelection.name.text = "音声ビットレート"
        binding.audioBitRateSelection.spinner.setItems(audioBitRateOptions)
        binding.audioStereoSelection.name.text = "ステレオ音声"
        binding.audioStereoSelection.spinner.setItems(audioStereoOptions)
        binding.roleSelection.name.text = "ロール"
        binding.roleSelection.spinner.setItems(roleOptions)
        binding.multistreamSelection.name.text = "マルチストリーム"
        binding.multistreamSelection.spinner.setItems(multistreamOptions)
        binding.videoBitRateSelection.name.text = "映像ビットレート"
        binding.videoBitRateSelection.spinner.setItems(videoBitRateOptions)
        binding.videoSizeSelection.name.text = "映像サイズ"
        binding.videoSizeSelection.spinner.setItems(videoSizeOptions)
        binding.fpsSelection.name.text = "フレームレート"
        binding.fpsSelection.spinner.setItems(fpsOptions)
        binding.resolutionChangeSelection.name.text = "解像度の変更"
        binding.resolutionChangeSelection.spinner.setItems(resolutionChangeOptions)
        binding.cameraFacingSelection.name.text = "カメラ"
        binding.cameraFacingSelection.spinner.setItems(cameraFacingOptions)
        binding.clientIdSelection.name.text = "クライアント ID"
        binding.clientIdSelection.spinner.setItems(clientIdOptions)
        binding.bundleIdSelection.name.text = "バンドル ID"
        binding.bundleIdSelection.spinner.setItems(bundleIdOptions)
        binding.dataChannelSignalingSelection.name.text = "データチャネル"
        binding.dataChannelSignalingSelection.spinner.setItems(dataChannelSignalingOptions)
        binding.ignoreDisconnectWebSocketSelection.name.text = "WS 切断を無視"
        binding.ignoreDisconnectWebSocketSelection.spinner.setItems(ignoreDisconnectWebSocketOptions)
    }

    private fun startVideoChat() {
        val channelName = binding.channelNameInput.text.toString()
        if (channelName.isEmpty()) {
            showInputError()
            return
        }

        val role = selectedItem(binding.roleSelection.spinner)
        val multistream = selectedItem(binding.multistreamSelection.spinner)
        val videoCodec = selectedItem(binding.videoCodecSelection.spinner)
        val videoEnabled = selectedItem(binding.videoEnabledSelection.spinner)
        val audioCodec = selectedItem(binding.audioCodecSelection.spinner)
        val audioEnabled = selectedItem(binding.audioEnabledSelection.spinner)
        val audioBitRate = selectedItem(binding.audioBitRateSelection.spinner)
        val audioStereo = selectedItem(binding.audioStereoSelection.spinner)
        val videoBitRate = selectedItem(binding.videoBitRateSelection.spinner)
        val videoSize = selectedItem(binding.videoSizeSelection.spinner)
        val fps = selectedItem(binding.fpsSelection.spinner)
        val resolutionChange = selectedItem(binding.resolutionChangeSelection.spinner)
        val cameraFacing = selectedItem(binding.cameraFacingSelection.spinner)
        val clientId = selectedItem(binding.clientIdSelection.spinner)
        val bundleId = selectedItem(binding.bundleIdSelection.spinner)
        val dataChannelSignaling = selectedItem(binding.dataChannelSignalingSelection.spinner)
        val ignoreDisconnectWebSocket = selectedItem(binding.ignoreDisconnectWebSocketSelection.spinner)

        val intent = Intent(this, VideoChatRoomActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channelName)
        intent.putExtra("ROLE", role)
        intent.putExtra("MULTISTREAM", multistream)
        intent.putExtra("VIDEO_CODEC", videoCodec)
        intent.putExtra("VIDEO_ENABLED", videoEnabled)
        intent.putExtra("AUDIO_CODEC", audioCodec)
        intent.putExtra("AUDIO_ENABLED", audioEnabled)
        intent.putExtra("AUDIO_BIT_RATE", audioBitRate)
        intent.putExtra("AUDIO_STEREO", audioStereo)
        intent.putExtra("VIDEO_BIT_RATE", videoBitRate)
        intent.putExtra("VIDEO_SIZE", videoSize)
        intent.putExtra("FPS", fps)
        intent.putExtra("RESOLUTION_CHANGE", resolutionChange)
        intent.putExtra("CAMERA_FACING", cameraFacing)
        intent.putExtra("CLIENT_ID", clientId)
        intent.putExtra("BUNDLE_ID", bundleId)
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
            "チャネル名 を適切に入力してください",
            Snackbar.LENGTH_LONG
        )
            .setAction("OK") { }
            .show()
    }
}
