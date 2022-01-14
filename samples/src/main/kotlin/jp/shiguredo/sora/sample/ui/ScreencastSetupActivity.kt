package jp.shiguredo.sora.sample.ui

import android.annotation.TargetApi
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.jaredrummler.materialspinner.MaterialSpinner
import jp.shiguredo.sora.sample.BuildConfig
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.screencast.SoraScreencastService
import jp.shiguredo.sora.sample.screencast.SoraScreencastServiceStarter
import kotlinx.android.synthetic.main.activity_screencast_setup.*
import kotlinx.android.synthetic.main.signaling_selection.view.*

@TargetApi(21)
class ScreencastSetupActivity : AppCompatActivity() {

    companion object {
        val TAG = ScreencastSetupActivity::class.simpleName
    }

    private val videoCodecOptions = listOf("VP9", "VP8", "H264")
    private val audioCodecOptions = listOf("OPUS")
    private val multistreamOptions = listOf("有効", "無効")

    private var screencastStarter: SoraScreencastServiceStarter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screencast_setup)

        videoCodecSelection.name.text = "映像コーデック"
        videoCodecSelection.spinner.setItems(videoCodecOptions)
        audioCodecSelection.name.text = "音声コーデック"
        audioCodecSelection.spinner.setItems(audioCodecOptions)
        multistreamSelection.name.text = "マルチストリーム"
        multistreamSelection.spinner.setItems(multistreamOptions)

        start.setOnClickListener {
            val channelName = channelNameInput.text.toString()
            val videoCodec = selectedItem(videoCodecSelection.spinner)
            val audioCodec = selectedItem(audioCodecSelection.spinner)
            var multistream = selectedItem(multistreamSelection.spinner)
            startScreencast(channelName, videoCodec, audioCodec, multistream == "有効")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        screencastStarter?.onActivityResult(requestCode, resultCode, data)
    }

    private fun startScreencast(
        channelId: String,
        videoCodec: String,
        audioCodec: String,
        multistream: Boolean
    ) {
        if (SoraScreencastService.isRunning()) {
            Snackbar.make(
                rootLayout,
                "既に起動中です",
                Snackbar.LENGTH_LONG
            )
                .setAction("OK") { }
                .show()
            return
        }

        screencastStarter = SoraScreencastServiceStarter(
            activity = this,
            signalingEndpoint = BuildConfig.SIGNALING_ENDPOINT,
            signalingMetadata = BuildConfig.SIGNALING_METADATA,
            channelId = channelId,
            videoCodec = videoCodec,
            audioCodec = audioCodec,
            videoScale = 0.5f,
            videoFPS = 30,
            multistream = multistream,
            stateTitle = "Sora Screencast",
            stateText = "live on $channelId",
            stateIcon = R.drawable.icon,
            notificationIcon = R.drawable.icon,
            boundActivityName = MainActivity::class.java.canonicalName!!,
            serviceClass = SoraScreencastService::class
        )
        screencastStarter?.start()
        showNavigationMessage()
    }

    private fun showNavigationMessage() {
        AlertDialog.Builder(this)
            .setPositiveButton("OK") { _, _ -> goToHome() }
            .setCancelable(false)
            .setMessage("スクリーンキャストを終了するときは上のナビゲーションバーから終了ボタンを押してください。")
            .show()
    }

    private fun goToHome() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun selectedItem(spinner: MaterialSpinner): String {
        return spinner.getItems<String>()[spinner.selectedIndex]
    }
}
