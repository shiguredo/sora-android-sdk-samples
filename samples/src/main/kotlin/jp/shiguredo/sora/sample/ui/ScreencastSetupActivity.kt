package jp.shiguredo.sora.sample.ui

import android.annotation.TargetApi
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import jp.shiguredo.sora.sample.BuildConfig
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.databinding.ActivityScreencastSetupBinding
import jp.shiguredo.sora.sample.screencast.SoraScreencastService
import jp.shiguredo.sora.sample.screencast.SoraScreencastServiceStarter

@TargetApi(21)
class ScreencastSetupActivity : AppCompatActivity() {
    companion object {
        val TAG = ScreencastSetupActivity::class.simpleName
    }

    private val videoCodecOptions = listOf("VP8", "VP9", "AV1", "H264", "H265")
    private val audioCodecOptions = listOf("OPUS")

    private var screencastStarter: SoraScreencastServiceStarter? = null

    private lateinit var binding: ActivityScreencastSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivityScreencastSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.videoCodecSelection.name.text = "映像コーデック"
        binding.videoCodecSelection.spinner.setDropdownItems(videoCodecOptions)
        binding.audioCodecSelection.name.text = "音声コーデック"
        binding.audioCodecSelection.spinner.setDropdownItems(audioCodecOptions)

        binding.start.setOnClickListener {
            val channelName = binding.channelNameInput.text.toString()
            val videoCodec = binding.videoCodecSelection.spinner.selectedItem()
            val audioCodec = binding.audioCodecSelection.spinner.selectedItem()
            startScreencast(channelName, videoCodec, audioCodec)
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        screencastStarter?.onActivityResult(requestCode, resultCode, data)
    }

    private fun startScreencast(
        channelId: String,
        videoCodec: String,
        audioCodec: String,
    ) {
        if (SoraScreencastService.isRunning()) {
            Snackbar
                .make(
                    binding.rootLayout,
                    "既に起動中です",
                    Snackbar.LENGTH_LONG,
                ).setAction("OK") { }
                .show()
            return
        }

        screencastStarter =
            SoraScreencastServiceStarter(
                activity = this,
                signalingEndpoint = BuildConfig.SIGNALING_ENDPOINT,
                signalingMetadata = BuildConfig.SIGNALING_METADATA,
                channelId = channelId,
                videoCodec = videoCodec,
                audioCodec = audioCodec,
                videoScale = 0.5f,
                videoFPS = 30,
                stateTitle = "Sora Screencast",
                stateText = "live on $channelId",
                stateIcon = R.drawable.icon,
                notificationIcon = R.drawable.icon,
                boundActivityName = MainActivity::class.java.canonicalName!!,
                serviceClass = SoraScreencastService::class,
            )
        screencastStarter?.start()
        /*
         * 1つのアプリ をスクリーンキャスト時にはここに下に何か書いても、
         * すでに選ばれたアプリに遷移しているためユーザーの視界に入ることはない
         */
    }
}
