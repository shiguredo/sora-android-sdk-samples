package jp.shiguredo.sora.sample.ui

import android.annotation.TargetApi
import android.content.Intent
import android.os.Build
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
        binding.audioCodecSelection.name.text = "音声コーデック"
        setupDropdowns(
            listOf(
                // 映像コーデックの初期値は VP9
                DropdownConfig(
                    binding.videoCodecSelection.spinner,
                    videoCodecOptions,
                    defaultIndex = videoCodecOptions.indexOf("VP9"),
                ),
                DropdownConfig(binding.audioCodecSelection.spinner, audioCodecOptions),
            ),
        )
        setupShareAppHint()

        binding.start.setOnClickListener {
            val channelName = binding.channelNameInput.text.toString()
            val videoCodec = binding.videoCodecSelection.spinner.selectedItem()
            val audioCodec = binding.audioCodecSelection.spinner.selectedItem()
            startScreencast(channelName, videoCodec, audioCodec)
        }
    }

    /**
     * 画面共有の確認画面で選択する項目は Android のバージョンによって異なるため、
     * 実行中のバージョンに合わせた手順を表示します。
     */
    private fun setupShareAppHint() {
        val connect = getString(R.string.screencast_connect)
        val appName = getString(R.string.app_name)
        binding.shareAppHint.text =
            when {
                // Android 16 (API 36) 以降は「1 個のアプリを共有」と表示される
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA ->
                    getString(R.string.screencast_share_app_hint_api36, connect, appName)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                    getString(R.string.screencast_share_app_hint_api34, connect, appName)
                else -> getString(R.string.screencast_share_app_hint_legacy, connect)
            }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        screencastStarter?.onActivityResult(requestCode, data)
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
                notificationIcon = R.drawable.icon,
                boundActivityName = ScreencastGameActivity::class.java.canonicalName!!,
                serviceClass = SoraScreencastService::class,
            )
        screencastStarter?.start()
    }
}
