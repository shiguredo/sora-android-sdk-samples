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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import jp.shiguredo.sora.sample.BuildConfig
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.databinding.ActivityVoiceChatRoomBinding
import jp.shiguredo.sora.sample.facade.SoraAudioChannel
import jp.shiguredo.sora.sample.option.SoraRoleType
import jp.shiguredo.sora.sdk.channel.data.ChannelAttendeesCount
import jp.shiguredo.sora.sdk.channel.option.SoraAudioOption
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import jp.shiguredo.sora.sample.audio.StreamVolumeMonitor
import jp.shiguredo.sora.sample.ui.adapter.UserVolumeAdapter

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

    // 音量監視関連
    private var streamVolumeMonitor: StreamVolumeMonitor? = null
    private lateinit var userVolumeAdapter: UserVolumeAdapter
    private val connectedUsers = mutableSetOf<String>()

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

        // 音量表示UIの初期化
        setupVolumeDisplay()
        // 音量監視の初期化
        initializeVolumeMonitoring()

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

    private fun setupVolumeDisplay() {
        userVolumeAdapter = UserVolumeAdapter()
        binding.volumeRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@VoiceChatRoomActivity)
            adapter = userVolumeAdapter
        }
    }

    private fun initializeVolumeMonitoring() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        streamVolumeMonitor = StreamVolumeMonitor(audioManager).apply {
            addVolumeListener(object : StreamVolumeMonitor.VolumeListener {
                override fun onVolumeChanged(streamId: String, volumeLevel: StreamVolumeMonitor.VolumeLevel) {
                    runOnUiThread {
                        updateVolumeDisplay()
                    }
                }
            })
        }
    }

    private fun updateVolumeDisplay() {
        val volumeMonitor = streamVolumeMonitor ?: return

        val userVolumeItems = connectedUsers.map { streamId ->
            UserVolumeAdapter.UserVolumeItem(
                streamId = streamId,
                volumeLevel = volumeMonitor.getVolumeLevel(streamId)
            )
        }

        userVolumeAdapter.submitList(userVolumeItems)
    }

    override fun onResume() {
        super.onResume()
        this.volumeControlStream = AudioManager.STREAM_VOICE_CALL

        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE)
                as AudioManager
        oldAudioMode = audioManager.mode
        Log.d(TAG, "AudioManager mode change: $oldAudioMode => MODE_IN_COMMUNICATION(3)")
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        // 音量監視開始
        streamVolumeMonitor?.startMonitoring()
    }

    // AudioManager.MODE_INVALID が使われているため lint でエラーが出るので一時的に抑制しておく
    @SuppressLint("WrongConstant")
    override fun onPause() {
        Log.d(TAG, "onPause")

        // 音量監視停止
        streamVolumeMonitor?.stopMonitoring()

        super.onPause()
        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE)
                as AudioManager
        Log.d(TAG, "AudioManager mode change: MODE_IN_COMMUNICATION(3) => $oldAudioMode")
        audioManager.mode = oldAudioMode
        close()
    }

    internal fun close() {
        Log.d(TAG, "close")
        // 音量監視のクリーンアップ
        streamVolumeMonitor?.stopMonitoring()
        streamVolumeMonitor = null
        disposeChannel()
        finish()
    }

    private var channel: SoraAudioChannel? = null
    private var channelListener: SoraAudioChannel.Listener = object : SoraAudioChannel.Listener {

        override fun onConnect(channel: SoraAudioChannel) {
            changeStateText("CONNECTED")
            // 自分のストリームを追加（送信する場合）
            if (role == SoraRoleType.SENDRECV || role == SoraRoleType.SENDONLY) {
                addUser("local_stream")
            }
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
            Log.d(TAG, "onAttendeesCountUpdated: ${attendees.numberOfConnections} users")
            // 参加者数の変化に基づいてダミーユーザーを管理
            updateUsersBasedOnAttendees(attendees.numberOfConnections)
        }
    }

    private fun addUser(streamId: String) {
        if (connectedUsers.add(streamId)) {
            Log.d(TAG, "ユーザーを追加: $streamId")
            streamVolumeMonitor?.registerStream(streamId)
            updateVolumeDisplay()
        }
    }

    private fun removeUser(streamId: String) {
        if (connectedUsers.remove(streamId)) {
            Log.d(TAG, "ユーザーを削除: $streamId")
            streamVolumeMonitor?.unregisterStream(streamId)
            updateVolumeDisplay()
        }
    }

    private fun updateUsersBasedOnAttendees(numberOfConnections: Int) {
        // 現在の接続数に基づいてユーザーリストを調整
        val targetUsers = mutableSetOf<String>()

        // 自分のストリーム
        if (role == SoraRoleType.SENDRECV || role == SoraRoleType.SENDONLY) {
            targetUsers.add("local_stream")
        }

        // リモートユーザー（実際のstreamIdの代わりにダミーIDを使用）
        for (i in 1 until numberOfConnections) {
            targetUsers.add("remote_user_$i")
        }

        // 削除されたユーザーを処理
        val usersToRemove = connectedUsers - targetUsers
        usersToRemove.forEach { removeUser(it) }

        // 新しいユーザーを追加
        val usersToAdd = targetUsers - connectedUsers
        usersToAdd.forEach { addUser(it) }
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
