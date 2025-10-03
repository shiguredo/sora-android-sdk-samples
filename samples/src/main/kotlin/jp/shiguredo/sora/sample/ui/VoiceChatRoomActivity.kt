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
import jp.shiguredo.sora.sample.audio.VolumeMonitoringSink
import jp.shiguredo.sora.sample.audio.MediaStreamAudioHelper.attachAudioSink
import jp.shiguredo.sora.sample.audio.MediaStreamAudioHelper.detachAudioSink
import jp.shiguredo.sora.sample.ui.adapter.UserVolumeAdapter
import org.webrtc.MediaStream

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

    // 音量監視関連（VolumeMonitoringSinkベース）
    private lateinit var volumeMonitoringSink: VolumeMonitoringSink
    private lateinit var userVolumeAdapter: UserVolumeAdapter
    private val connectedTracks = mutableMapOf<String, String>() // trackId -> streamId
    private val connectedStreams = mutableSetOf<MediaStream>()
    private val audioSinks = mutableMapOf<String, VolumeMonitoringSink>() // streamId -> AudioSink

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
        volumeMonitoringSink = VolumeMonitoringSink().apply {
            addVolumeListener(object : VolumeMonitoringSink.VolumeListener {
                override fun onVolumeChanged(trackId: String, volumeLevel: VolumeMonitoringSink.VolumeLevel) {
                    Log.d(TAG, "[kensaku] VolumeChanged: trackId=$trackId, peakVolume=${volumeLevel.peakVolume}, rmsVolume=${volumeLevel.rmsVolume}")
                    runOnUiThread {
                        updateVolumeDisplay()
                    }
                }
            })
        }
        Log.d(TAG, "[kensaku] VolumeMonitoringSink初期化完了")
    }

    private fun updateVolumeDisplay() {
        Log.d(TAG, "[kensaku] updateVolumeDisplay開始 - connectedTracks.size=${connectedTracks.size}")
        Log.d(TAG, "[kensaku] updateVolumeDisplay - connectedTracks: $connectedTracks")
        Log.d(TAG, "[kensaku] updateVolumeDisplay - audioSinks.keys: ${audioSinks.keys}")

        val userVolumeItems = connectedTracks.map { (trackId, streamId) ->
            // 該当するストリームのAudioSinkから音量を取得
            val audioSink = audioSinks[streamId]
            val volumeLevel = audioSink?.getVolumeLevel(trackId)
            Log.d(TAG, "[kensaku] TrackData: trackId=$trackId, streamId=$streamId, volumeLevel=${volumeLevel?.peakVolume ?: "null"}, audioSinkExists=${audioSink != null}")

            UserVolumeAdapter.UserVolumeItem(
                streamId = streamId,
                trackId = trackId,
                volumeLevel = volumeLevel
            )
        }

        Log.d(TAG, "[kensaku] submitList: ${userVolumeItems.size} items")
        Log.d(TAG, "[kensaku] submitList items details:")
        userVolumeItems.forEachIndexed { index, item ->
            Log.d(TAG, "[kensaku]   [$index] streamId=${item.streamId}, trackId=${item.trackId}, hasVolumeLevel=${item.volumeLevel != null}")
        }

        // 現在のアイテム数と新しいアイテム数を比較してログ出力
        val currentItemCount = userVolumeAdapter.itemCount
        Log.d(TAG, "[kensaku] RecyclerView更新前: currentItemCount=$currentItemCount, newItemCount=${userVolumeItems.size}")

        // ListAdapterのsubmitListを使用してリストを更新
        userVolumeAdapter.submitList(userVolumeItems.toList()) {
            val updatedItemCount = userVolumeAdapter.itemCount
            Log.d(TAG, "[kensaku] RecyclerView更新完了: updatedItemCount=$updatedItemCount")

            // 追加の強制更新（削除が反映されない場合の対策）
            if (updatedItemCount != userVolumeItems.size) {
                Log.w(TAG, "[kensaku] WARNING: ItemCount不一致、強制更新実行")
                userVolumeAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        this.volumeControlStream = AudioManager.STREAM_VOICE_CALL

        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE)
                as AudioManager
        oldAudioMode = audioManager.mode
        Log.d(TAG, "AudioManager mode change: $oldAudioMode => MODE_IN_COMMUNICATION(3)")
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        // VolumeMonitoringSinkは自動で動作するため、明示的な開始は不要
    }

    // AudioManager.MODE_INVALID が使われているため lint でエラーが出るので一時的に抑制しておく
    @SuppressLint("WrongConstant")
    override fun onPause() {
        Log.d(TAG, "onPause")
        // VolumeMonitoringSinkのクリーンアップ
        cleanupVolumeMonitoring()

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
        cleanupVolumeMonitoring()
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

        override fun onAddRemoteStream(channel: SoraAudioChannel, ms: MediaStream) {
            Log.d(TAG, "[kensaku] ===== onAddRemoteStream開始 =====")
            Log.d(TAG, "[kensaku] リモートストリームを追加: streamId=${ms.id}")
            Log.d(TAG, "[kensaku] audioTracks.size=${ms.audioTracks.size}")

            // このストリーム専用のVolumeMonitoringSinkを作成
            try {
                Log.d(TAG, "[kensaku] AudioSink作成開始")
                val streamAudioSink = VolumeMonitoringSink().apply {
                    addVolumeListener(object : VolumeMonitoringSink.VolumeListener {
                        override fun onVolumeChanged(trackId: String, volumeLevel: VolumeMonitoringSink.VolumeLevel) {
                            Log.d(TAG, "[kensaku] VolumeChanged: streamId=${ms.id}, trackId=$trackId, peakVolume=${volumeLevel.peakVolume}")
                            runOnUiThread {
                                updateVolumeDisplay()
                            }
                        }
                    })
                }

                Log.d(TAG, "[kensaku] AudioSink接続開始: ${streamAudioSink.hashCode()}")

                // AudioTrackの詳細情報をログ出力
                ms.audioTracks.forEachIndexed { index, audioTrack ->
                    Log.d(TAG, "[kensaku] AudioTrack[$index] before attach: id=${audioTrack.id()}, enabled=${audioTrack.enabled()}, state=${audioTrack.state()}")
                }

                ms.attachAudioSink(streamAudioSink)
                audioSinks[ms.id] = streamAudioSink
                connectedStreams.add(ms)
                Log.d(TAG, "[kensaku] AudioSink接続完了、connectedStreams.size=${connectedStreams.size}")

                // AudioTrackのIDを取得してマッピングに追加
                ms.audioTracks.forEachIndexed { index, audioTrack ->
                    val trackId = audioTrack.id()
                    connectedTracks[trackId] = ms.id
                    Log.d(TAG, "[kensaku] AudioTrack[$index]を登録: StreamID=${ms.id}, TrackID=$trackId")
                    Log.d(TAG, "[kensaku] AudioTrack[$index] after attach: enabled=${audioTrack.enabled()}, state=${audioTrack.state()}")
                }

                Log.d(TAG, "[kensaku] connectedTracks: $connectedTracks")
                Log.d(TAG, "[kensaku] audioSinks.size=${audioSinks.size}")
                Log.d(TAG, "[kensaku] audioSinks keys: ${audioSinks.keys}")
                Log.d(TAG, "[kensaku] updateVolumeDisplay呼び出し")
                updateVolumeDisplay()
                Log.d(TAG, "[kensaku] ===== onAddRemoteStream完了 =====")
            } catch (e: Exception) {
                Log.e(TAG, "[kensaku ERROR] AudioSinkの接続に失敗: ${e.message}", e)
            }
        }

        override fun onRemoveRemoteStream(channel: SoraAudioChannel, label: String) {
            Log.d(TAG, "[kensaku] ===== onRemoveRemoteStream開始 =====")
            Log.d(TAG, "[kensaku] リモートストリームを削除: label=$label")
            Log.d(TAG, "[kensaku] 削除前 connectedStreams.size=${connectedStreams.size}")
            Log.d(TAG, "[kensaku] 削除前 connectedTracks: $connectedTracks")
            Log.d(TAG, "[kensaku] 削除前 audioSinks.size=${audioSinks.size}")

            try {
                // labelに一致するMediaStreamを見つける（disposedされたストリームも考慮）
                var streamToRemove: MediaStream? = null
                var streamIdToRemove: String? = null

                // connectedStreamsから対象を探す
                for (stream in connectedStreams) {
                    try {
                        if (stream.id == label) {
                            streamToRemove = stream
                            streamIdToRemove = label
                            break
                        }
                    } catch (e: IllegalStateException) {
                        // MediaStreamがdisposeされている場合、labelと一致するかチェックできないので
                        // audioSinksのキーで判定する
                        if (audioSinks.containsKey(label)) {
                            streamToRemove = stream
                            streamIdToRemove = label
                            Log.d(TAG, "[kensaku] dispose済みストリームを発見: label=$label")
                            break
                        }
                    }
                }

                if (streamToRemove != null && streamIdToRemove != null) {
                    Log.d(TAG, "[kensaku] 削除対象ストリーム発見: label=$streamIdToRemove")

                    // 該当するAudioSinkを取得してデタッチ
                    val streamAudioSink = audioSinks[streamIdToRemove]
                    if (streamAudioSink != null) {
                        try {
                            streamToRemove.detachAudioSink(streamAudioSink)
                            Log.d(TAG, "[kensaku] AudioSinkをデタッチ成功: $streamIdToRemove")
                        } catch (e: IllegalStateException) {
                            Log.d(TAG, "[kensaku] MediaStream既にdispose済み、AudioSinkデタッチをスキップ: $streamIdToRemove")
                        }
                        audioSinks.remove(streamIdToRemove)
                        Log.d(TAG, "[kensaku] AudioSinkを削除: $streamIdToRemove")
                    } else {
                        Log.w(TAG, "[kensaku WARNING] AudioSinkが見つからない: $streamIdToRemove")
                    }

                    connectedStreams.remove(streamToRemove)

                    // connectedTracksから該当するtrackIdを削除
                    val tracksToRemove = connectedTracks.filter { it.value == streamIdToRemove }
                    tracksToRemove.forEach { (trackId, _) ->
                        Log.d(TAG, "[kensaku] AudioTrackを削除: TrackID=$trackId")
                        connectedTracks.remove(trackId)
                        // 該当するAudioSinkから音量データをクリア
                        streamAudioSink?.clearVolumeData(trackId)
                    }

                    Log.d(TAG, "[kensaku] 削除後 connectedStreams.size=${connectedStreams.size}")
                    Log.d(TAG, "[kensaku] 削除後 connectedTracks: $connectedTracks")
                    Log.d(TAG, "[kensaku] 削除後 audioSinks.size=${audioSinks.size}")

                    // UI更新を確実にメインスレッドで実行
                    runOnUiThread {
                        updateVolumeDisplay()
                    }
                } else {
                    Log.w(TAG, "[kensaku WARNING] 削除対象ストリームが見つからない: label=$label")
                    Log.d(TAG, "[kensaku] 現在のストリーム一覧:")
                    connectedStreams.forEachIndexed { index, stream ->
                        try {
                            Log.d(TAG, "[kensaku]   [$index] streamId=${stream.id}")
                        } catch (e: IllegalStateException) {
                            Log.d(TAG, "[kensaku]   [$index] streamId=<disposed>")
                        }
                    }

                    // audioSinksにlabelが存在する場合は、dispose済みストリームの削除処理を実行
                    if (audioSinks.containsKey(label)) {
                        Log.d(TAG, "[kensaku] audioSinksにエントリが存在するため、クリーンアップを実行: $label")
                        val streamAudioSink = audioSinks.remove(label)

                        // connectedTracksから該当するtrackIdを削除
                        val tracksToRemove = connectedTracks.filter { it.value == label }
                        tracksToRemove.forEach { (trackId, _) ->
                            Log.d(TAG, "[kensaku] AudioTrackを削除: TrackID=$trackId")
                            connectedTracks.remove(trackId)
                            streamAudioSink?.clearVolumeData(trackId)
                        }

                        // UI更新
                        runOnUiThread {
                            updateVolumeDisplay()
                        }
                    }
                }
                Log.d(TAG, "[kensaku] ===== onRemoveRemoteStream完了 =====")
            } catch (e: Exception) {
                Log.e(TAG, "[kensaku ERROR] AudioSinkの切断に失敗: ${e.message}", e)

                // エラーが発生した場合でも、最低限のクリーンアップを実行
                try {
                    if (audioSinks.containsKey(label)) {
                        Log.d(TAG, "[kensaku] エラー後のクリーンアップを実行: $label")
                        val streamAudioSink = audioSinks.remove(label)

                        val tracksToRemove = connectedTracks.filter { it.value == label }
                        tracksToRemove.forEach { (trackId, _) ->
                            connectedTracks.remove(trackId)
                            streamAudioSink?.clearVolumeData(trackId)
                        }

                        runOnUiThread {
                            updateVolumeDisplay()
                        }
                    }
                } catch (cleanupError: Exception) {
                    Log.e(TAG, "[kensaku ERROR] クリーンアップ処理も失敗: ${cleanupError.message}")
                }
            }
        }

        override fun onAttendeesCountUpdated(channel: SoraAudioChannel, attendees: ChannelAttendeesCount) {
            Log.d(TAG, "onAttendeesCountUpdated: ${attendees.numberOfConnections} users")
            // 実際のストリーム管理に変更したため、ダミーユーザー管理は不要
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

    private fun cleanupVolumeMonitoring() {
        // 全てのストリームから個別のAudioSinkをデタッチ
        audioSinks.forEach { (streamId, audioSink) ->
            try {
                val stream = connectedStreams.find { it.id == streamId }
                stream?.detachAudioSink(audioSink)
                audioSink.clearAllVolumeData()
                Log.d(TAG, "[kensaku] AudioSinkをクリーンアップ: $streamId")
            } catch (e: Exception) {
                Log.w(TAG, "[kensaku WARNING] AudioSinkのデタッチに失敗: streamId=$streamId, ${e.message}")
            }
        }
        audioSinks.clear()
        connectedStreams.clear()
        connectedTracks.clear()
        Log.d(TAG, "[kensaku] 音量監視のクリーンアップ完了")
    }
}
