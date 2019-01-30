package jp.shiguredo.sora.sample.ui

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.jaredrummler.materialspinner.MaterialSpinner
import jp.shiguredo.sora.sample.R
import kotlinx.android.synthetic.main.activity_video_chat_room_setup.*
import kotlinx.android.synthetic.main.signaling_selection.view.*

class VideoChatRoomSetupActivity : AppCompatActivity() {

    companion object {
        val TAG = VideoChatRoomSetupActivity::class.simpleName
    }

    private val videoCodecOptions = listOf("VP9", "VP8", "H264")
    private val audioCodecOptions = listOf("OPUS", "PCMU")
    private val audioEnabledOptions = listOf("YES", "NO")
    private val streamTypeOptions = listOf("BIDIRECTIONAL", "SINGLE-UP", "SINGLE-DOWN", "MULTI-DOWN")
    private val bitRateOptions = listOf("UNDEFINED", "100", "300", "500", "800", "1000", "1500",
            "2000", "2500", "3000", "5000")
    private val videoSizeOptions = listOf(
            // Portrait
            "VGA", "QQVGA", "QCIF", "HQVGA", "QVGA", "HD", "FHD",
            "Res1920x3840", "UHD2160x3840", "UHD2160x4096",
            // Landscape
            "Res3840x1920")
    private val fpsOptions = listOf("30", "10", "15", "20", "24", "60")
    private val sdpSemanticsOptions = listOf("Unified Plan", "Plan B")

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_chat_room_setup)

        start.setOnClickListener { startVideoChat() }

        videoCodecSelection.name.text = "VIDEO CODEC"
        videoCodecSelection.spinner.setItems(videoCodecOptions)
        audioEnabledSelection.name.text = "AUDIO ENABLED"
        audioEnabledSelection.spinner.setItems(audioEnabledOptions)
        audioCodecSelection.name.text = "AUDIO CODEC"
        audioCodecSelection.spinner.setItems(audioCodecOptions)
        streamTypeSelection.name.text = "STREAM TYPE"
        streamTypeSelection.spinner.setItems(streamTypeOptions)
        bitRateSelection.name.text = "BIT RATE"
        bitRateSelection.spinner.setItems(bitRateOptions)
        videoSizeSelection.name.text = "VIDEO SIZE"
        videoSizeSelection.spinner.setItems(videoSizeOptions)
        fpsSelection.name.text = "FPS"
        fpsSelection.spinner.setItems(fpsOptions)
        sdpSemanticsSelection.name.text = "SDP SEMANTICS"
        sdpSemanticsSelection.spinner.setItems(sdpSemanticsOptions)
    }

    private fun startVideoChat() {
        val channelName = channelNameInput.text.toString()
        if (channelName.isEmpty()) {
            showInputError()
            return
        }

        val streamType = selectedItem(streamTypeSelection.spinner)
        val videoCodec = selectedItem(videoCodecSelection.spinner)
        val audioCodec = selectedItem(audioCodecSelection.spinner)
        val audioEnabled = selectedItem(audioEnabledSelection.spinner)
        val bitRate = selectedItem(bitRateSelection.spinner)
        val videoSize = selectedItem(videoSizeSelection.spinner)
        val fps = selectedItem(fpsSelection.spinner)
        val sdpSemantics = selectedItem(sdpSemanticsSelection.spinner)

        val intent = Intent(this, VideoChatRoomActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channelName)
        intent.putExtra("STREAM_TYPE", streamType)
        intent.putExtra("VIDEO_CODEC", videoCodec)
        intent.putExtra("AUDIO_CODEC", audioCodec)
        intent.putExtra("AUDIO_ENABLED", audioEnabled)
        intent.putExtra("BITRATE", bitRate)
        intent.putExtra("VIDEO_SIZE", videoSize)
        intent.putExtra("FPS", fps)
        intent.putExtra("SDP_SEMANTICS", sdpSemantics)

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
