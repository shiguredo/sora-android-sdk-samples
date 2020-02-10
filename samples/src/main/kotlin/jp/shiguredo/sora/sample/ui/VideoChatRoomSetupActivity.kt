package jp.shiguredo.sora.sample.ui

import android.content.Intent
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.jaredrummler.materialspinner.MaterialSpinner
import jp.shiguredo.sora.sample.R
import kotlinx.android.synthetic.main.activity_video_chat_room_setup.*
import kotlinx.android.synthetic.main.signaling_selection.view.*

class VideoChatRoomSetupActivity : AppCompatActivity() {

    companion object {
        val TAG = VideoChatRoomSetupActivity::class.simpleName
    }

    private val videoCodecOptions = listOf("H264", "VP8", "VP9", "VP8", "H264")
    private val videoEnabledOptions = listOf("YES", "NO")
    private val audioCodecOptions = listOf("OPUS", "PCMU")
    private val audioEnabledOptions = listOf("NO", "YES", "NO")
    private val audioBitRateOptions = listOf("UNDEFINED", "8", "16", "24", "32",
            "64", "96", "128", "256")
    private val audioStereoOptions = listOf("MONO", "STEREO")
    private val streamTypeOptions = listOf("SINGLE-UP", "BIDIRECTIONAL", "SINGLE-UP", "SINGLE-DOWN", "MULTI-DOWN")
    private val videoBitRateOptions = listOf("30000", "UNDEFINED", "100", "300", "500", "800", "1000", "1500",
            "2000", "2500", "3000", "5000", "10000", "15000", "20000", "30000")
    private val videoSizeOptions = listOf(
            "UHD3840x2160",
            // Portrait
            "FHD", "QQVGA", "QCIF", "HQVGA", "QVGA", "VGA", "HD", "FHD",
            "Res1920x3840", "UHD2160x3840", "UHD2160x4096",
            // Landscape
            "Res3840x1920", "UHD3840x2160")
    private val simulcastOptions = listOf("ENABLED", "DISABLED", "ENABLED")
    private val fpsOptions = listOf("30", "10", "15", "20", "24", "60")
    private val resolutionChangeOptions = listOf("FIXED", "VARIABLE", "FIXED")
    private val cameraFacingOptions = listOf("REAR", "FRONT")
    private val clientIdOptions = listOf("NONE", "BUILD MODEL", "時雨堂", "RANDOM UUID")

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_chat_room_setup)

        start.setOnClickListener { startVideoChat() }

        videoEnabledSelection.name.text = "VIDEO ENABLED"
        videoEnabledSelection.spinner.setItems(videoEnabledOptions)
        videoCodecSelection.name.text = "VIDEO CODEC"
        videoCodecSelection.spinner.setItems(videoCodecOptions)
        audioEnabledSelection.name.text = "AUDIO ENABLED"
        audioEnabledSelection.spinner.setItems(audioEnabledOptions)
        audioCodecSelection.name.text = "AUDIO CODEC"
        audioCodecSelection.spinner.setItems(audioCodecOptions)
        audioBitRateSelection.name.text = "AUDIO BIT RATE"
        audioBitRateSelection.spinner.setItems(audioBitRateOptions)
        audioStereoSelection.name.text = "AUDIO STEREO"
        audioStereoSelection.spinner.setItems(audioStereoOptions)
        streamTypeSelection.name.text = "STREAM TYPE"
        streamTypeSelection.spinner.setItems(streamTypeOptions)
        videoBitRateSelection.name.text = "VIDEO BIT RATE"
        videoBitRateSelection.spinner.setItems(videoBitRateOptions)
        videoSizeSelection.name.text = "VIDEO SIZE"
        videoSizeSelection.spinner.setItems(videoSizeOptions)
        simulcastSelection.name.text = "SIMULCAST"
        simulcastSelection.spinner.setItems(simulcastOptions)
        fpsSelection.name.text = "FPS"
        fpsSelection.spinner.setItems(fpsOptions)
        resolutionChangeSelection.name.text = "RESOLUTION CHANGE"
        resolutionChangeSelection.spinner.setItems(resolutionChangeOptions)
        cameraFacingSelection.name.text = "CAMERA FACING"
        cameraFacingSelection.spinner.setItems(cameraFacingOptions)
        clientIdSelection.name.text = "CLIENT ID"
        clientIdSelection.spinner.setItems(clientIdOptions)
    }

    private fun startVideoChat() {
        val channelName = channelNameInput.text.toString()
        if (channelName.isEmpty()) {
            showInputError()
            return
        }

        val streamType = selectedItem(streamTypeSelection.spinner)
        val videoCodec = selectedItem(videoCodecSelection.spinner)
        val videoEnabled = selectedItem(videoEnabledSelection.spinner)
        val audioCodec = selectedItem(audioCodecSelection.spinner)
        val audioEnabled = selectedItem(audioEnabledSelection.spinner)
        val audioBitRate = selectedItem(audioBitRateSelection.spinner)
        val audioStereo = selectedItem(audioStereoSelection.spinner)
        val videoBitRate = selectedItem(videoBitRateSelection.spinner)
        val videoSize = selectedItem(videoSizeSelection.spinner)
        val simulcast = selectedItem(simulcastSelection.spinner)
        val fps = selectedItem(fpsSelection.spinner)
        val resolutionChange = selectedItem(resolutionChangeSelection.spinner)
        val cameraFacing = selectedItem(cameraFacingSelection.spinner)
        val clientId = selectedItem(clientIdSelection.spinner)

        val intent = Intent(this, VideoChatRoomActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channelName)
        intent.putExtra("STREAM_TYPE", streamType)
        intent.putExtra("VIDEO_CODEC", videoCodec)
        intent.putExtra("VIDEO_ENABLED", videoEnabled)
        intent.putExtra("AUDIO_CODEC", audioCodec)
        intent.putExtra("AUDIO_ENABLED", audioEnabled)
        intent.putExtra("AUDIO_BIT_RATE", audioBitRate)
        intent.putExtra("AUDIO_STEREO", audioStereo)
        intent.putExtra("VIDEO_BIT_RATE", videoBitRate)
        intent.putExtra("VIDEO_SIZE", videoSize)
        intent.putExtra("SIMULCAST", simulcast)
        intent.putExtra("FPS", fps)
        intent.putExtra("RESOLUTION_CHANGE", resolutionChange)
        intent.putExtra("CAMERA_FACING", cameraFacing)
        intent.putExtra("CLIENT_ID", clientId)

        startActivity(intent)
    }

    private fun selectedItem(spinner: MaterialSpinner): String {
        return spinner.getItems<String>()[spinner.selectedIndex]
    }

    private fun showInputError() {
        Snackbar.make(rootLayout,
                "Channel Name を適切に入力してください",
                Snackbar.LENGTH_LONG)
                .setAction("OK") { }
                .show()
    }

}
