package jp.shiguredo.sora.sample.ui

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import jp.shiguredo.sora.sample.BuildConfig
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.databinding.ActivityRpcChatBinding
import jp.shiguredo.sora.sample.facade.SoraVideoChannel
import jp.shiguredo.sora.sample.option.SoraFrameSize
import jp.shiguredo.sora.sample.option.SoraRoleType
import jp.shiguredo.sora.sample.ui.util.MicMuteController
import jp.shiguredo.sora.sample.ui.util.RendererLayoutCalculator
import jp.shiguredo.sora.sample.ui.util.SoraScreenUtil
import jp.shiguredo.sora.sdk.channel.data.ChannelAttendeesCount
import jp.shiguredo.sora.sdk.channel.option.SoraAudioOption
import jp.shiguredo.sora.sdk.channel.option.SoraVideoOption
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import jp.shiguredo.sora.sdk.util.SoraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import org.webrtc.VideoTrack
import java.util.UUID

class RpcChatActivity : AppCompatActivity() {
    companion object {
        private val TAG = RpcChatActivity::class.simpleName

        private enum class CameraState {
            ON,
            SOFT_MUTED,
            HARD_MUTED,
        }
    }

    private var channelName = ""
    private var videoEnabled = true
    private var videoCodec: SoraVideoOption.Codec = SoraVideoOption.Codec.DEFAULT
    private var audioCodec: SoraAudioOption.Codec = SoraAudioOption.Codec.DEFAULT
    private var audioEnabled = true
    private var audioBitRate: Int? = null
    private var videoBitRate: Int? = null
    private var videoWidth: Int = SoraVideoOption.FrameSize.Landscape.VGA.x
    private var videoHeight: Int = SoraVideoOption.FrameSize.Landscape.VGA.y
    private var startWithCamera: Boolean = true
    private var fps: Int = 30
    private var degradationPreference: SoraVideoOption.DegradationPreference? = null
    private var resolutionAdjustment: SoraVideoOption.ResolutionAdjustment? = null
    private var clientId: String? = null
    private var bundleId: String? = null
    private var dataChannelSignaling: Boolean? = null
    private var ignoreDisconnectWebSocket: Boolean? = null
    private var simulcastRequestRid: String? = null
    private var spotlightNumber: Int? = null
    private var spotlightFocusRid: String? = null
    private var spotlightUnfocusRid: String? = null
    private var spotlightEnabled: Boolean = true
    private var rpcEnabled: Boolean = false
    private var dataChannelEnabled: Boolean = true

    private var oldAudioMode: Int = AudioManager.MODE_NORMAL
    private var role = SoraRoleType.SENDRECV
    private var cameraState = CameraState.ON

    private var ui: RpcChatActivityUI? = null
    private var channel: SoraVideoChannel? = null

    private lateinit var binding: ActivityRpcChatBinding

    // 解像度監視用
    private var lastResolutionWidth: Int? = null
    private var lastResolutionHeight: Int? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var resolutionMonitorSink: ResolutionMonitorSink? = null
    private var resolutionMonitorRetryCount = 0

    private val mainHandler = Handler(Looper.getMainLooper())

