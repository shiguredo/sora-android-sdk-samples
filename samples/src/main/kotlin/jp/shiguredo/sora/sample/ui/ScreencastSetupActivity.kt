package jp.shiguredo.sora.sample.ui

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
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
    private var isReceiverRegistered = false
    private var isStarting = false
    private var pendingFailureReason: String? = null

    private val screencastStartReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                when (intent?.action) {
                    SoraScreencastService.ACTION_SCREENCAST_STARTED -> {
                        isStarting = false
                        finishAndRemoveTask()
                    }
                    SoraScreencastService.ACTION_SCREENCAST_START_FAILED ->
                        handleStartFailed(intent.getStringExtra(SoraScreencastService.EXTRA_START_FAILURE_REASON))
                    SoraScreencastService.ACTION_SCREENCAST_STOPPED -> {
                        if (isStarting) {
                            handleStartFailed(null)
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivityScreencastSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        registerScreencastStartReceiver()

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

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerScreencastStartReceiver() {
        val filter =
            IntentFilter().apply {
                addAction(SoraScreencastService.ACTION_SCREENCAST_STARTED)
                addAction(SoraScreencastService.ACTION_SCREENCAST_START_FAILED)
                addAction(SoraScreencastService.ACTION_SCREENCAST_STOPPED)
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screencastStartReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screencastStartReceiver, filter)
        }
        isReceiverRegistered = true
    }

    private fun handleStartFailed(reason: String?) {
        if (reason == SoraScreencastService.START_FAILURE_REASON_CANCELED) {
            pendingFailureReason = null
            isStarting = false
            binding.start.isEnabled = true
            return
        }
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            showStartFailed(reason)
        } else {
            pendingFailureReason = reason
        }
    }

    private fun showStartFailed(reason: String?) {
        pendingFailureReason = null
        isStarting = false
        binding.start.isEnabled = true
        val messageRes =
            if (reason == SoraScreencastService.START_FAILURE_REASON_PERMISSION) {
                R.string.screencast_overlay_permission_required
            } else {
                R.string.screencast_start_failed
            }
        Snackbar
            .make(
                binding.rootLayout,
                getString(messageRes),
                Snackbar.LENGTH_LONG,
            ).show()
    }

    override fun onResume() {
        super.onResume()
        // 開始結果の broadcast を取り逃した場合でも、配信が開始済みなら画面を閉じる
        if (SoraScreencastService.isRunning()) {
            finishAndRemoveTask()
            return
        }
        pendingFailureReason?.let { showStartFailed(it) }
    }

    override fun onDestroy() {
        if (isReceiverRegistered) {
            unregisterReceiver(screencastStartReceiver)
            isReceiverRegistered = false
        }
        super.onDestroy()
    }

    private fun startScreencast(
        channelId: String,
        videoCodec: String,
        audioCodec: String,
    ) {
        if (isStarting) {
            return
        }
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

        isStarting = true
        binding.start.isEnabled = false
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
        try {
            screencastStarter?.start()
        } catch (e: Exception) {
            Log.e(TAG, "failed to request screen capture", e)
            showStartFailed(null)
        }
    }
}
