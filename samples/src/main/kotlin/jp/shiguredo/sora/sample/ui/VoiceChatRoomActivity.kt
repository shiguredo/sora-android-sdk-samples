package jp.shiguredo.sora.sample.ui

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import jp.shiguredo.sora.sample.BuildConfig
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.databinding.ActivityVoiceChatRoomBinding
import jp.shiguredo.sora.sample.facade.SoraAudioChannel
import jp.shiguredo.sora.sample.option.SoraRoleType
import jp.shiguredo.sora.sdk.channel.data.ChannelAttendeesCount
import jp.shiguredo.sora.sdk.channel.option.SoraAudioOption
import jp.shiguredo.sora.sdk.error.SoraErrorReason

class VoiceChatRoomActivity : AppCompatActivity() {

    companion object {
        private val TAG = VoiceChatRoomActivity::class.simpleName
    }

    private var channelName: String = ""

    private var audioCodec: SoraAudioOption.Codec = SoraAudioOption.Codec.DEFAULT
    private var audioBitRate: Int? = null
    private var role = SoraRoleType.SENDRECV
    private var dataChannelSignaling: Boolean? = null
    private var ignoreDisconnectWebSocket: Boolean? = null

    private var oldAudioMode: Int = AudioManager.MODE_INVALID

    private lateinit var binding: ActivityVoiceChatRoomBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setupWindow()
        binding = ActivityVoiceChatRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        channelName = intent.getStringExtra("CHANNEL_NAME") ?: getString(R.string.channelId)

        audioCodec = when (intent.getStringExtra("AUDIO_CODEC")) {
            "未指定" -> SoraAudioOption.Codec.DEFAULT
            "OPUS" -> SoraAudioOption.Codec.OPUS
            else -> SoraAudioOption.Codec.DEFAULT
        }

        audioBitRate = when (intent.getStringExtra("AUDIO_BIT_RATE")) {
            "未指定" -> null
            else -> intent.getStringExtra("AUDIO_BIT_RATE")?.toInt()
        }

        role = when (intent.getStringExtra("ROLE")) {
            "SENDRECV" -> SoraRoleType.SENDRECV
            "SENDONLY" -> SoraRoleType.SENDONLY
            "RECVONLY" -> SoraRoleType.RECVONLY
            else -> SoraRoleType.SENDRECV
        }

        dataChannelSignaling = when (intent.getStringExtra("DATA_CHANNEL_SIGNALING")) {
            "無効" -> false
            "有効" -> true
            "未指定" -> null
            else -> null
        }

        ignoreDisconnectWebSocket = when (intent.getStringExtra("IGNORE_DISCONNECT_WEBSOCKET")) {
            "無効" -> false
            "有効" -> true
            "未指定" -> null
            else -> null
        }

        binding.channelNameText.text = channelName
        binding.closeButton.setOnClickListener { close() }

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
        this.volumeControlStream = AudioManager.STREAM_VOICE_CALL

        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE)
            as AudioManager
        oldAudioMode = audioManager.mode
        Log.d(TAG, "AudioManager mode change: $oldAudioMode => MODE_IN_COMMUNICATION(3)")
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    }

    // AudioManager.MODE_INVALID が使われているため lint でエラーが出るので一時的に抑制しておく
    @SuppressLint("WrongConstant")
    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE)
            as AudioManager
        Log.d(TAG, "AudioManager mode change: MODE_IN_COMMUNICATION(3) => $oldAudioMode")
        audioManager.mode = oldAudioMode
        close()
    }

    internal fun close() {
        Log.d(TAG, "close")
        disposeChannel()
        finish()
    }

    private var channel: SoraAudioChannel? = null
    private var channelListener: SoraAudioChannel.Listener = object : SoraAudioChannel.Listener {

        override fun onConnect(channel: SoraAudioChannel) {
            changeStateText("CONNECTED")
        }

        override fun onClose(channel: SoraAudioChannel) {
            changeStateText("CLOSED")
            close()
        }

        override fun onError(channel: SoraAudioChannel, reason: SoraErrorReason, message: String) {
            changeStateText("ERROR")
            Toast.makeText(this@VoiceChatRoomActivity, "Error: [$reason]: $message", Toast.LENGTH_LONG).show()
            close()
        }

        override fun onAttendeesCountUpdated(channel: SoraAudioChannel, attendees: ChannelAttendeesCount) {
            Log.d(TAG, "onAttendeesCountUpdated")
        }
    }

    private fun connectChannel() {
        Log.d(TAG, "connectChannel")
        val signalingEndpointCandidates = BuildConfig.SIGNALING_ENDPOINT.split(",").map { it.trim() }
        val signalingMetadata = Gson().fromJson(BuildConfig.SIGNALING_METADATA, Map::class.java)
        channel = SoraAudioChannel(
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
            listener = channelListener
        )
        channel!!.connect()
    }

    internal fun changeStateText(msg: String) {
        binding.stateText.text = msg
    }

    private fun disposeChannel() {
        Log.d(TAG, "disposeChannel")
        channel?.dispose()
    }
}
