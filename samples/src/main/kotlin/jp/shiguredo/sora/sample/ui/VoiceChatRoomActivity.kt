package jp.shiguredo.sora.sample.ui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.gson.Gson
import jp.shiguredo.sora.sample.BuildConfig
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.databinding.ActivityVoiceChatRoomBinding
import jp.shiguredo.sora.sample.facade.SoraAudioChannel
import jp.shiguredo.sora.sample.option.SoraRoleType
import jp.shiguredo.sora.sample.voicechat.BackgroundCallService
import jp.shiguredo.sora.sdk.channel.data.ChannelAttendeesCount
import jp.shiguredo.sora.sdk.channel.option.SoraAudioOption
import jp.shiguredo.sora.sdk.error.SoraErrorReason

class VoiceChatRoomActivity : AppCompatActivity() {
    companion object {
        private val TAG = VoiceChatRoomActivity::class.simpleName

        internal const val EXTRA_BACKGROUND_CALL = "BACKGROUND_CALL"
    }

    private var channelName: String = ""

    private var audioCodec: SoraAudioOption.Codec = SoraAudioOption.Codec.DEFAULT
    private var audioBitRate: Int? = null
    private var role = SoraRoleType.SENDRECV
    private var dataChannelSignaling: Boolean? = null
    private var ignoreDisconnectWebSocket: Boolean? = null
    private var backgroundCall = false

    private var connectionRequested = false
    private var closed = false
    private var audioModeConfigured = false

    private var oldAudioMode: Int = AudioManager.MODE_INVALID

    private lateinit var binding: ActivityVoiceChatRoomBinding

    private var startFailedReceiverRegistered = false

    private val startFailedReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                if (intent?.action != BackgroundCallService.ACTION_BACKGROUND_CALL_START_FAILED) {
                    return
                }
                handleBackgroundCallStartFailed()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setupWindow()
        binding = ActivityVoiceChatRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        channelName = intent.getStringExtra("CHANNEL_NAME") ?: getString(R.string.channelId)

        audioCodec =
            when (intent.getStringExtra("AUDIO_CODEC")) {
                "未指定" -> SoraAudioOption.Codec.DEFAULT
                "OPUS" -> SoraAudioOption.Codec.OPUS
                else -> SoraAudioOption.Codec.DEFAULT
            }

        audioBitRate =
            when (intent.getStringExtra("AUDIO_BIT_RATE")) {
                "未指定" -> null
                else -> intent.getStringExtra("AUDIO_BIT_RATE")?.toInt()
            }

        role =
            when (intent.getStringExtra("ROLE")) {
                "SENDRECV" -> SoraRoleType.SENDRECV
                "SENDONLY" -> SoraRoleType.SENDONLY
                "RECVONLY" -> SoraRoleType.RECVONLY
                else -> SoraRoleType.SENDRECV
            }

        dataChannelSignaling =
            when (intent.getStringExtra("DATA_CHANNEL_SIGNALING")) {
                "無効" -> false
                "有効" -> true
                "未指定" -> null
                else -> null
            }

        ignoreDisconnectWebSocket =
            when (intent.getStringExtra("IGNORE_DISCONNECT_WEBSOCKET")) {
                "無効" -> false
                "有効" -> true
                "未指定" -> null
                else -> null
            }

        backgroundCall = intent.getBooleanExtra(EXTRA_BACKGROUND_CALL, false)

        if (backgroundCall) {
            ContextCompat.registerReceiver(
                this,
                startFailedReceiver,
                IntentFilter(BackgroundCallService.ACTION_BACKGROUND_CALL_START_FAILED),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            startFailedReceiverRegistered = true
        }

        binding.channelNameText.text = channelName
        binding.closeButton.setOnClickListener { close() }
    }

    override fun onStart() {
        super.onStart()
        // FGS は可視状態の Activity から開始する。動作中の再開始は失敗時に通話を終了させる
        // ため行わず、Service が停止している場合だけ復帰させる
        if (backgroundCall && !closed && !BackgroundCallService.isRunning()) {
            startBackgroundCallService()
        }
        if (closed) {
            return
        }
        // 通話の接続は初回の onStart のみ行い、バックグラウンドから戻ったときは再接続しない
        if (connectionRequested) {
            return
        }
        connectionRequested = true
        connectChannel()
    }

    private fun setupWindow() {
        supportActionBar?.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setWindowVisibility()
    }

