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

    private val activeSpeakerLimitOptions = listOf("1", "2", "3", "4", "5", "6", "7", "8")
    private val videoCodecOptions = listOf("VP9", "VP8", "H264")
    private val audioCodecOptions = listOf("OPUS", "PCMU")
    private val audioBitRateOptions = listOf("未指定", "8", "16", "24", "32",
            "64", "96", "128", "256")
    private val videoEnabledOptions = listOf("有効", "無効")
    private val audioEnabledOptions = listOf("有効", "無効")
    private val roleOptions = listOf("SENDRECV", "SENDONLY", "RECVONLY")
    private val legacyOptions = listOf("有効", "無効")
    private val videoBitRateOptions = listOf("30", "150", "200", "350", "450", "600", "700", "1200", "2500", "4000", "5000")
    private val videoSizeOptions = listOf("VGA", "QQVGA", "QCIF", "HQVGA", "QVGA", "HD", "FHD")
    private val fpsOptions = listOf("30", "10", "15", "20", "24", "60")

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_spotlight_room_setup)

        start.setOnClickListener { startSpotlightChat() }

        activeSpeakerLimitSelection.name.text = "アクティブ配信数"
        activeSpeakerLimitSelection.spinner.setItems(activeSpeakerLimitOptions)
        activeSpeakerLimitSelection.spinner.selectedIndex = 2 // 3
        roleSelection.name.text = "ロール"
        roleSelection.spinner.setItems(roleOptions)
        legacySelection.name.text = "レガシー機能"
        legacySelection.spinner.setItems(legacyOptions)
        legacySelection.spinner.selectedIndex = 1 // 無効
        videoCodecSelection.name.text = "映像コーデック"
        videoCodecSelection.spinner.setItems(videoCodecOptions)
        videoCodecSelection.spinner.selectedIndex = 1 // VP8
        videoEnabledSelection.name.text = "映像の有無"
        videoEnabledSelection.spinner.setItems(videoEnabledOptions)
        audioCodecSelection.name.text = "音声コーデック"
        audioCodecSelection.spinner.setItems(audioCodecOptions)
        audioEnabledSelection.name.text = "音声の有無"
        audioEnabledSelection.spinner.setItems(audioEnabledOptions)
        audioBitRateSelection.name.text = "音声ビットレート"
        audioBitRateSelection.spinner.setItems(audioBitRateOptions)
        videoBitRateSelection.name.text = "映像ビットレート"
        videoBitRateSelection.spinner.setItems(videoBitRateOptions)
        videoBitRateSelection.spinner.selectedIndex = 7 // 1200
        videoSizeSelection.name.text = "映像サイズ"
        videoSizeSelection.spinner.setItems(videoSizeOptions)
        videoSizeSelection.spinner.selectedIndex = 6 // FHD
        fpsSelection.name.text = "フレームレート"
        fpsSelection.spinner.setItems(fpsOptions)
    }

    private fun startSpotlightChat() {
        val channelName = channelNameInput!!.text.toString()
        if (channelName.isEmpty()) {
            showInputError()
            return
        }

        val activeSpeakerLimit = selectedItem(activeSpeakerLimitSelection.spinner)
        val role = selectedItem(roleSelection.spinner)
        val legacy = selectedItem(legacySelection.spinner)
        val videoCodec = selectedItem(videoCodecSelection.spinner)
        val audioCodec = selectedItem(audioCodecSelection.spinner)
        val audioBitRate = selectedItem(audioBitRateSelection.spinner)
        val audioEnabled = selectedItem(audioEnabledSelection.spinner)
        val videoEnabled = selectedItem(videoEnabledSelection.spinner)
        val videoBitRate = selectedItem(videoBitRateSelection.spinner)
        val videoSize = selectedItem(videoSizeSelection.spinner)
        val fps = selectedItem(fpsSelection.spinner)

        val intent = Intent(this, SimulcastActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channelName)
        intent.putExtra("SPOTLIGHT", "有効")
        intent.putExtra("SPOTLIGHT_NUMBER", activeSpeakerLimit)
        intent.putExtra("SPOTLIGHT_LEGACY", legacy)
        intent.putExtra("ROLE", role)
        intent.putExtra("LEGACY", legacy)
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
                "チャネル名を適切に入力してください",
                Snackbar.LENGTH_LONG)
                .setAction("OK") { }
                .show()
    }
}