    // マイク状態の追跡
    private var isAudioMuted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivityRpcChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindow()
        parseIntent()
        connectChannel()
    }

    private fun setupWindow() {
        supportActionBar?.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun parseIntent() {
        channelName = intent.getStringExtra("CHANNEL_NAME") ?: ""

        videoCodec =
            when (intent.getStringExtra("VIDEO_CODEC")) {
                "未指定" -> SoraVideoOption.Codec.DEFAULT
                "VP8" -> SoraVideoOption.Codec.VP8
                "VP9" -> SoraVideoOption.Codec.VP9
                "AV1" -> SoraVideoOption.Codec.AV1
                "H264" -> SoraVideoOption.Codec.H264
                "H265" -> SoraVideoOption.Codec.H265
                else -> SoraVideoOption.Codec.DEFAULT
            }

        audioCodec =
            when (intent.getStringExtra("AUDIO_CODEC")) {
                "未指定" -> SoraAudioOption.Codec.DEFAULT
                "OPUS" -> SoraAudioOption.Codec.OPUS
                else -> SoraAudioOption.Codec.DEFAULT
            }

        role =
            when (intent.getStringExtra("ROLE")) {
                "SENDONLY" -> SoraRoleType.SENDONLY
                "RECVONLY" -> SoraRoleType.RECVONLY
                "SENDRECV" -> SoraRoleType.SENDRECV
                else -> SoraRoleType.SENDRECV
            }

        videoEnabled =
            when (intent.getStringExtra("VIDEO_ENABLED")) {
                "有効" -> true
                "無効" -> false
                else -> true
            }

        audioEnabled =
            when (intent.getStringExtra("AUDIO_ENABLED")) {
                "有効" -> true
                "無効" -> false
                else -> true
            }

        startWithCamera =
            when (intent.getStringExtra("INITIAL_CAMERA")) {
                "有効" -> true
                "無効" -> false
                else -> true
            }

        fps = (intent.getStringExtra("FPS") ?: "30").toInt()

        intent.getStringExtra("VIDEO_SIZE")?.let { key ->
            SoraFrameSize.landscape[key]?.let { p ->
                videoWidth = p.x
                videoHeight = p.y
            }
        }

        degradationPreference =
            when (intent.getStringExtra("RESOLUTION_CHANGE")) {
                "未指定" -> null
                "MAINTAIN_RESOLUTION" -> SoraVideoOption.DegradationPreference.MAINTAIN_RESOLUTION
                "MAINTAIN_FRAMERATE" -> SoraVideoOption.DegradationPreference.MAINTAIN_FRAMERATE
                "BALANCED" -> SoraVideoOption.DegradationPreference.BALANCED
                "DISABLED" -> SoraVideoOption.DegradationPreference.DISABLED
                else -> null
            }

        resolutionAdjustment =
            when (intent.getStringExtra("RESOLUTION_ADJUSTMENT")) {
                "16" -> SoraVideoOption.ResolutionAdjustment.MULTIPLE_OF_16
                "8" -> SoraVideoOption.ResolutionAdjustment.MULTIPLE_OF_8
                "4" -> SoraVideoOption.ResolutionAdjustment.MULTIPLE_OF_4
                "2" -> SoraVideoOption.ResolutionAdjustment.MULTIPLE_OF_2
                "無効" -> SoraVideoOption.ResolutionAdjustment.NONE
                else -> null
            }

        videoBitRate =
            when (val stringValue = intent.getStringExtra("VIDEO_BIT_RATE")) {
                "未指定" -> null
                else -> stringValue?.toInt()
            }

        audioBitRate =
            when (val stringValue = intent.getStringExtra("AUDIO_BIT_RATE")) {
                "未指定" -> null
                else -> stringValue?.toInt()
            }

        clientId =
            when (intent.getStringExtra("CLIENT_ID")) {
                "なし" -> null
                "端末情報" -> Build.MODEL
                "時雨堂" -> "🍖時雨堂🍗"
                "ランダム" -> UUID.randomUUID().toString()
                else -> null
            }

        bundleId =
            when (intent.getStringExtra("BUNDLE_ID")) {
                "なし" -> null
                "端末情報" -> Build.MODEL
                "時雨堂" -> "☔時雨堂🌂"
                "ランダム" -> UUID.randomUUID().toString()
                else -> null
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

        simulcastRequestRid =
            when (val stringValue = intent.getStringExtra("SIMULCAST_REQUEST_RID")) {
                "未指定" -> null
                else -> stringValue
            }

        spotlightNumber =
            when (val stringValue = intent.getStringExtra("SPOTLIGHT_NUMBER")) {
                "未指定" -> null
                else -> stringValue?.toInt()
            }

        spotlightFocusRid =
            when (val stringValue = intent.getStringExtra("SPOTLIGHT_FOCUS_RID")) {
                "未指定" -> null
                else -> stringValue
            }

        spotlightUnfocusRid =
            when (val stringValue = intent.getStringExtra("SPOTLIGHT_UNFOCUS_RID")) {
                "未指定" -> null
                else -> stringValue
            }

        rpcEnabled = intent.getBooleanExtra("RPC_ENABLED", false)
        spotlightEnabled = intent.getBooleanExtra("SPOTLIGHT_ENABLED", true)
        dataChannelEnabled = intent.getBooleanExtra("DATA_CHANNEL_ENABLED", true)

        ui =
            RpcChatActivityUI(
                activity = this,
                channelName = channelName,
                resources = resources,
                videoViewWidth = 100,
                videoViewHeight = 100,
                videoViewMargin = 10,
                density = this.resources.displayMetrics.density,
                rpcEnabled = rpcEnabled,
                spotlightEnabled = spotlightEnabled,
            )

        if (videoEnabled && startWithCamera) {
            cameraState = CameraState.ON
            ui?.showCameraOnButton()
        } else {
            cameraState = CameraState.HARD_MUTED
            ui?.showCameraOffButton()
        }
    }

    override fun onResume() {
        super.onResume()
        this.volumeControlStream = AudioManager.STREAM_VOICE_CALL
        val audioManager =
            applicationContext.getSystemService(Context.AUDIO_SERVICE)
                as AudioManager
        oldAudioMode = audioManager.mode
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        val audioManager =
            applicationContext.getSystemService(Context.AUDIO_SERVICE)
                as AudioManager
        audioManager.mode = oldAudioMode
        close()
    }

    internal fun close() {
        Log.d(TAG, "close")
        disconnectChannel()
        finish()
    }

    private var channelListener: SoraVideoChannel.Listener =
        object : SoraVideoChannel.Listener {
            override fun onConnect(channel: SoraVideoChannel) {
                ui?.changeState("#00C853")
            }

            override fun onClose(channel: SoraVideoChannel) {
                ui?.changeState("#37474F")
                close()
            }

            override fun onError(
                channel: SoraVideoChannel,
                reason: SoraErrorReason,
            ) {
                Log.e(TAG, "onError: reason=$reason")
                ui?.changeState("#E53935")
                ui?.showToastOnUI("エラーが発生しました: $reason")
            }

            override fun onAddLocalRenderer(
                channel: SoraVideoChannel,
                renderer: SurfaceViewRenderer,
            ) {
                Log.d(TAG, "onAddLocalRenderer")
                ui?.addLocalRenderer(renderer)
            }

            override fun onRemoveRemoteRenderer(
                channel: SoraVideoChannel,
                renderer: SurfaceViewRenderer,
            ) {
                Log.d(TAG, "onRemoveRemoteRenderer")
                ui?.removeRenderer(renderer)
            }

            override fun onAddRemoteRenderer(
                channel: SoraVideoChannel,
                renderer: SurfaceViewRenderer,
            ) {
                Log.d(TAG, "onAddRemoteRenderer")
                ui?.addRenderer(renderer)
                tryAttachResolutionMonitor()
                // 解像度情報を初期表示
                ui?.updateResolutionDisplay("Resolution: 受信中...")
            }

            override fun onAttendeesCountUpdated(
                channel: SoraVideoChannel,
                attendees: ChannelAttendeesCount,
            ) {
                Log.d(TAG, "onAttendeesCountUpdated: attendees=$attendees")
            }

            override fun onCameraMuteStateChanged(
                channel: SoraVideoChannel,
                cameraHardMuted: Boolean,
                cameraSoftMuted: Boolean,
            ) {
                if (cameraHardMuted || cameraSoftMuted) {
                    cameraState = CameraState.SOFT_MUTED
                    ui?.showCameraOffButton()
                } else {
                    cameraState = CameraState.ON
                    ui?.showCameraOnButton()
                }
            }
        }

    private fun connectChannel() {
        Log.d(TAG, "connectChannel")
        val handler = Handler(Looper.getMainLooper())

        try {
            val signalingEndpointCandidates = BuildConfig.SIGNALING_ENDPOINT.split(",").map { it.trim() }
            val signalingMetadata = Gson().fromJson(BuildConfig.SIGNALING_METADATA, Map::class.java)

            // RID 値の変換（文字列から enum に）
            val spotlightFocusRidEnum =
                spotlightFocusRid?.let {
                    when (it) {
                        "none" -> SoraVideoOption.SpotlightRid.NONE
                        "r0" -> SoraVideoOption.SpotlightRid.R0
                        "r1" -> SoraVideoOption.SpotlightRid.R1
                        "r2" -> SoraVideoOption.SpotlightRid.R2
                        else -> null
                    }
                }

            val spotlightUnfocusRidEnum =
                spotlightUnfocusRid?.let {
                    when (it) {
                        "none" -> SoraVideoOption.SpotlightRid.NONE
                        "r0" -> SoraVideoOption.SpotlightRid.R0
                        "r1" -> SoraVideoOption.SpotlightRid.R1
                        "r2" -> SoraVideoOption.SpotlightRid.R2
                        else -> null
                    }
                }

            val simulcastRequestRidEnum =
                simulcastRequestRid?.let {
                    when (it) {
                        "none" -> SoraVideoOption.SimulcastRequestRid.NONE
                        "r0" -> SoraVideoOption.SimulcastRequestRid.R0
                        "r1" -> SoraVideoOption.SimulcastRequestRid.R1
                        "r2" -> SoraVideoOption.SimulcastRequestRid.R2
                        else -> null
                    }
                }

            channel =
                SoraVideoChannel(
                    context = this,
                    handler = handler,
                    signalingEndpointCandidates = signalingEndpointCandidates,
                    channelId = channelName,
                    signalingMetadata = signalingMetadata,
                    clientId = clientId,
                    bundleId = bundleId,
                    dataChannelSignaling = dataChannelSignaling,
                    ignoreDisconnectWebSocket = ignoreDisconnectWebSocket,
                    spotlight = spotlightEnabled,
                    spotlightNumber = spotlightNumber,
                    spotlightFocusRid = spotlightFocusRidEnum,
                    spotlightUnfocusRid = spotlightUnfocusRidEnum,
                    roleType = role,
                    videoEnabled = videoEnabled,
                    startWithCamera = startWithCamera,
                    videoWidth = videoWidth,
                    videoHeight = videoHeight,
                    videoFPS = fps,
                    videoCodec = videoCodec,
                    videoBitRate = videoBitRate,
                    audioEnabled = audioEnabled,
                    audioCodec = audioCodec,
                    audioBitRate = audioBitRate,
                    degradationPreference = degradationPreference,
                    resolutionAdjustment = resolutionAdjustment,
                    simulcast = false,
                    simulcastRequestRid = simulcastRequestRidEnum,
                    listener = channelListener,
                    rpcEnabled = rpcEnabled,
                    dataChannel = dataChannelEnabled,
                )

            channel?.connect()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect", e)
            ui?.showToastOnUI("接続に失敗しました: ${e.message}")
        }
    }

    private fun disconnectChannel() {
        Log.d(TAG, "disconnectChannel")
        detachResolutionMonitor()
        channel?.dispose()
        channel = null
    }

    internal fun handleToggleMute() {
        val channel = channel ?: return
        try {
            isAudioMuted = !isAudioMuted
            channel.mute(isAudioMuted)
            // UI ボタンを更新
            if (isAudioMuted) {
                ui?.showMicOffButton()
            } else {
                ui?.showMicOnButton()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle mute", e)
        }
    }

    internal fun handleToggleCamera() {
        val channel = channel ?: return
        when (cameraState) {
            CameraState.ON -> {
                cameraState = CameraState.SOFT_MUTED
                channel.setCameraSoftMuted(true)
                ui?.showCameraOffButton()
            }

            CameraState.SOFT_MUTED -> {
                cameraState = CameraState.ON
                channel.setCameraSoftMuted(false)
                ui?.showCameraOnButton()
            }

            CameraState.HARD_MUTED -> {
                // hard muted cannot be changed
                ui?.showToastOnUI("映像は無効です")
            }
        }
    }

    internal fun handleSwitchCamera() {
        val channel = channel ?: return
        try {
            channel.switchCamera()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch camera", e)
        }
    }

    private fun tryAttachResolutionMonitor() {
        if (resolutionMonitorSink != null) {
            return
        }

        try {
            val track = channel?.getRemoteVideoTrack()
            if (track == null) {
                // onAddRemoteRenderer 呼び出し時点では track 未登録のことがあるため短時間だけ再試行する
                if (resolutionMonitorRetryCount < 20) {
                    resolutionMonitorRetryCount += 1
                    mainHandler.postDelayed({ tryAttachResolutionMonitor() }, 100L)
                }
                return
            }

            remoteVideoTrack = track
            val monitorSink = ResolutionMonitorSink(::updateRemoteResolution)
            resolutionMonitorSink = monitorSink
            track.addSink(monitorSink)
            resolutionMonitorRetryCount = 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach resolution monitor", e)
        }
    }

    private fun detachResolutionMonitor() {
        try {
            val track = remoteVideoTrack
            val sink = resolutionMonitorSink
            if (track != null && sink != null) {
                track.removeSink(sink)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to detach resolution monitor", e)
        } finally {
            mainHandler.removeCallbacksAndMessages(null)
            resolutionMonitorRetryCount = 0
            resolutionMonitorSink = null
            remoteVideoTrack = null
            lastResolutionWidth = null
            lastResolutionHeight = null
        }
    }

    internal fun updateRemoteResolution(
        width: Int,
        height: Int,
    ) {
        if (lastResolutionWidth != width || lastResolutionHeight != height) {
            lastResolutionWidth = width
            lastResolutionHeight = height
            ui?.updateResolutionDisplay("Resolution: $width x $height")
        }
    }

    internal fun handleRequestSimulcastRid(rid: String) {
        val channel = channel ?: return
        lifecycleScope.launch {
            try {
                // リクエスト情報を表示
                val requestLog = "REQUEST:\nmethod: requestSimulcastRid\nrid: $rid\n"
                ui?.appendSimulcastRequestResponseLog(requestLog)

                val result = channel.requestSimulcastRid(rid)

                withContext(Dispatchers.Main) {
                    // レスポンス情報を表示
                    val responseLog = "RESPONSE:\nresult: $result\n"
                    ui?.appendSimulcastRequestResponseLog(responseLog)
                    ui?.showToastOnUI("Simulcast RID リクエスト: $rid -> $result")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request simulcast rid", e)
                withContext(Dispatchers.Main) {
                    val errorLog = "ERROR:\nmessage: ${e.message}\n"
                    ui?.appendSimulcastRequestResponseLog(errorLog)
                    ui?.showToastOnUI("Simulcast RID リクエスト失敗: ${e.message}")
                }
            }
        }
    }

    internal fun handleRequestSpotlightRid(
        focusRid: String,
        unfocusRid: String,
    ) {
        val channel = channel ?: return
        lifecycleScope.launch {
            try {
                // リクエスト情報を表示
                val requestLog = "REQUEST:\nmethod: requestSpotlightRid\nfocus_rid: $focusRid\nunfocus_rid: $unfocusRid\n"
                ui?.appendSimulcastRequestResponseLog(requestLog)

                val result = channel.requestSpotlightRid(focusRid, unfocusRid)

                withContext(Dispatchers.Main) {
                    // レスポンス情報を表示
                    val responseLog = "RESPONSE:\nresult: $result\n"
                    ui?.appendSimulcastRequestResponseLog(responseLog)
                    ui?.showToastOnUI("Spotlight RID リクエスト: focus=$focusRid, unfocus=$unfocusRid -> $result")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request spotlight rid", e)
                withContext(Dispatchers.Main) {
                    val errorLog = "ERROR:\nmessage: ${e.message}\n"
                    ui?.appendSimulcastRequestResponseLog(errorLog)
                    ui?.showToastOnUI("Spotlight RID リクエスト失敗: ${e.message}")
                }
            }
        }
    }

    internal fun handleResetSpotlightRid() {
        val channel = channel ?: return
        lifecycleScope.launch {
            try {
                // リクエスト情報を表示
                val requestLog = "REQUEST:\nmethod: resetSpotlightRid\n"
                ui?.appendSimulcastRequestResponseLog(requestLog)

                val result = channel.resetSpotlightRid()

                withContext(Dispatchers.Main) {
                    // レスポンス情報を表示
                    val responseLog = "RESPONSE:\nresult: $result\n"
                    ui?.appendSimulcastRequestResponseLog(responseLog)
                    ui?.showToastOnUI("Spotlight RID リセット -> $result")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset spotlight rid", e)
                withContext(Dispatchers.Main) {
                    val errorLog = "ERROR:\nmessage: ${e.message}\n"
                    ui?.appendSimulcastRequestResponseLog(errorLog)
                    ui?.showToastOnUI("Spotlight RID リセット失敗: ${e.message}")
                }
            }
        }
    }

    internal fun handlePutSignalingNotifyMetadata(
        metadataJson: String,
        push: Boolean,
    ) {
        val channel = channel ?: return
        lifecycleScope.launch {
            try {
                val requestLog =
                    "REQUEST: PutSignalingNotifyMetadata\n" +
                        "metadata: $metadataJson\n" +
                        "push: $push"
                ui?.appendSimulcastRequestResponseLog(requestLog)

                val result = channel.putSignalingNotifyMetadata(metadataJson, push)
                withContext(Dispatchers.Main) {
                    val responseLog = "RESPONSE: $result"
                    ui?.appendSimulcastRequestResponseLog(responseLog)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to put metadata", e)
                withContext(Dispatchers.Main) {
                    val errorLog = "ERROR: ${e.message}"
                    ui?.appendSimulcastRequestResponseLog(errorLog)
                }
            }
        }
    }

    internal fun handlePutSignalingNotifyMetadataItem(
        key: String,
        valueJson: String,
        push: Boolean,
    ) {
        val channel = channel ?: return
        lifecycleScope.launch {
            try {
                val requestLog =
                    "REQUEST: PutSignalingNotifyMetadataItem\n" +
                        "key: $key\n" +
                        "value: $valueJson\n" +
                        "push: $push"
                ui?.appendSimulcastRequestResponseLog(requestLog)

                val result = channel.putSignalingNotifyMetadataItem(key, valueJson, push)
                withContext(Dispatchers.Main) {
                    val responseLog = "RESPONSE: $result"
                    ui?.appendSimulcastRequestResponseLog(responseLog)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to put metadata item", e)
                withContext(Dispatchers.Main) {
                    val errorLog = "ERROR: ${e.message}"
                    ui?.appendSimulcastRequestResponseLog(errorLog)
                }
            }
        }
    }
}

class RpcChatActivityUI(
    val activity: RpcChatActivity,
    val channelName: String,
    val resources: Resources,
    val videoViewWidth: Int,
    val videoViewHeight: Int,
    val videoViewMargin: Int,
    val density: Float,
    val rpcEnabled: Boolean = false,
    val spotlightEnabled: Boolean = true,
) {
    private val renderersLayoutCalculator: RendererLayoutCalculator
    private var binding: ActivityRpcChatBinding
    private var selectedSimulcastRid: String = "none"
    private var selectedSpotlightFocusRid: String = "none"
    private var selectedSpotlightUnfocusRid: String = "none"

    init {
        binding = ActivityRpcChatBinding.inflate(activity.layoutInflater)
        activity.setContentView(binding.root)
        binding.channelNameText.text = channelName

        this.renderersLayoutCalculator =
            RendererLayoutCalculator(
                width = SoraScreenUtil.size(activity).x - dp2px(20 * 2),
                height = SoraScreenUtil.size(activity).y - dp2px(20 * 2 + 100),
            )

        binding.toggleMuteButton.setOnClickListener { activity.handleToggleMute() }
        binding.toggleCameraButton.setOnClickListener { activity.handleToggleCamera() }
        binding.switchCameraButton.setOnClickListener { activity.handleSwitchCamera() }
        binding.closeButton.setOnClickListener { activity.close() }

        // RPC UI の表示制御
        if (rpcEnabled) {
            setupRpcUI()
        }

        showMicOnButton()
    }

    private fun setupRpcUI() {
        // RequestSimulcastRid ボタン
        binding.rpcRequestSimulcastRidButton.setOnClickListener {
            showSimulcastDialog()
        }

        // RequestSpotlightRid ボタン
        binding.rpcRequestSpotlightRidButton.setOnClickListener {
            if (spotlightEnabled) {
                showSpotlightDialog()
            }
        }

        // ResetSpotlightRid ボタン
        binding.rpcResetSpotlightRidButton.setOnClickListener {
            if (spotlightEnabled) {
                activity.handleResetSpotlightRid()
            }
        }

        // PutSignalingNotifyMetadata ボタン
        binding.rpcPutSignalingMetadataButton.setOnClickListener {
            showMetadataDialog()
        }

        // PutSignalingNotifyMetadataItem ボタン
        binding.rpcPutSignalingMetadataItemButton.setOnClickListener {
            showMetadataItemDialog()
        }
    }

    private fun showSimulcastDialog() {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_simulcast_rid, null)

        val noneBtn = dialogView.findViewById<android.widget.Button>(R.id.dialogSimulcastNoneButton)
        val r0Btn = dialogView.findViewById<android.widget.Button>(R.id.dialogSimulcastR0Button)
        val r1Btn = dialogView.findViewById<android.widget.Button>(R.id.dialogSimulcastR1Button)
        val r2Btn = dialogView.findViewById<android.widget.Button>(R.id.dialogSimulcastR2Button)

        val selectionText = dialogView.findViewById<android.widget.TextView>(R.id.dialogSimulcastSelectionText)
        val cancelBtn = dialogView.findViewById<android.widget.Button>(R.id.dialogSimulcastCancelButton)
        val sendBtn = dialogView.findViewById<android.widget.Button>(R.id.dialogSimulcastSendButton)

        var dialogSelectedRid = selectedSimulcastRid

        val updateSelectionText = {
            selectionText.text = "Selected: $dialogSelectedRid"
        }

        noneBtn.setOnClickListener {
            dialogSelectedRid = "none"
            updateSelectionText()
        }
        r0Btn.setOnClickListener {
            dialogSelectedRid = "r0"
            updateSelectionText()
        }
        r1Btn.setOnClickListener {
            dialogSelectedRid = "r1"
            updateSelectionText()
        }
        r2Btn.setOnClickListener {
            dialogSelectedRid = "r2"
            updateSelectionText()
        }

        updateSelectionText()

        val dialog =
            androidx.appcompat.app.AlertDialog
                .Builder(activity)
                .setView(dialogView)
                .setCancelable(true)
                .create()

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }

        sendBtn.setOnClickListener {
            selectedSimulcastRid = dialogSelectedRid
            activity.handleRequestSimulcastRid(dialogSelectedRid)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showSpotlightDialog() {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_spotlight_rid, null)

        val focusNoneBtn = dialogView.findViewById<android.widget.Button>(R.id.dialogSpotlightFocusNoneButton)
        val focusR0Btn = dialogView.findViewById<android.widget.Button>(R.id.dialogSpotlightFocusR0Button)
        val focusR1Btn = dialogView.findViewById<android.widget.Button>(R.id.dialogSpotlightFocusR1Button)
        val focusR2Btn = dialogView.findViewById<android.widget.Button>(R.id.dialogSpotlightFocusR2Button)

        val unfocusNoneBtn = dialogView.findViewById<android.widget.Button>(R.id.dialogSpotlightUnfocusNoneButton)
        val unfocusR0Btn = dialogView.findViewById<android.widget.Button>(R.id.dialogSpotlightUnfocusR0Button)
        val unfocusR1Btn = dialogView.findViewById<android.widget.Button>(R.id.dialogSpotlightUnfocusR1Button)
        val unfocusR2Btn = dialogView.findViewById<android.widget.Button>(R.id.dialogSpotlightUnfocusR2Button)

        val selectionText = dialogView.findViewById<android.widget.TextView>(R.id.dialogSpotlightSelectionText)
        val cancelBtn = dialogView.findViewById<android.widget.Button>(R.id.dialogSpotlightCancelButton)
        val sendBtn = dialogView.findViewById<android.widget.Button>(R.id.dialogSpotlightSendButton)

        var dialogFocusRid = selectedSpotlightFocusRid
        var dialogUnfocusRid = selectedSpotlightUnfocusRid

        val updateSelectionText = {
            selectionText.text = "Focus: $dialogFocusRid | Unfocus: $dialogUnfocusRid"
        }

        focusNoneBtn.setOnClickListener {
            dialogFocusRid = "none"
            updateSelectionText()
        }
        focusR0Btn.setOnClickListener {
            dialogFocusRid = "r0"
            updateSelectionText()
        }
        focusR1Btn.setOnClickListener {
            dialogFocusRid = "r1"
            updateSelectionText()
        }
        focusR2Btn.setOnClickListener {
            dialogFocusRid = "r2"
            updateSelectionText()
        }

        unfocusNoneBtn.setOnClickListener {
            dialogUnfocusRid = "none"
            updateSelectionText()
        }
        unfocusR0Btn.setOnClickListener {
            dialogUnfocusRid = "r0"
            updateSelectionText()
        }
        unfocusR1Btn.setOnClickListener {
            dialogUnfocusRid = "r1"
            updateSelectionText()
        }
        unfocusR2Btn.setOnClickListener {
            dialogUnfocusRid = "r2"
            updateSelectionText()
        }

        updateSelectionText()

        val dialog =
            androidx.appcompat.app.AlertDialog
                .Builder(activity)
                .setView(dialogView)
                .setCancelable(true)
                .create()

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }

        sendBtn.setOnClickListener {
            selectedSpotlightFocusRid = dialogFocusRid
            selectedSpotlightUnfocusRid = dialogUnfocusRid
            activity.handleRequestSpotlightRid(dialogFocusRid, dialogUnfocusRid)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showMetadataDialog() {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_put_signaling_metadata, null)

        val metadataInput = dialogView.findViewById<android.widget.EditText>(R.id.dialogMetadataValueInput)
        val pushCheckbox = dialogView.findViewById<android.widget.CheckBox>(R.id.dialogMetadataPushCheckbox)
        val cancelBtn = dialogView.findViewById<android.widget.Button>(R.id.dialogMetadataCancelButton)
        val sendBtn = dialogView.findViewById<android.widget.Button>(R.id.dialogMetadataSendButton)

        // サンプル JSON をデフォルト値として設定
        metadataInput.setText("{\"example_key_1\": \"example_value_1\", \"example_key_2\": \"example_value_2\"}")

        val dialog =
            androidx.appcompat.app.AlertDialog
                .Builder(activity)
                .setView(dialogView)
                .setCancelable(true)
                .create()

        // ボタンサイズを大きくする
        dialog.setOnShowListener {
            sendBtn.minimumHeight = 80
            cancelBtn.minimumHeight = 80
        }

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }

        sendBtn.setOnClickListener {
            val metadata = metadataInput.text.toString().trim()
            val push = pushCheckbox.isChecked
            if (metadata.isNotEmpty()) {
                activity.handlePutSignalingNotifyMetadata(metadata, push)
                dialog.dismiss()
            } else {
                android.widget.Toast
                    .makeText(activity, "Metadata JSON is required", android.widget.Toast.LENGTH_SHORT)
                    .show()
            }
        }

        dialog.show()
    }

    private fun showMetadataItemDialog() {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_put_signaling_metadata_item, null)

        val keyInput = dialogView.findViewById<android.widget.EditText>(R.id.dialogMetadataItemKeyInput)
        val valueInput = dialogView.findViewById<android.widget.EditText>(R.id.dialogMetadataItemValueInput)
        val pushCheckbox = dialogView.findViewById<android.widget.CheckBox>(R.id.dialogMetadataItemPushCheckbox)
        val cancelBtn = dialogView.findViewById<android.widget.Button>(R.id.dialogMetadataItemCancelButton)
        val sendBtn = dialogView.findViewById<android.widget.Button>(R.id.dialogMetadataItemSendButton)

        // サンプル値をデフォルト値として設定
        keyInput.setText("example_key")
        valueInput.setText("\"example_value\"")

        val dialog =
            androidx.appcompat.app.AlertDialog
                .Builder(activity)
                .setView(dialogView)
                .setCancelable(true)
                .create()

        // ボタンサイズを大きくする
        dialog.setOnShowListener {
            sendBtn.minimumHeight = 80
            cancelBtn.minimumHeight = 80
        }

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }

        sendBtn.setOnClickListener {
            val key = keyInput.text.toString().trim()
            val value = valueInput.text.toString().trim()
            val push = pushCheckbox.isChecked
            if (key.isNotEmpty() && value.isNotEmpty()) {
                activity.handlePutSignalingNotifyMetadataItem(key, value, push)
                dialog.dismiss()
            } else {
                android.widget.Toast
                    .makeText(activity, "Key and Value are required", android.widget.Toast.LENGTH_SHORT)
                    .show()
            }
        }

        dialog.show()
    }

    internal fun appendSimulcastRequestResponseLog(message: String) {
        val currentText = binding.simulcastRequestResponseText.text.toString()
        val newText =
            if (currentText == "Waiting for request...") {
                message
            } else {
                "$currentText$message"
            }
        binding.simulcastRequestResponseText.text = newText
    }

    internal fun changeState(colorCode: String) {
        binding.channelNameText.setBackgroundColor(Color.parseColor(colorCode))
    }

    internal fun addLocalRenderer(renderer: SurfaceViewRenderer) {
        renderer.layoutParams =
            FrameLayout.LayoutParams(dp2px(100), dp2px(100))
        binding.localRendererContainer.addView(renderer)
    }

    internal fun removeLocalRenderer() {
        binding.localRendererContainer.removeAllViews()
    }

    internal fun addRenderer(renderer: SurfaceViewRenderer) {
        renderer.layoutParams = rendererLayoutParams()
        binding.rendererContainer.addView(renderer)
        renderersLayoutCalculator.add(renderer)
    }

    internal fun removeRenderer(renderer: SurfaceViewRenderer) {
        binding.rendererContainer.removeView(renderer)
        renderersLayoutCalculator.remove(renderer)
    }

    internal fun updateResolutionDisplay(resolution: String) {
        activity.runOnUiThread {
            binding.resolutionText.text = resolution
        }
    }

    internal fun showMicOnButton() {
        binding.toggleMuteButton.setImageDrawable(
            activity.resources.getDrawable(R.drawable.ic_mic_white_48dp, null),
        )
    }

    internal fun showMicOffButton() {
        binding.toggleMuteButton.setImageDrawable(
            activity.resources.getDrawable(R.drawable.ic_mic_off_white_48dp, null),
        )
    }

    internal fun showCameraOffButton() {
        binding.toggleCameraButton.setImageDrawable(
            activity.resources.getDrawable(R.drawable.ic_videocam_off_black_48dp, null),
        )
    }

    internal fun showCameraOnButton() {
        binding.toggleCameraButton.setImageDrawable(
            activity.resources.getDrawable(R.drawable.ic_videocam_on_white_48dp, null),
        )
    }

    internal fun showToastOnUI(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun dp2px(d: Int): Int = (density * d).toInt()

    private fun rendererLayoutParams(): RelativeLayout.LayoutParams {
        val params = RelativeLayout.LayoutParams(dp2px(videoViewWidth), dp2px(videoViewHeight))
        val margin = dp2px(videoViewMargin)
        params.setMargins(margin, margin, margin, margin)
        return params
    }

    private val lifecycleScope = activity.lifecycleScope
}

/**
 * ビデオフレームから解像度を抽出するカスタム VideoSink
 */
class ResolutionMonitorSink(
    private val onResolutionChanged: (width: Int, height: Int) -> Unit,
) : VideoSink {
    private var lastWidth: Int? = null
    private var lastHeight: Int? = null

    override fun onFrame(frame: VideoFrame) {
        val width = frame.rotatedWidth
        val height = frame.rotatedHeight

        // 解像度が変更された場合のみコールバック
        if (lastWidth != width || lastHeight != height) {
            lastWidth = width
            lastHeight = height
            onResolutionChanged(width, height)
        }
    }
}
