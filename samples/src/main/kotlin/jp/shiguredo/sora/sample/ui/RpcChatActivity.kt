package jp.shiguredo.sora.sample.ui

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.widget.FrameLayout
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
import jp.shiguredo.sora.sample.ui.util.RendererLayoutCalculator
import jp.shiguredo.sora.sample.ui.util.SoraScreenUtil
import jp.shiguredo.sora.sdk.channel.data.ChannelAttendeesCount
import jp.shiguredo.sora.sdk.channel.option.SoraAudioOption
import jp.shiguredo.sora.sdk.channel.option.SoraVideoOption
import jp.shiguredo.sora.sdk.error.SoraErrorReason
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
        const val EXTRA_CHANNEL_NAME = "CHANNEL_NAME"
        const val EXTRA_VIDEO_CODEC = "VIDEO_CODEC"
        const val EXTRA_AUDIO_CODEC = "AUDIO_CODEC"
        const val EXTRA_ROLE = "ROLE"
        const val EXTRA_VIDEO_ENABLED = "VIDEO_ENABLED"
        const val EXTRA_AUDIO_ENABLED = "AUDIO_ENABLED"
        const val EXTRA_FPS = "FPS"
        const val EXTRA_VIDEO_SIZE = "VIDEO_SIZE"
        const val EXTRA_RESOLUTION_CHANGE = "RESOLUTION_CHANGE"
        const val EXTRA_RESOLUTION_ADJUSTMENT = "RESOLUTION_ADJUSTMENT"
        const val EXTRA_VIDEO_BIT_RATE = "VIDEO_BIT_RATE"
        const val EXTRA_AUDIO_BIT_RATE = "AUDIO_BIT_RATE"
        const val EXTRA_CLIENT_ID = "CLIENT_ID"
        const val EXTRA_BUNDLE_ID = "BUNDLE_ID"
        const val EXTRA_DATA_CHANNEL_SIGNALING = "DATA_CHANNEL_SIGNALING"
        const val EXTRA_IGNORE_DISCONNECT_WEBSOCKET = "IGNORE_DISCONNECT_WEBSOCKET"
        const val EXTRA_SIMULCAST_REQUEST_RID = "SIMULCAST_REQUEST_RID"
        const val EXTRA_SPOTLIGHT_NUMBER = "SPOTLIGHT_NUMBER"
        const val EXTRA_SPOTLIGHT_FOCUS_RID = "SPOTLIGHT_FOCUS_RID"
        const val EXTRA_SPOTLIGHT_UNFOCUS_RID = "SPOTLIGHT_UNFOCUS_RID"
        const val EXTRA_RPC_ENABLED = "RPC_ENABLED"
        const val EXTRA_SPOTLIGHT_ENABLED = "SPOTLIGHT_ENABLED"
        const val EXTRA_INITIAL_CAMERA = "INITIAL_CAMERA"

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

    private var oldAudioMode: Int = AudioManager.MODE_NORMAL
    private var role = SoraRoleType.SENDRECV
    private var cameraState = CameraState.ON

    private var ui: RpcChatActivityUI? = null
    private var channel: SoraVideoChannel? = null

    // 解像度監視用
    private var lastResolutionWidth: Int? = null
    private var lastResolutionHeight: Int? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var resolutionMonitorSink: ResolutionMonitorSink? = null
    private var resolutionMonitorRetryCount = 0

    private val mainHandler = Handler(Looper.getMainLooper())
    private val resolutionMonitorRetryRunnable = Runnable { tryAttachResolutionMonitor() }

    // マイク状態の追跡
    private var isAudioMuted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

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
        channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: ""

        videoCodec =
            when (intent.getStringExtra(EXTRA_VIDEO_CODEC)) {
                "未指定" -> SoraVideoOption.Codec.DEFAULT
                "VP8" -> SoraVideoOption.Codec.VP8
                "VP9" -> SoraVideoOption.Codec.VP9
                "AV1" -> SoraVideoOption.Codec.AV1
                "H264" -> SoraVideoOption.Codec.H264
                "H265" -> SoraVideoOption.Codec.H265
                else -> SoraVideoOption.Codec.DEFAULT
            }

        audioCodec =
            when (intent.getStringExtra(EXTRA_AUDIO_CODEC)) {
                "未指定" -> SoraAudioOption.Codec.DEFAULT
                "OPUS" -> SoraAudioOption.Codec.OPUS
                else -> SoraAudioOption.Codec.DEFAULT
            }

        role =
            when (intent.getStringExtra(EXTRA_ROLE)) {
                "SENDONLY" -> SoraRoleType.SENDONLY
                "RECVONLY" -> SoraRoleType.RECVONLY
                "SENDRECV" -> SoraRoleType.SENDRECV
                else -> SoraRoleType.SENDRECV
            }

        videoEnabled =
            when (intent.getStringExtra(EXTRA_VIDEO_ENABLED)) {
                "有効" -> true
                "無効" -> false
                else -> true
            }

        audioEnabled =
            when (intent.getStringExtra(EXTRA_AUDIO_ENABLED)) {
                "有効" -> true
                "無効" -> false
                else -> true
            }

        startWithCamera =
            when (intent.getStringExtra(EXTRA_INITIAL_CAMERA)) {
                "有効" -> true
                "無効" -> false
                else -> true
            }

        fps = (intent.getStringExtra(EXTRA_FPS) ?: "30").toInt()

        intent.getStringExtra(EXTRA_VIDEO_SIZE)?.let { key ->
            SoraFrameSize.landscape[key]?.let { p ->
                videoWidth = p.x
                videoHeight = p.y
            }
        }

        degradationPreference =
            when (intent.getStringExtra(EXTRA_RESOLUTION_CHANGE)) {
                "未指定" -> null
                "MAINTAIN_RESOLUTION" -> SoraVideoOption.DegradationPreference.MAINTAIN_RESOLUTION
                "MAINTAIN_FRAMERATE" -> SoraVideoOption.DegradationPreference.MAINTAIN_FRAMERATE
                "BALANCED" -> SoraVideoOption.DegradationPreference.BALANCED
                "DISABLED" -> SoraVideoOption.DegradationPreference.DISABLED
                else -> null
            }

        resolutionAdjustment =
            when (intent.getStringExtra(EXTRA_RESOLUTION_ADJUSTMENT)) {
                "16" -> SoraVideoOption.ResolutionAdjustment.MULTIPLE_OF_16
                "8" -> SoraVideoOption.ResolutionAdjustment.MULTIPLE_OF_8
                "4" -> SoraVideoOption.ResolutionAdjustment.MULTIPLE_OF_4
                "2" -> SoraVideoOption.ResolutionAdjustment.MULTIPLE_OF_2
                "無効" -> SoraVideoOption.ResolutionAdjustment.NONE
                else -> null
            }

        videoBitRate =
            when (val stringValue = intent.getStringExtra(EXTRA_VIDEO_BIT_RATE)) {
                "未指定" -> null
                else -> stringValue?.toInt()
            }

        audioBitRate =
            when (val stringValue = intent.getStringExtra(EXTRA_AUDIO_BIT_RATE)) {
                "未指定" -> null
                else -> stringValue?.toInt()
            }

        clientId =
            when (intent.getStringExtra(EXTRA_CLIENT_ID)) {
                "なし" -> null
                "端末情報" -> Build.MODEL
                "時雨堂" -> "🍖時雨堂🍗"
                "ランダム" -> UUID.randomUUID().toString()
                else -> null
            }

        bundleId =
            when (intent.getStringExtra(EXTRA_BUNDLE_ID)) {
                "なし" -> null
                "端末情報" -> Build.MODEL
                "時雨堂" -> "☔時雨堂🌂"
                "ランダム" -> UUID.randomUUID().toString()
                else -> null
            }

        dataChannelSignaling =
            when (intent.getStringExtra(EXTRA_DATA_CHANNEL_SIGNALING)) {
                "無効" -> false
                "有効" -> true
                "未指定" -> null
                else -> null
            }

        ignoreDisconnectWebSocket =
            when (intent.getStringExtra(EXTRA_IGNORE_DISCONNECT_WEBSOCKET)) {
                "無効" -> false
                "有効" -> true
                "未指定" -> null
                else -> null
            }

        simulcastRequestRid =
            when (val stringValue = intent.getStringExtra(EXTRA_SIMULCAST_REQUEST_RID)) {
                "未指定" -> null
                else -> stringValue
            }

        spotlightNumber =
            when (val stringValue = intent.getStringExtra(EXTRA_SPOTLIGHT_NUMBER)) {
                "未指定" -> null
                else -> stringValue?.toInt()
            }

        spotlightFocusRid =
            when (val stringValue = intent.getStringExtra(EXTRA_SPOTLIGHT_FOCUS_RID)) {
                "未指定" -> null
                else -> stringValue
            }

        spotlightUnfocusRid =
            when (val stringValue = intent.getStringExtra(EXTRA_SPOTLIGHT_UNFOCUS_RID)) {
                "未指定" -> null
                else -> stringValue
            }

        rpcEnabled = intent.getBooleanExtra(EXTRA_RPC_ENABLED, false)
        spotlightEnabled = intent.getBooleanExtra(EXTRA_SPOTLIGHT_ENABLED, true)

        ui =
            RpcChatActivityUI(
                activity = this,
                channelName = channelName,
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
                detachResolutionMonitor()
                tryAttachResolutionMonitor()
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
                if (cameraHardMuted) {
                    cameraState = CameraState.HARD_MUTED
                    ui?.showCameraOffButton()
                } else if (cameraSoftMuted) {
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
                    handler = mainHandler,
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
                    simulcast = true,
                    simulcastRequestRid = simulcastRequestRidEnum,
                    listener = channelListener,
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
                if (videoEnabled) {
                    channel.setCameraHardMuted(false)
                    channel.setCameraSoftMuted(false)
                }
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
                    mainHandler.postDelayed(resolutionMonitorRetryRunnable, 100L)
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
            mainHandler.removeCallbacks(resolutionMonitorRetryRunnable)
            resolutionMonitorRetryCount = 0
            resolutionMonitorSink = null
            remoteVideoTrack = null
            lastResolutionWidth = null
            lastResolutionHeight = null
        }
    }

    private fun formatRpcResult(result: SoraVideoChannel.RpcCallResult): String =
        when (result) {
            is SoraVideoChannel.RpcCallResult.Success -> {
                if (result.result.isNullOrBlank()) {
                    "Success"
                } else {
                    "Success: ${result.result}"
                }
            }
            is SoraVideoChannel.RpcCallResult.Error -> "Error: ${result.message}"
        }

    internal fun updateRemoteResolution(
        width: Int,
        height: Int,
    ) {
        mainHandler.post {
            if (lastResolutionWidth != width || lastResolutionHeight != height) {
                lastResolutionWidth = width
                lastResolutionHeight = height
                ui?.updateResolutionDisplay("Resolution: $width x $height")
            }
        }
    }

    internal fun handleRequestSimulcastRid(rid: String) {
        val channel = channel ?: return
        lifecycleScope.launch {
            try {
                // リクエスト情報を表示
                val requestLog = "REQUEST:\nmethod: requestSimulcastRid\nrid: $rid\n"
                ui?.appendRpcLog(requestLog)

                val result =
                    withContext(Dispatchers.IO) {
                        channel.requestSimulcastRid(rid)
                    }
                val resultText = formatRpcResult(result)

                // レスポンス情報を表示
                val responseLog = "RESPONSE:\nresult: $resultText\n"
                ui?.appendRpcLog(responseLog)
                ui?.showToastOnUI("Simulcast RID リクエスト: $rid -> $resultText")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request simulcast rid", e)
                val errorLog = "ERROR:\nmessage: ${e.message}\n"
                ui?.appendRpcLog(errorLog)
                ui?.showToastOnUI("Simulcast RID リクエスト失敗: ${e.message}")
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
                ui?.appendRpcLog(requestLog)

                val result =
                    withContext(Dispatchers.IO) {
                        channel.requestSpotlightRid(focusRid, unfocusRid)
                    }
                val resultText = formatRpcResult(result)

                // レスポンス情報を表示
                val responseLog = "RESPONSE:\nresult: $resultText\n"
                ui?.appendRpcLog(responseLog)
                ui?.showToastOnUI(
                    "Spotlight RID リクエスト: focus=$focusRid, unfocus=$unfocusRid -> $resultText",
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request spotlight rid", e)
                val errorLog = "ERROR:\nmessage: ${e.message}\n"
                ui?.appendRpcLog(errorLog)
                ui?.showToastOnUI("Spotlight RID リクエスト失敗: ${e.message}")
            }
        }
    }

    internal fun handleResetSpotlightRid() {
        val channel = channel ?: return
        lifecycleScope.launch {
            try {
                // リクエスト情報を表示
                val requestLog = "REQUEST:\nmethod: resetSpotlightRid\n"
                ui?.appendRpcLog(requestLog)

                val result =
                    withContext(Dispatchers.IO) {
                        channel.resetSpotlightRid()
                    }
                val resultText = formatRpcResult(result)

                // レスポンス情報を表示
                val responseLog = "RESPONSE:\nresult: $resultText\n"
                ui?.appendRpcLog(responseLog)
                ui?.showToastOnUI("Spotlight RID リセット -> $resultText")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset spotlight rid", e)
                val errorLog = "ERROR:\nmessage: ${e.message}\n"
                ui?.appendRpcLog(errorLog)
                ui?.showToastOnUI("Spotlight RID リセット失敗: ${e.message}")
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
                ui?.appendRpcLog(requestLog)

                val result =
                    withContext(Dispatchers.IO) {
                        channel.putSignalingNotifyMetadata(metadataJson, push)
                    }
                val resultText = formatRpcResult(result)
                val responseLog = "RESPONSE: $resultText"
                ui?.appendRpcLog(responseLog)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to put metadata", e)
                val errorLog = "ERROR: ${e.message}"
                ui?.appendRpcLog(errorLog)
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
                ui?.appendRpcLog(requestLog)

                val result =
                    withContext(Dispatchers.IO) {
                        channel.putSignalingNotifyMetadataItem(key, valueJson, push)
                    }
                val resultText = formatRpcResult(result)
                val responseLog = "RESPONSE: $resultText"
                ui?.appendRpcLog(responseLog)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to put metadata item", e)
                val errorLog = "ERROR: ${e.message}"
                ui?.appendRpcLog(errorLog)
            }
        }
    }
}

