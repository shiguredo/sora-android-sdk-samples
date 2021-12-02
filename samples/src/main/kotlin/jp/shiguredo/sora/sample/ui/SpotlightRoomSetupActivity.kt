package jp.shiguredo.sora.sample.ui

import android.content.Intent
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.jaredrummler.materialspinner.MaterialSpinner
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sdk.Sora
import kotlinx.android.synthetic.main.activity_spotlight_room_setup.*
import kotlinx.android.synthetic.main.signaling_selection.view.*

class SpotlightRoomSetupActivity : AppCompatActivity() {

    companion object {
        private val TAG = SpotlightRoomSetupActivity::class.simpleName
    }

    private val spotlightNumberOptions = listOf("未指定", "1", "2", "3", "4", "5", "6", "7", "8")
    private val videoCodecOptions = listOf("VP8", "H264")
    private val audioCodecOptions = listOf("OPUS")
    private val audioBitRateOptions = listOf("未指定", "8", "16", "24", "32",
            "64", "96", "128", "256")
    private val videoEnabledOptions = listOf("有効", "無効")
    private val audioEnabledOptions = listOf("有効", "無効")
    private val roleOptions = listOf("SENDRECV", "SENDONLY", "RECVONLY")
    private val spotlightFocusRidOptions = listOf("未指定", "none", "r0", "r1", "r2")
    private val spotlightUnfocusRidOptions = listOf("未指定", "none", "r0", "r1", "r2")
    private val videoBitRateOptions = listOf("500", "200", "700", "1200", "2500", "4000", "5000",
            "10000", "15000", "20000", "30000")
    private val videoSizeOptions = listOf("VGA", "QQVGA", "QCIF", "HQVGA", "QVGA", "HD", "FHD")
    private val fpsOptions = listOf("30", "10", "15", "20", "24", "60")
    private val dataChannelSignalingOptions = listOf("未指定", "無効", "有効")
    private val ignoreDisconnectWebSocketOptions = listOf("未指定", "無効", "有効")

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_spotlight_room_setup)

        start.setOnClickListener { startSpotlightChat() }

        spotlightNumberSelection.name.text = "スポットライト数"
        spotlightNumberSelection.spinner.setItems(spotlightNumberOptions)
        roleSelection.name.text = "ロール"
        roleSelection.spinner.setItems(roleOptions)
        spotlightFocusRidSelection.spinner.setItems(spotlightFocusRidOptions)
        spotlightUnfocusRidSelection.name.text = "非フォーカス時の rid"
        spotlightUnfocusRidSelection.spinner.setItems(spotlightUnfocusRidOptions)
        videoCodecSelection.name.text = "映像コーデック"
        videoCodecSelection.spinner.setItems(videoCodecOptions)
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
        videoSizeSelection.name.text = "映像サイズ"
        videoSizeSelection.spinner.setItems(videoSizeOptions)
        fpsSelection.name.text = "フレームレート"
        fpsSelection.spinner.setItems(fpsOptions)
        dataChannelSignalingSelection.name.text = "データチャネル"
        dataChannelSignalingSelection.spinner.setItems(dataChannelSignalingOptions)
        ignoreDisconnectWebSocketSelection.name.text = "WS 切断を無視"
        ignoreDisconnectWebSocketSelection.spinner.setItems(ignoreDisconnectWebSocketOptions)
    }

    private fun startSpotlightChat() {
        val channelName = channelNameInput!!.text.toString()
        if (channelName.isEmpty()) {
            showInputError()
            return
        }

        val spotlightNumber = selectedItem(spotlightNumberSelection.spinner)
        val role = selectedItem(roleSelection.spinner)
        var spotlightFocusRid = selectedItem(spotlightFocusRidSelection.spinner)
        var spotlightUnfocusRid = selectedItem(spotlightUnfocusRidSelection.spinner)
        val videoCodec = selectedItem(videoCodecSelection.spinner)
        val audioCodec = selectedItem(audioCodecSelection.spinner)
        val audioBitRate = selectedItem(audioBitRateSelection.spinner)
        val audioEnabled = selectedItem(audioEnabledSelection.spinner)
        val videoEnabled = selectedItem(videoEnabledSelection.spinner)
        val videoBitRate = selectedItem(videoBitRateSelection.spinner)
        val videoSize = selectedItem(videoSizeSelection.spinner)
        val fps = selectedItem(fpsSelection.spinner)
        val dataChannelSignaling = selectedItem(dataChannelSignalingSelection.spinner)
        val ignoreDisconnectWebSocket = selectedItem(ignoreDisconnectWebSocketSelection.spinner)

        val intent = Intent(this, SimulcastActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channelName)
        intent.putExtra("SPOTLIGHT", "有効")
        intent.putExtra("SPOTLIGHT_NUMBER", spotlightNumber)
        intent.putExtra("SPOTLIGHT_FOCUS_RID", spotlightFocusRid)
        intent.putExtra("SPOTLIGHT_UNFOCUS_RID", spotlightUnfocusRid)
        intent.putExtra("ROLE", role)
        intent.putExtra("VIDEO_CODEC", videoCodec)
        intent.putExtra("AUDIO_CODEC", audioCodec)
        intent.putExtra("AUDIO_BIT_RATE", audioBitRate)
        intent.putExtra("AUDIO_ENABLED", audioEnabled)
        intent.putExtra("VIDEO_ENABLED", videoEnabled)
        intent.putExtra("VIDEO_BIT_RATE", videoBitRate)
        intent.putExtra("VIDEO_SIZE", videoSize)
        intent.putExtra("FPS", fps)
        intent.putExtra("DATA_CHANNEL_SIGNALING", dataChannelSignaling)
        intent.putExtra("IGNORE_DISCONNECT_WEBSOCKET", ignoreDisconnectWebSocket)

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
