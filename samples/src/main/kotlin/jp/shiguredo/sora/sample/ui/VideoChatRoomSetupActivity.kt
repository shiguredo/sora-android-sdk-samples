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

class VideoChatRoomSetupActivity : SampleAppSetupActivity() {

    companion object {
        val TAG = VideoChatRoomSetupActivity::class.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_chat_room_setup)

        start.setOnClickListener { startVideoChat() }
    }

    private fun startVideoChat() {
        val channelName = channelNameInput.text.toString()
        if (channelName.isEmpty()) {
            showInputError()
            return
        }

        val role = selectedItem(roleSelection.spinner)
        val multistream = selectedItem(multistreamSelection.spinner)
        val videoCodec = selectedItem(videoCodecSelection.spinner)
        val videoEnabled = selectedItem(videoEnabledSelection.spinner)
        val audioCodec = selectedItem(audioCodecSelection.spinner)
        val audioEnabled = selectedItem(audioEnabledSelection.spinner)
        val audioBitRate = selectedItem(audioBitRateSelection.spinner)
        val audioStereo = selectedItem(audioStereoSelection.spinner)
        val videoBitRate = selectedItem(videoBitRateSelection.spinner)
        val videoSize = selectedItem(videoSizeSelection.spinner)
        val fps = selectedItem(fpsSelection.spinner)
        val resolutionChange = selectedItem(resolutionChangeSelection.spinner)

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

        startActivity(intent)
    }

    private fun selectedItem(spinner: MaterialSpinner): String {
        return spinner.getItems<String>()[spinner.selectedIndex]
    }

    private fun showInputError() {
        Snackbar.make(rootLayout,
                "チャネル名 を適切に入力してください",
                Snackbar.LENGTH_LONG)
                .setAction("OK") { }
                .show()
    }

}
