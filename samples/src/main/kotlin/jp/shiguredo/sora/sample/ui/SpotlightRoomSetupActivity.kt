package jp.shiguredo.sora.sample.ui

import android.content.Intent
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.jaredrummler.materialspinner.MaterialSpinner
import jp.shiguredo.sora.sample.R
import kotlinx.android.synthetic.main.activity_spotlight_room_setup.*
import kotlinx.android.synthetic.main.signaling_selection.view.*

class SpotlightRoomSetupActivity : AppCompatActivity() {

    companion object {
        private val TAG = SpotlightRoomSetupActivity::class.simpleName
    }

    private val spotlightNumberOptions = listOf("2", "1", "3", "4", "5")
    private val videoCodecOptions = listOf("VP9", "VP8", "H264")
    private val audioCodecOptions = listOf("OPUS", "PCMU")
    private val audioBitRateOptions = listOf("UNDEFINED", "8", "16", "24", "32",
            "64", "96", "128", "256")
    private val videoEnabledOptions = listOf("YES", "NO")
    private val audioEnabledOptions = listOf("YES", "NO")
    private val roleOptions = listOf("SENDRECV", "MULTI-DOWN")
    private val videoBitRateOptions = listOf("1000", "UNDEFINED", "100", "300", "500", "800",
            "1500", "2000", "2500")
    private val videoSizeOptions = listOf("VGA", "QQVGA", "QCIF", "HQVGA", "QVGA", "HD", "FHD")
    private val fpsOptions = listOf("30", "10", "15", "20", "24", "60")

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_spotlight_room_setup)

        start.setOnClickListener { startSpotlightChat() }

        spotlightNumberSelection.name.text = "SPOTLIGHT"
        spotlightNumberSelection.spinner.setItems(spotlightNumberOptions)
        roleSelection.name.text = "ROLE"
        roleSelection.spinner.setItems(roleOptions)
        videoCodecSelection.name.text = "VIDEO CODEC"
        videoCodecSelection.spinner.setItems(videoCodecOptions)
        videoEnabledSelection.name.text = "VIDEO ENABLED"
        videoEnabledSelection.spinner.setItems(videoEnabledOptions)
        audioCodecSelection.name.text = "AUDIO CODEC"
        audioCodecSelection.spinner.setItems(audioCodecOptions)
        audioEnabledSelection.name.text = "AUDIO ENABLED"
        audioEnabledSelection.spinner.setItems(audioEnabledOptions)
        audioBitRateSelection.name.text = "AUDIO BIT RATE"
        audioBitRateSelection.spinner.setItems(audioBitRateOptions)
        videoBitRateSelection.name.text = "VIDEO BIT RATE"
        videoBitRateSelection.spinner.setItems(videoBitRateOptions)
        videoSizeSelection.name.text = "VIDEO SIZE"
        videoSizeSelection.spinner.setItems(videoSizeOptions)
        fpsSelection.name.text = "FPS"
        fpsSelection.spinner.setItems(fpsOptions)
    }

    private fun startSpotlightChat() {
        val channelName = channelNameInput!!.text.toString()
        if (channelName.isEmpty()) {
            showInputError()
            return
        }

        val spotlightNumber = selectedItem(spotlightNumberSelection.spinner).toInt()
        val role = selectedItem(roleSelection.spinner)
        val videoCodec = selectedItem(videoCodecSelection.spinner)
        val audioCodec = selectedItem(audioCodecSelection.spinner)
        val audioBitRate = selectedItem(audioBitRateSelection.spinner)
        val audioEnabled = selectedItem(audioEnabledSelection.spinner)
        val videoEnabled = selectedItem(videoEnabledSelection.spinner)
        val videoBitRate = selectedItem(videoBitRateSelection.spinner)
        val videoSize = selectedItem(videoSizeSelection.spinner)
        val fps = selectedItem(fpsSelection.spinner)

        val intent = Intent(this, VideoChatRoomActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channelName)
        intent.putExtra("SPOTLIGHT", spotlightNumber)
        intent.putExtra("ROLE", role)
        intent.putExtra("VIDEO_CODEC", videoCodec)
        intent.putExtra("AUDIO_CODEC", audioCodec)
        intent.putExtra("AUDIO_BIT_RATE", audioBitRate)
        intent.putExtra("AUDIO_ENABLED", audioEnabled)
        intent.putExtra("VIDEO_ENABLED", videoEnabled)
        intent.putExtra("VIDEO_BIT_RATE", videoBitRate)
        intent.putExtra("VIDEO_SIZE", videoSize)
        intent.putExtra("FPS", fps)

        startActivity(intent)
    }

    private fun selectedItem(spinner: MaterialSpinner): String {
        return spinner.getItems<String>()[spinner.selectedIndex]
    }

    private fun showInputError() {
        Snackbar.make(rootLayout,
                "Channel Nameを適切に入力してください",
                Snackbar.LENGTH_LONG)
                .setAction("OK") { }
                .show()
    }
}