    private fun setWindowVisibility() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onResume() {
        super.onResume()
        if (closed) {
            return
        }
        this.volumeControlStream = AudioManager.STREAM_VOICE_CALL

        val audioManager =
            applicationContext.getSystemService(Context.AUDIO_SERVICE)
                as AudioManager
        if (!audioModeConfigured) {
            oldAudioMode = audioManager.mode
            audioModeConfigured = true
        }
        Log.d(TAG, "AudioManager mode change: ${audioManager.mode} => MODE_IN_COMMUNICATION(3)")
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        Log.d(TAG, "AudioManager.isMicrophoneMute=${audioManager.isMicrophoneMute}")
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        if (backgroundCall) {
            // バックグラウンド通話では通話と AudioManager のモードを維持する
            return
        }
        close()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        if (startFailedReceiverRegistered) {
            unregisterReceiver(startFailedReceiver)
            startFailedReceiverRegistered = false
        }
        if (!closed) {
            closed = true
            releaseCallResources()
        }
        super.onDestroy()
    }

    internal fun close() {
        if (closed) {
            return
        }
        closed = true
        Log.d(TAG, "close")
        releaseCallResources()
        finish()
    }

    private fun releaseCallResources() {
        disposeChannel()
        BackgroundCallService.stop(this)
        restoreAudioMode()
    }

    private fun startBackgroundCallService() {
        try {
            BackgroundCallService.start(this, intent)
        } catch (e: Exception) {
            Log.e(TAG, "failed to start background call service", e)
            handleBackgroundCallStartFailed()
        }
    }

    private fun handleBackgroundCallStartFailed() {
        // FGS を開始できない場合はバックグラウンド通話を継続できないため、通話を終了する
        Toast.makeText(this, R.string.background_call_start_failed, Toast.LENGTH_LONG).show()
        close()
    }

    // AudioManager.MODE_INVALID が使われているため lint でエラーが出るので一時的に抑制しておく
    @SuppressLint("WrongConstant")
    private fun restoreAudioMode() {
        if (!audioModeConfigured) {
            return
        }
        val audioManager =
            applicationContext.getSystemService(Context.AUDIO_SERVICE)
                as AudioManager
        Log.d(TAG, "AudioManager mode change: ${audioManager.mode} => $oldAudioMode")
        audioManager.mode = oldAudioMode
        audioModeConfigured = false
    }

    private var channel: SoraAudioChannel? = null
    private var channelListener: SoraAudioChannel.Listener =
        object : SoraAudioChannel.Listener {
            override fun onConnect(channel: SoraAudioChannel) {
                changeStateText("CONNECTED")
            }

            override fun onClose(channel: SoraAudioChannel) {
                changeStateText("CLOSED")
                close()
            }

            override fun onError(
                channel: SoraAudioChannel,
                reason: SoraErrorReason,
                message: String,
            ) {
                changeStateText("ERROR")
                Toast.makeText(this@VoiceChatRoomActivity, "Error: [$reason]: $message", Toast.LENGTH_LONG).show()
                close()
            }

            override fun onAttendeesCountUpdated(
                channel: SoraAudioChannel,
                attendees: ChannelAttendeesCount,
            ) {
                Log.d(TAG, "onAttendeesCountUpdated")
            }
        }

    private fun connectChannel() {
        Log.d(TAG, "connectChannel")
        val signalingEndpointCandidates = BuildConfig.SIGNALING_ENDPOINT.split(",").map { it.trim() }
        val signalingMetadata = Gson().fromJson(BuildConfig.SIGNALING_METADATA, Map::class.java)
        channel =
            SoraAudioChannel(
                context = this,
                handler = Handler(Looper.getMainLooper()),
                signalingEndpointCandidates = signalingEndpointCandidates,
                channelId = channelName,
                dataChannelSignaling = dataChannelSignaling,
                ignoreDisconnectWebSocket = ignoreDisconnectWebSocket,
                signalingMetadata = signalingMetadata,
                audioCodec = audioCodec,
                audioBitRate = audioBitRate,
                roleType = role,
                listener = channelListener,
            )
        channel!!.connect()
    }

    internal fun changeStateText(msg: String) {
        binding.stateText.text = msg
    }

    private fun disposeChannel() {
        Log.d(TAG, "disposeChannel")
        channel?.dispose()
        channel = null
    }
}
