package jp.shiguredo.sora.sample.ui

import android.content.Intent
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.jaredrummler.materialspinner.MaterialSpinner
import jp.shiguredo.sora.sample.R
import kotlinx.android.synthetic.main.activity_simulcast_setup.*
import kotlinx.android.synthetic.main.signaling_selection.view.*

class SimulcastSetupActivity : AppCompatActivity() {

    companion object {
        val TAG = SimulcastSetupActivity::class.simpleName
    }

    private val videoCodecOptions = listOf("VP8", "H264")
    private val videoEnabledOptions = listOf("有効", "無効")
    private val audioCodecOptions = listOf("OPUS", "PCMU")
    private val audioEnabledOptions = listOf("有効", "無効")
    private val audioBitRateOptions = listOf("未指定", "8", "16", "24", "32",
            "64", "96", "128", "256")
    private val audioStereoOptions = listOf("モノラル", "ステレオ")
    private val roleOptions = listOf("SENDRECV", "SENDONLY", "RECVONLY")
    private val multistreamOptions = listOf("有効", "無効")
    private val videoBitRateOptions = listOf("未指定", "100", "300", "500", "800", "1000", "1500",
            "2000", "2500", "3000", "5000", "10000", "15000", "20000", "30000")
    private val videoSizeOptions = listOf(
            // Portrait
            "VGA", "QQVGA", "QCIF", "HQVGA", "QVGA", "HD", "FHD",
            "Res1920x3840", "UHD2160x3840", "UHD2160x4096",
            // Landscape
            "Res3840x1920", "UHD3840x2160")
    private val fpsOptions = listOf("30", "10", "15", "20", "24", "60")
    private val resolutionChangeOptions = listOf("可変", "固定")

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simulcast_setup)

        start.setOnClickListener { startVideoChat() }

        videoEnabledSelection.name.text = "映像の有無"
        videoEnabledSelection.spinner.setItems(videoEnabledOptions)
        videoCodecSelection.name.text = "映像コーデック"
        videoCodecSelection.spinner.setItems(videoCodecOptions)
        audioEnabledSelection.name.text = "音声の有無"
        audioEnabledSelection.spinner.setItems(audioEnabledOptions)
        audioCodecSelection.name.text = "音声コーデック"
        audioCodecSelection.spinner.setItems(audioCodecOptions)
        audioBitRateSelection.name.text = "音声ビットレート"
        audioBitRateSelection.spinner.setItems(audioBitRateOptions)
        audioStereoSelection.name.text = "ステレオ音声"
        audioStereoSelection.spinner.setItems(audioStereoOptions)
        roleSelection.name.text = "ロール"
        roleSelection.spinner.setItems(roleOptions)
        multistreamSelection.name.text = "マルチストリーム"
        multistreamSelection.spinner.setItems(multistreamOptions)
        videoBitRateSelection.name.text = "映像ビットレート"
        videoBitRateSelection.spinner.setItems(videoBitRateOptions)
        videoSizeSelection.name.text = "映像サイズ"
        videoSizeSelection.spinner.setItems(videoSizeOptions)
        fpsSelection.name.text = "フレームレート"
        fpsSelection.spinner.setItems(fpsOptions)
        resolutionChangeSelection.name.text = "解像度の変更"
        resolutionChangeSelection.spinner.setItems(resolutionChangeOptions)
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

        val intent = Intent(this, SimulcastActivity::class.java)
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
        intent.putExtra("SIMULCAST", true)
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
