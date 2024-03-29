package jp.shiguredo.sora.sample.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.jaredrummler.materialspinner.MaterialSpinner
import jp.shiguredo.sora.sample.databinding.ActivityVideoChatRoomSetupBinding
import jp.shiguredo.sora.sample.option.SoraFrameSize

class VideoChatRoomSetupActivity : AppCompatActivity() {

    companion object {
        val TAG = VideoChatRoomSetupActivity::class.simpleName
    }

    private val videoCodecOptions = listOf("VP9", "VP8", "H264", "H265", "AV1")
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
    private val videoSizeOptions = SoraFrameSize.all.keys.toList()
    private val vp9ProfileIdOptions = listOf("未指定", "0", "1", "2", "3")
    private val av1ProfileOptions = listOf("未指定", "0", "1", "2")
    private val h264ProfileLevelIdOptions = listOf("未指定", "42e01f", "42e020", "42e034")
    private val fpsOptions = listOf("30", "10", "15", "20", "24", "60")
    private val resolutionChangeOptions = listOf("可変", "固定")
    private val resolutionAdjustmentOptions = listOf("未指定", "16", "8", "4", "2", "無効")
    private val cameraFacingOptions = listOf("前面", "背面")
    private val clientIdOptions = listOf("なし", "端末情報", "時雨堂", "ランダム")
    private val bundleIdOptions = listOf("なし", "端末情報", "時雨堂", "ランダム")
    private val dataChannelSignalingOptions = listOf("未指定", "無効", "有効")
    private val ignoreDisconnectWebSocketOptions = listOf("未指定", "無効", "有効")
    private val audioStreamingLanguageCodeOptions = listOf("未指定", "ja-JP", "en-US")

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
        binding.videoSizeSelection.spinner.selectedIndex = 3
        binding.vp9ProfileIdSelection.name.text = "VP9 プロファイル"
        binding.vp9ProfileIdSelection.spinner.setItems(vp9ProfileIdOptions)
        binding.av1ProfileSelection.name.text = "AV1 プロファイル"
        binding.av1ProfileSelection.spinner.setItems(av1ProfileOptions)
        binding.h264ProfileLevelIdSelection.name.text = "H264 プロファイル"
        binding.h264ProfileLevelIdSelection.spinner.setItems(h264ProfileLevelIdOptions)
        binding.fpsSelection.name.text = "フレームレート"
        binding.fpsSelection.spinner.setItems(fpsOptions)
        binding.resolutionChangeSelection.name.text = "解像度の変更"
        binding.resolutionChangeSelection.spinner.setItems(resolutionChangeOptions)
        binding.resolutionAdjustmentSelection.name.text = "解像度の調整"
        binding.resolutionAdjustmentSelection.spinner.setItems(resolutionAdjustmentOptions)
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
        binding.audioStreamingLanguageCodeSelection.name.text = "文字変換言語コード"
        binding.audioStreamingLanguageCodeSelection.spinner.setItems(audioStreamingLanguageCodeOptions)
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
        val vp9ProfileId = selectedItem(binding.vp9ProfileIdSelection.spinner)
        val av1Profile = selectedItem(binding.av1ProfileSelection.spinner)
        val h264ProfileLevelId = selectedItem(binding.h264ProfileLevelIdSelection.spinner)
        val fps = selectedItem(binding.fpsSelection.spinner)
        val resolutionChange = selectedItem(binding.resolutionChangeSelection.spinner)
        val resolutionAdjustment = selectedItem(binding.resolutionAdjustmentSelection.spinner)
        val cameraFacing = selectedItem(binding.cameraFacingSelection.spinner)
        val clientId = selectedItem(binding.clientIdSelection.spinner)
        val bundleId = selectedItem(binding.bundleIdSelection.spinner)
        val dataChannelSignaling = selectedItem(binding.dataChannelSignalingSelection.spinner)
        val ignoreDisconnectWebSocket = selectedItem(binding.ignoreDisconnectWebSocketSelection.spinner)
        val audioStreamingLanguageCode = selectedItem(binding.audioStreamingLanguageCodeSelection.spinner)

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
        intent.putExtra("VP9_PROFILE_ID", vp9ProfileId)
        intent.putExtra("AV1_PROFILE", av1Profile)
        intent.putExtra("H264_PROFILE_LEVEL_ID", h264ProfileLevelId)
        intent.putExtra("FPS", fps)
        intent.putExtra("RESOLUTION_CHANGE", resolutionChange)
        intent.putExtra("RESOLUTION_ADJUSTMENT", resolutionAdjustment)
        intent.putExtra("CAMERA_FACING", cameraFacing)
        intent.putExtra("CLIENT_ID", clientId)
        intent.putExtra("BUNDLE_ID", bundleId)
        intent.putExtra("DATA_CHANNEL_SIGNALING", dataChannelSignaling)
        intent.putExtra("IGNORE_DISCONNECT_WEBSOCKET", ignoreDisconnectWebSocket)
        intent.putExtra("AUDIO_STREAMING_LANGUAGE_CODE", audioStreamingLanguageCode)

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
