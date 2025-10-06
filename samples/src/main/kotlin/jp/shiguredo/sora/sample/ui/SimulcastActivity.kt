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
import jp.shiguredo.sora.sample.databinding.ActivitySimulcastBinding
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
import kotlinx.coroutines.withContext
import org.webrtc.SurfaceViewRenderer
import java.util.UUID

class SimulcastActivity : AppCompatActivity() {
    companion object {
        private val TAG = SimulcastActivity::class.simpleName
    }

    private var channelName = ""
    private var videoEnabled = true
    private var videoCodec: SoraVideoOption.Codec = SoraVideoOption.Codec.DEFAULT
    private var audioCodec: SoraAudioOption.Codec = SoraAudioOption.Codec.DEFAULT
    private var audioEnabled = true
    private var audioBitRate: Int? = null
    private var audioStereo: Boolean = false
    private var videoBitRate: Int? = null
    private var videoWidth: Int = SoraVideoOption.FrameSize.Portrait.VGA.x
    private var videoHeight: Int = SoraVideoOption.FrameSize.Portrait.VGA.y
    private var startWithCamera: Boolean = true
    private var spotlight = false
    private var spotlightNumber: Int? = null
    private var spotlightFocusRid: SoraVideoOption.SpotlightRid? = null
    private var spotlightUnfocusRid: SoraVideoOption.SpotlightRid? = null
    private var simulcastEnabled: Boolean = true
    private var fps: Int = 30
    private var degradationPreference: SoraVideoOption.DegradationPreference? = null
    private var resolutionAdjustment: SoraVideoOption.ResolutionAdjustment? = null
    private var simulcastRid: SoraVideoOption.SimulcastRid? = null
    private var clientId: String? = null
    private var bundleId: String? = null
    private var dataChannelSignaling: Boolean? = null
    private var ignoreDisconnectWebSocket: Boolean? = null

    private var oldAudioMode: Int = AudioManager.MODE_NORMAL

    private var role = SoraRoleType.SENDRECV

    private var ui: SimulcastActivityUI? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        SoraLogger.d(TAG, "onConfigurationChanged: orientation=${newConfig.orientation}")
        super.onConfigurationChanged(newConfig)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setupWindow()

        channelName = intent.getStringExtra("CHANNEL_NAME") ?: getString(R.string.channelId)

