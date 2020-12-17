package jp.shiguredo.sora.sample.ui

import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.jaredrummler.materialspinner.MaterialSpinner
import kotlinx.android.synthetic.main.activity_video_chat_room_setup.*
import kotlinx.android.synthetic.main.signaling_selection.view.*

open class SampleAppSetupActivity: AppCompatActivity() {

    companion object {
        val TAG = SampleAppSetupActivity::class.simpleName
    }

    private val videoCodecOptions = listOf("VP9", "VP8", "H264")
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

    internal fun <T> setContentView(layoutResID: Int, intentClass: Class<T>) {
        setContentView(layoutResID)

        start.setOnClickListener {
            val intent = createIntent(intentClass)
            if (intent != null) {
                startActivity(intent)
            }
        }
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)

        if (videoEnabledSelection != null) {
            videoEnabledSelection.name.text = "映像の有無"
            videoEnabledSelection.spinner.setItems(videoEnabledOptions)
        }

        if (videoCodecSelection != null) {
            videoCodecSelection.name.text = "映像コーデック"
            videoCodecSelection.spinner.setItems(videoCodecOptions)
        }

        if (audioEnabledSelection != null) {
            audioEnabledSelection.name.text = "音声の有無"
            audioEnabledSelection.spinner.setItems(audioEnabledOptions)
        }

        if (audioCodecSelection != null) {
            audioCodecSelection.name.text = "音声コーデック"
            audioCodecSelection.spinner.setItems(audioCodecOptions)
        }

        if (audioBitRateSelection != null) {
            audioBitRateSelection.name.text = "音声ビットレート"
            audioBitRateSelection.spinner.setItems(audioBitRateOptions)
        }

        if (audioStereoSelection != null) {
            audioStereoSelection.name.text = "ステレオ音声"
            audioStereoSelection.spinner.setItems(audioStereoOptions)
        }

        if (roleSelection != null) {
            roleSelection.name.text = "ロール"
            roleSelection.spinner.setItems(roleOptions)
        }

        if (multistreamSelection != null) {
            multistreamSelection.name.text = "マルチストリーム"
            multistreamSelection.spinner.setItems(multistreamOptions)
        }

        if (videoBitRateSelection != null) {
            videoBitRateSelection.name.text = "映像ビットレート"
            videoBitRateSelection.spinner.setItems(videoBitRateOptions)
        }

        if (videoSizeSelection != null) {
            videoSizeSelection.name.text = "映像サイズ"
            videoSizeSelection.spinner.setItems(videoSizeOptions)
        }

       if (fpsSelection != null) {
           fpsSelection.name.text = "フレームレート"
           fpsSelection.spinner.setItems(fpsOptions)
       }

        if (resolutionChangeSelection != null) {
            resolutionChangeSelection.name.text = "解像度の変更"
            resolutionChangeSelection.spinner.setItems(resolutionChangeOptions)
        }
    }

    internal fun <T> createIntent(klass: Class<T>): Intent? {
        val channelName = channelNameInput.text.toString()
        if (channelName.isEmpty()) {
            showInputError()
            return null
        }

        val role = selectedItem(roleSelection.spinner)
        val multistream = selectedItem(multistreamSelection.spinner)
        val videoCodec = videoCodecSelection?.run { selectedItem(spinner) }
        val videoEnabled = videoEnabledSelection?.run { selectedItem(spinner) }
        val audioCodec = audioCodecSelection?.run { selectedItem(spinner) }
        val audioEnabled = audioEnabledSelection?.run { selectedItem(spinner) }
        val audioBitRate = audioBitRateSelection?.run { selectedItem(spinner) }
        val audioStereo = audioStereoSelection?.run { selectedItem(spinner) }
        val videoBitRate = videoBitRateSelection?.run { selectedItem(spinner) }
        val videoSize = videoSizeSelection?.run { selectedItem(spinner) }
        val fps = fpsSelection?.run { selectedItem(spinner) }
        val resolutionChange = resolutionChangeSelection?.run { selectedItem(spinner) }

        val intent = Intent(this, klass)
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

        return intent
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