class RpcChatActivityUI(
    private val activity: RpcChatActivity,
    private val channelName: String,
    private val videoViewWidth: Int,
    private val videoViewHeight: Int,
    private val videoViewMargin: Int,
    private val density: Float,
    private val rpcEnabled: Boolean = false,
    private val spotlightEnabled: Boolean = true,
) {
    private val renderersLayoutCalculator: RendererLayoutCalculator
    private var binding: ActivityRpcChatBinding
    private var selectedSimulcastRid: String = "none"
    private var selectedSpotlightFocusRid: String = "none"
    private var selectedSpotlightUnfocusRid: String = "none"
    private val waitingRpcLogText = "Waiting for request..."
    private val maxRpcLogChars = 12000

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

    private fun createDialog(layoutResId: Int): Pair<android.view.View, androidx.appcompat.app.AlertDialog> {
        val dialogView = activity.layoutInflater.inflate(layoutResId, null)
        val dialog =
            androidx.appcompat.app.AlertDialog
                .Builder(activity)
                .setView(dialogView)
                .setCancelable(true)
                .create()
        return dialogView to dialog
    }

    private fun bindCancelButton(
        dialogView: android.view.View,
        cancelButtonId: Int,
        dialog: androidx.appcompat.app.AlertDialog,
    ) {
        dialogView.findViewById<android.widget.Button>(cancelButtonId).setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun setLargeActionButtons(
        dialog: androidx.appcompat.app.AlertDialog,
        vararg buttons: android.widget.Button,
    ) {
        dialog.setOnShowListener {
            buttons.forEach { button ->
                button.minimumHeight = dp2px(80)
            }
        }
    }

    private fun showSimulcastDialog() {
        val (dialogView, dialog) = createDialog(R.layout.dialog_simulcast_rid)

        val noneBtn = dialogView.findViewById<android.widget.Button>(R.id.dialogSimulcastNoneButton)
        val r0Btn = dialogView.findViewById<android.widget.Button>(R.id.dialogSimulcastR0Button)
        val r1Btn = dialogView.findViewById<android.widget.Button>(R.id.dialogSimulcastR1Button)
        val r2Btn = dialogView.findViewById<android.widget.Button>(R.id.dialogSimulcastR2Button)

        val selectionText = dialogView.findViewById<android.widget.TextView>(R.id.dialogSimulcastSelectionText)
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

        bindCancelButton(dialogView, R.id.dialogSimulcastCancelButton, dialog)

        sendBtn.setOnClickListener {
            selectedSimulcastRid = dialogSelectedRid
            activity.handleRequestSimulcastRid(dialogSelectedRid)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showSpotlightDialog() {
        val (dialogView, dialog) = createDialog(R.layout.dialog_spotlight_rid)

        val focusNoneBtn = dialogView.findViewById<android.widget.Button>(R.id.dialogSpotlightFocusNoneButton)
        val focusR0Btn = dialogView.findViewById<android.widget.Button>(R.id.dialogSpotlightFocusR0Button)
        val focusR1Btn = dialogView.findViewById<android.widget.Button>(R.id.dialogSpotlightFocusR1Button)
        val focusR2Btn = dialogView.findViewById<android.widget.Button>(R.id.dialogSpotlightFocusR2Button)

        val unfocusNoneBtn = dialogView.findViewById<android.widget.Button>(R.id.dialogSpotlightUnfocusNoneButton)
        val unfocusR0Btn = dialogView.findViewById<android.widget.Button>(R.id.dialogSpotlightUnfocusR0Button)
        val unfocusR1Btn = dialogView.findViewById<android.widget.Button>(R.id.dialogSpotlightUnfocusR1Button)
        val unfocusR2Btn = dialogView.findViewById<android.widget.Button>(R.id.dialogSpotlightUnfocusR2Button)

        val selectionText = dialogView.findViewById<android.widget.TextView>(R.id.dialogSpotlightSelectionText)
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

        bindCancelButton(dialogView, R.id.dialogSpotlightCancelButton, dialog)

        sendBtn.setOnClickListener {
            selectedSpotlightFocusRid = dialogFocusRid
            selectedSpotlightUnfocusRid = dialogUnfocusRid
            activity.handleRequestSpotlightRid(dialogFocusRid, dialogUnfocusRid)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showMetadataDialog() {
        val (dialogView, dialog) = createDialog(R.layout.dialog_put_signaling_metadata)

        val metadataInput = dialogView.findViewById<android.widget.EditText>(R.id.dialogMetadataValueInput)
        val pushCheckbox = dialogView.findViewById<android.widget.CheckBox>(R.id.dialogMetadataPushCheckbox)
        val sendBtn = dialogView.findViewById<android.widget.Button>(R.id.dialogMetadataSendButton)
        val cancelBtn = dialogView.findViewById<android.widget.Button>(R.id.dialogMetadataCancelButton)

        // サンプル JSON をデフォルト値として設定
        metadataInput.setText("{\"example_key_1\": \"example_value_1\", \"example_key_2\": \"example_value_2\"}")

        setLargeActionButtons(dialog, sendBtn, cancelBtn)
        bindCancelButton(dialogView, R.id.dialogMetadataCancelButton, dialog)

        sendBtn.setOnClickListener {
            val metadata = metadataInput.text.toString().trim()
            val push = pushCheckbox.isChecked
            if (metadata.isNotEmpty()) {
                activity.handlePutSignalingNotifyMetadata(metadata, push)
                dialog.dismiss()
            } else {
                android.widget.Toast
                    .makeText(activity, "Metadata JSON を入力してください", android.widget.Toast.LENGTH_SHORT)
                    .show()
            }
        }

        dialog.show()
    }

    private fun showMetadataItemDialog() {
        val (dialogView, dialog) = createDialog(R.layout.dialog_put_signaling_metadata_item)

        val keyInput = dialogView.findViewById<android.widget.EditText>(R.id.dialogMetadataItemKeyInput)
        val valueInput = dialogView.findViewById<android.widget.EditText>(R.id.dialogMetadataItemValueInput)
        val pushCheckbox = dialogView.findViewById<android.widget.CheckBox>(R.id.dialogMetadataItemPushCheckbox)
        val sendBtn = dialogView.findViewById<android.widget.Button>(R.id.dialogMetadataItemSendButton)
        val cancelBtn = dialogView.findViewById<android.widget.Button>(R.id.dialogMetadataItemCancelButton)

        // サンプル値をデフォルト値として設定
        keyInput.setText("example_key")
        valueInput.setText("\"example_value\"")

        setLargeActionButtons(dialog, sendBtn, cancelBtn)
        bindCancelButton(dialogView, R.id.dialogMetadataItemCancelButton, dialog)

        sendBtn.setOnClickListener {
            val key = keyInput.text.toString().trim()
            val value = valueInput.text.toString().trim()
            val push = pushCheckbox.isChecked
            if (key.isNotEmpty() && value.isNotEmpty()) {
                activity.handlePutSignalingNotifyMetadataItem(key, value, push)
                dialog.dismiss()
            } else {
                android.widget.Toast
                    .makeText(activity, "Key と Value を入力してください", android.widget.Toast.LENGTH_SHORT)
                    .show()
            }
        }

        dialog.show()
    }

    internal fun appendRpcLog(message: String) {
        val currentText = binding.rpcLogText.text.toString()
        val mergedText =
            if (currentText == waitingRpcLogText) {
                message
            } else {
                "$currentText$message"
            }
        val trimmedText =
            if (mergedText.length > maxRpcLogChars) {
                mergedText.takeLast(maxRpcLogChars)
            } else {
                mergedText
            }
        binding.rpcLogText.text = trimmedText
    }

    internal fun changeState(colorCode: String) {
        binding.channelNameText.setBackgroundColor(Color.parseColor(colorCode))
    }

    internal fun addLocalRenderer(renderer: SurfaceViewRenderer) {
        renderer.layoutParams =
            FrameLayout.LayoutParams(dp2px(100), dp2px(100))
        binding.localRendererContainer.addView(renderer)
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
        activity.runOnUiThread {
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