        videoEnabled =
            when (intent.getStringExtra("VIDEO_ENABLED")) {
                "有効" -> true
                "無効" -> false
                else -> true
            }

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
            SoraFrameSize.all[key]?.let { p ->
                videoWidth = p.x
                videoHeight = p.y
            }
        }

        spotlight =
            when (intent.getStringExtra("SPOTLIGHT")) {
                "有効" -> true
                else -> false
            }

        spotlightNumber =
            when (val stringValue = intent.getStringExtra("SPOTLIGHT_NUMBER")) {
                "未指定" -> null
                else -> stringValue?.toInt()
            }

        spotlightFocusRid =
            when (intent.getStringExtra("SPOTLIGHT_FOCUS_RID")) {
                "none" -> SoraVideoOption.SpotlightRid.NONE
                "r0" -> SoraVideoOption.SpotlightRid.R0
                "r1" -> SoraVideoOption.SpotlightRid.R1
                "r2" -> SoraVideoOption.SpotlightRid.R2
                else -> null
            }

        spotlightUnfocusRid =
            when (intent.getStringExtra("SPOTLIGHT_UNFOCUS_RID")) {
                "none" -> SoraVideoOption.SpotlightRid.NONE
                "r0" -> SoraVideoOption.SpotlightRid.R0
                "r1" -> SoraVideoOption.SpotlightRid.R1
                "r2" -> SoraVideoOption.SpotlightRid.R2
                else -> null
            }

        simulcastEnabled =
            when (intent.getStringExtra("SIMULCAST_ENABLED")) {
                "有効" -> true
                "無効" -> false
                else -> true
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

        audioStereo =
            when (intent.getStringExtra("AUDIO_STEREO")) {
                "モノラル" -> false
                "ステレオ" -> true
                else -> false
            }

        // ステレオでは landscape にしたほうが内蔵マイクを使うときに自然な向きとなる。
        if (audioStereo) {
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }

        simulcastRid =
            when (intent.getStringExtra("SIMULCAST_RID")) {
                "r0" -> SoraVideoOption.SimulcastRid.R0
                "r1" -> SoraVideoOption.SimulcastRid.R1
                "r2" -> SoraVideoOption.SimulcastRid.R2
                else -> null
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

        ui =
            SimulcastActivityUI(
                activity = this,
                channelName = channelName,
                resources = resources,
                videoViewWidth = 100,
                videoViewHeight = 100,
                videoViewMargin = 10,
                density = this.resources.displayMetrics.density,
            )

        // 初期表示を反映（接続直後のコールバック前にアイコン状態を整える）
        if (videoEnabled && startWithCamera) {
            cameraState = CameraState.ON
            ui?.showCameraOnButton()
        } else {
            cameraState = CameraState.HARD_MUTED
            ui?.showCameraOffButton()
        }

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
        val audioManager =
            applicationContext.getSystemService(Context.AUDIO_SERVICE)
                as AudioManager
        oldAudioMode = audioManager.mode
        Log.d(TAG, "AudioManager mode change: $oldAudioMode => MODE_IN_COMMUNICATION(3)")
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        Log.d(TAG, "AudioManager.isMicrophoneMute=${audioManager.isMicrophoneMute}")
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        val audioManager =
            applicationContext.getSystemService(Context.AUDIO_SERVICE)
                as AudioManager
        Log.d(TAG, "AudioManager mode change: MODE_IN_COMMUNICATION(3) => $oldAudioMode")
        audioManager.mode = oldAudioMode
        close()
    }

    internal fun close() {
        Log.d(TAG, "close")
        disconnectChannel()
        finish()
    }

    private var channel: SoraVideoChannel? = null
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
                ui?.changeState("#DD2C00")
                Toast.makeText(this@SimulcastActivity, "Error: ${reason.name}", Toast.LENGTH_LONG).show()
                close()
            }

            override fun onWarning(
                channel: SoraVideoChannel,
                reason: SoraErrorReason,
            ) {
                Toast.makeText(this@SimulcastActivity, "Error: ${reason.name}", Toast.LENGTH_LONG).show()
            }

            override fun onAddLocalRenderer(
                channel: SoraVideoChannel,
                renderer: SurfaceViewRenderer,
            ) {
                ui?.addLocalRenderer(renderer)
            }

            override fun onAddRemoteRenderer(
                channel: SoraVideoChannel,
                renderer: SurfaceViewRenderer,
            ) {
                ui?.addRenderer(renderer)
            }

            override fun onRemoveRemoteRenderer(
                channel: SoraVideoChannel,
                renderer: SurfaceViewRenderer,
            ) {
                ui?.removeRenderer(renderer)
            }

            override fun onAttendeesCountUpdated(
                channel: SoraVideoChannel,
                attendees: ChannelAttendeesCount,
            ) {
                // nop
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
                    ui?.showCameraSoftOffButton()
                } else {
                    cameraState = CameraState.ON
                    ui?.showCameraOnButton()
                }
            }
        }

    private fun connectChannel() {
        Log.d(TAG, "openChannel")
        val signalingEndpointCandidates = BuildConfig.SIGNALING_ENDPOINT.split(",").map { it.trim() }
        val signalingMetadata = Gson().fromJson(BuildConfig.SIGNALING_METADATA, Map::class.java)
        channel =
            SoraVideoChannel(
                context = this,
                handler = Handler(Looper.getMainLooper()),
                signalingEndpointCandidates = signalingEndpointCandidates,
                channelId = channelName,
                clientId = clientId,
                bundleId = bundleId,
                dataChannelSignaling = dataChannelSignaling,
                ignoreDisconnectWebSocket = ignoreDisconnectWebSocket,
                signalingMetadata = signalingMetadata,
                spotlight = spotlight,
                spotlightNumber = spotlightNumber,
                spotlightFocusRid = spotlightFocusRid,
                spotlightUnfocusRid = spotlightUnfocusRid,
                videoEnabled = videoEnabled,
                startWithCamera = startWithCamera,
                videoWidth = videoWidth,
                videoHeight = videoHeight,
                simulcast = simulcastEnabled,
                simulcastRid = simulcastRid,
                videoFPS = fps,
                degradationPreference = degradationPreference,
                resolutionAdjustment = resolutionAdjustment,
                videoCodec = videoCodec,
                videoBitRate = videoBitRate,
                audioEnabled = audioEnabled,
                audioCodec = audioCodec,
                audioBitRate = audioBitRate,
                audioStereo = audioStereo,
                roleType = role,
                listener = channelListener,
            )
        channel!!.connect()
    }

    private fun disconnectChannel() {
        Log.d(TAG, "disconnectChannel")
        channel?.dispose()
    }

    internal fun switchCamera() {
        channel?.switchCamera()
    }

    private val micMuteController by lazy {
        MicMuteController(
            scope = lifecycleScope,
            setSoftMute = { mute -> channel?.mute(mute) },
            setHardMute = { muted -> setAudioHardMuted(muted) },
            showMicOn = { ui?.showMicOnButton() },
            showMicSoft = { ui?.showMicSoftMuteButton() },
            showMicHard = { ui?.showMicHardMuteButton() },
            log = { message -> Log.d(TAG, message) },
        )
    }

    private enum class CameraState { ON, SOFT_MUTED, HARD_MUTED }

    private var cameraState: CameraState = CameraState.ON

    internal fun toggleMuted() {
        micMuteController.toggleMuted()
    }

    private suspend fun setAudioHardMuted(muted: Boolean): Boolean =
        withContext(Dispatchers.Default) {
            runCatching { channel?.setAudioHardMutedAsync(muted) ?: true }
                .onFailure { Log.e(TAG, "setAudioHardMutedAsync failed", it) }
                .getOrElse { false }
        }

    internal fun toggleCamera() {
        // UI 更新は onCameraMuteStateChanged で行う
        when (cameraState) {
            CameraState.ON -> {
                // ON -> ソフトウェアミュート
                channel?.setCameraSoftMuted(true)
            }
            CameraState.SOFT_MUTED -> {
                // ソフトウェアミュート -> ハードウェアミュート
                channel?.setCameraHardMuted(true)
            }
            CameraState.HARD_MUTED -> {
                // ハードウェアミュート -> ON
                channel?.setCameraHardMuted(false)
                channel?.setCameraSoftMuted(false)
            }
        }
    }
}

class SimulcastActivityUI(
    val activity: SimulcastActivity,
    val channelName: String,
    val resources: Resources,
    val videoViewWidth: Int,
    val videoViewHeight: Int,
    val videoViewMargin: Int,
    val density: Float,
) {
    private val renderersLayoutCalculator: RendererLayoutCalculator

    private var binding: ActivitySimulcastBinding

    init {
        binding = ActivitySimulcastBinding.inflate(activity.layoutInflater)
        activity.setContentView(binding.root)
        binding.channelNameText.text = channelName
        this.renderersLayoutCalculator =
            RendererLayoutCalculator(
                width = SoraScreenUtil.size(activity).x - dp2px(20 * 2),
                height = SoraScreenUtil.size(activity).y - dp2px(20 * 2 + 100),
            )
        binding.toggleMuteButton.setOnClickListener { activity.toggleMuted() }
        binding.toggleCameraButton.setOnClickListener { activity.toggleCamera() }
        binding.switchCameraButton.setOnClickListener { activity.switchCamera() }
        binding.closeButton.setOnClickListener { activity.close() }
        // 初期表示はマイク ON
        showMicOnButton()
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

    internal fun showMicOnButton() {
        binding.toggleMuteButton.setImageDrawable(
            resources.getDrawable(R.drawable.ic_mic_white_48dp, null),
        )
    }

    internal fun showMicSoftMuteButton() {
        binding.toggleMuteButton.setImageDrawable(
            resources.getDrawable(R.drawable.ic_mic_off_white_48dp, null),
        )
    }

    internal fun showMicHardMuteButton() {
        binding.toggleMuteButton.setImageDrawable(
            resources.getDrawable(R.drawable.ic_mic_off_black_48dp, null),
        )
    }

    internal fun showCameraOffButton() {
        binding.toggleCameraButton.setImageDrawable(
            resources.getDrawable(R.drawable.ic_videocam_off_black_48dp, null),
        )
    }

    internal fun showCameraSoftOffButton() {
        binding.toggleCameraButton.setImageDrawable(
            resources.getDrawable(R.drawable.ic_videocam_off_white_48dp, null),
        )
    }

    internal fun showCameraOnButton() {
        binding.toggleCameraButton.setImageDrawable(
            resources.getDrawable(R.drawable.ic_videocam_on_white_48dp, null),
        )
    }

    private fun dp2px(d: Int): Int = (density * d).toInt()

    private fun rendererLayoutParams(): RelativeLayout.LayoutParams {
        val params = RelativeLayout.LayoutParams(dp2px(videoViewWidth), dp2px(videoViewHeight))
        val margin = dp2px(videoViewMargin)
        params.setMargins(margin, margin, margin, margin)
        return params
    }
}
