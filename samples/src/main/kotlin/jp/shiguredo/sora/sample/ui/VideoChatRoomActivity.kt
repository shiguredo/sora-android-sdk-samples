package jp.shiguredo.sora.sample.ui

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import jp.shiguredo.sora.sample.BuildConfig
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.databinding.ActivityVideoChatRoomBinding
import jp.shiguredo.sora.sample.facade.SoraVideoChannel
import jp.shiguredo.sora.sample.option.SoraFrameSize
import jp.shiguredo.sora.sample.option.SoraRoleType
import jp.shiguredo.sora.sample.ui.util.RendererLayoutCalculator
import jp.shiguredo.sora.sample.ui.util.SoraScreenUtil
import jp.shiguredo.sora.sdk.channel.data.ChannelAttendeesCount
import jp.shiguredo.sora.sdk.channel.option.SoraAudioOption
import jp.shiguredo.sora.sdk.channel.option.SoraVideoOption
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import jp.shiguredo.sora.sdk.util.SoraLogger
import org.webrtc.SurfaceViewRenderer
import java.util.UUID

class VideoChatRoomActivity : AppCompatActivity() {

    companion object {
        private val TAG = VideoChatRoomActivity::class.simpleName
    }

    private var channelName = ""
    private var videoEnabled = true
    private var videoCodec: SoraVideoOption.Codec = SoraVideoOption.Codec.VP9
    private var audioCodec: SoraAudioOption.Codec = SoraAudioOption.Codec.OPUS
    private var audioEnabled = true
    private var audioBitRate: Int? = null
    private var audioStereo: Boolean = false
    private var videoBitRate: Int? = null
    private var videoWidth: Int = SoraVideoOption.FrameSize.Portrait.VGA.x
    private var videoHeight: Int = SoraVideoOption.FrameSize.Portrait.VGA.y
    private var videoVp9Params: Any? = null
    private var videoAv1Params: Any? = null
    private var videoH264Params: Any? = null
    private var multistream = true
    private var spotlight = false
    private var spotlightNumber: Int? = null
    private var fps: Int = 30
    private var fixedResolution = false
    private var resolutionAdjustment: SoraVideoOption.ResolutionAdjustment? = null
    private var cameraFacing = true
    private var clientId: String? = null
    private var bundleId: String? = null
    private var dataChannelSignaling: Boolean? = null
    private var ignoreDisconnectWebSocket: Boolean? = null
    private var audioStreamingLanguageCode: String? = null

    private var oldAudioMode: Int = AudioManager.MODE_NORMAL

    private var role = SoraRoleType.SENDRECV

    private var ui: VideoChatRoomActivityUI? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        SoraLogger.d(TAG, "onConfigurationChanged: orientation=${newConfig.orientation}")
        super.onConfigurationChanged(newConfig)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setupWindow()

        channelName = intent.getStringExtra("CHANNEL_NAME") ?: getString(R.string.channelId)

        videoEnabled = when (intent.getStringExtra("VIDEO_ENABLED")) {
            "有効" -> true
            "無効" -> false
            else -> true
        }

        videoCodec = SoraVideoOption.Codec.valueOf(intent.getStringExtra("VIDEO_CODEC") ?: "VP9")

        audioCodec = SoraAudioOption.Codec.valueOf(intent.getStringExtra("AUDIO_CODEC") ?: "OPUS")

        role = when (intent.getStringExtra("ROLE")) {
            "SENDONLY" -> SoraRoleType.SENDONLY
            "RECVONLY" -> SoraRoleType.RECVONLY
            "SENDRECV" -> SoraRoleType.SENDRECV
            else -> SoraRoleType.SENDRECV
        }

        audioEnabled = when (intent.getStringExtra("AUDIO_ENABLED")) {
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

        multistream = when (intent.getStringExtra("MULTISTREAM")) {
            "有効" -> true
            else -> false
        }

        spotlight = when (intent.getStringExtra("SPOTLIGHT")) {
            "有効" -> true
            else -> false
        }

        spotlightNumber = when (val stringValue = intent.getStringExtra("SPOTLIGHT_NUMBER")) {
            "未指定" -> null
            else -> stringValue?.toInt()
        }

        Log.d(TAG, "spotlight => $spotlight, $spotlightNumber")

        fixedResolution = when (intent.getStringExtra("RESOLUTION_CHANGE")) {
            "可変" -> false
            "固定" -> true
            else -> false
        }

        videoVp9Params = when (val stringValue = intent.getStringExtra("VP9_PROFILE_ID")) {
            "未指定" -> null
            else -> object {
                var profile_id: Int? = stringValue?.toIntOrNull()
            }
        }

        videoAv1Params = when (val stringValue = intent.getStringExtra("AV1_PROFILE")) {
            "未指定" -> null
            else -> object {
                var profile: Int? = stringValue?.toIntOrNull()
            }
        }

        videoH264Params = when (val stringValue = intent.getStringExtra("H264_PROFILE_LEVEL_ID")) {
            "未指定" -> null
            else -> object {
                var profile_level_id: String? = stringValue
            }
        }
        resolutionAdjustment = when (intent.getStringExtra("RESOLUTION_ADJUSTMENT")) {
            "16" -> SoraVideoOption.ResolutionAdjustment.MULTIPLE_OF_16
            "8" -> SoraVideoOption.ResolutionAdjustment.MULTIPLE_OF_8
            "4" -> SoraVideoOption.ResolutionAdjustment.MULTIPLE_OF_4
            "2" -> SoraVideoOption.ResolutionAdjustment.MULTIPLE_OF_2
            "無効" -> SoraVideoOption.ResolutionAdjustment.NONE
            else -> null
        }

        videoBitRate = when (val stringValue = intent.getStringExtra("VIDEO_BIT_RATE")) {
            "未指定" -> null
            else -> stringValue?.toInt()
        }

        audioBitRate = when (val stringValue = intent.getStringExtra("AUDIO_BIT_RATE")) {
            "未指定" -> null
            else -> stringValue?.toInt()
        }

        audioStereo = when (intent.getStringExtra("AUDIO_STEREO")) {
            "モノラル" -> false
            "ステレオ" -> true
            else -> false
        }

        cameraFacing = when (intent.getStringExtra("CAMERA_FACING")) {
            "前面" -> true
            "背面" -> false
            else -> true
        }

        clientId = when (intent.getStringExtra("CLIENT_ID")) {
            "なし" -> null
            "端末情報" -> Build.MODEL
            "時雨堂" -> "🍖時雨堂🍗"
            "ランダム" -> UUID.randomUUID().toString()
            else -> null
        }

        bundleId = when (intent.getStringExtra("BUNDLE_ID")) {
            "なし" -> null
            "端末情報" -> Build.MODEL
            "時雨堂" -> "☔時雨堂🌂"
            "ランダム" -> UUID.randomUUID().toString()
            else -> null
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

        audioStreamingLanguageCode = when (intent.getStringExtra("AUDIO_STREAMING_LANGUAGE_CODE")) {
            "ja-JP" -> "ja-JP"
            "en-US" -> "en-US"
            "未指定" -> null
            else -> null
        }

        // ステレオでは landscape にしたほうが内蔵マイクを使うときに自然な向きとなる。
        if (audioStereo) {
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }

        ui = VideoChatRoomActivityUI(
            activity = this,
            channelName = channelName,
            resources = resources,
            videoViewWidth = 100,
            videoViewHeight = 100,
            videoViewMargin = 10,
            density = this.resources.displayMetrics.density
        )

        connectChannel()
    }

    private fun setupWindow() {
        supportActionBar?.hide()

        window.addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN
                or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        setWindowVisibility()
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun setWindowVisibility() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
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
        disconnectChannel()
        finish()
    }

    private var channel: SoraVideoChannel? = null
    private var channelListener: SoraVideoChannel.Listener = object : SoraVideoChannel.Listener {

        override fun onConnect(channel: SoraVideoChannel) {
            ui?.changeState("#00C853")
        }

        override fun onClose(channel: SoraVideoChannel) {
            ui?.changeState("#37474F")
            close()
        }

        override fun onError(channel: SoraVideoChannel, reason: SoraErrorReason) {
            ui?.changeState("#DD2C00")
            Toast.makeText(this@VideoChatRoomActivity, "Error: ${reason.name}", Toast.LENGTH_LONG).show()
            close()
        }

        override fun onWarning(channel: SoraVideoChannel, reason: SoraErrorReason) {
            Toast.makeText(this@VideoChatRoomActivity, "Error: ${reason.name}", Toast.LENGTH_LONG).show()
        }

        override fun onAddLocalRenderer(channel: SoraVideoChannel, renderer: SurfaceViewRenderer) {
            ui?.addLocalRenderer(renderer)
        }

        override fun onAddRemoteRenderer(channel: SoraVideoChannel, renderer: SurfaceViewRenderer) {
            ui?.addRenderer(renderer)
            ui?.setMsids(channel.listMsids())
        }

        override fun onRemoveRemoteRenderer(channel: SoraVideoChannel, renderer: SurfaceViewRenderer) {
            ui?.removeRenderer(renderer)
            ui?.setMsids(channel.listMsids())
        }

        override fun onAttendeesCountUpdated(channel: SoraVideoChannel, attendees: ChannelAttendeesCount) {
            // nop
        }
    }

    private fun connectChannel() {
        Log.d(TAG, "openChannel")
        val signalingEndpointCandidates = BuildConfig.SIGNALING_ENDPOINT.split(",").map { it.trim() }
        val signalingMetadata = Gson().fromJson(BuildConfig.SIGNALING_METADATA, Map::class.java)
        channel = SoraVideoChannel(
            context = this,
            handler = Handler(),
            signalingEndpointCandidates = signalingEndpointCandidates,
            channelId = channelName,
            signalingMetadata = signalingMetadata,
            dataChannelSignaling = dataChannelSignaling,
            ignoreDisconnectWebSocket = ignoreDisconnectWebSocket,
            spotlight = spotlight,
            spotlightNumber = spotlightNumber,
            videoEnabled = videoEnabled,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            videoFPS = fps,
            videoVp9Params = videoVp9Params,
            videoAv1Params = videoAv1Params,
            videoH264Params = videoH264Params,
            fixedResolution = fixedResolution,
            resolutionAdjustment = resolutionAdjustment,
            videoCodec = videoCodec,
            videoBitRate = videoBitRate,
            audioEnabled = audioEnabled,
            audioCodec = audioCodec,
            audioBitRate = audioBitRate,
            audioStereo = audioStereo,
            roleType = role,
            multistream = multistream,
            cameraFacing = cameraFacing,
            clientId = clientId,
            bundleId = bundleId,
            audioStreamingLanguageCode = audioStreamingLanguageCode,
            listener = channelListener,
            needLocalRenderer = true
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

    private var muted = false

    internal fun toggleMuted() {
        if (muted) {
            ui?.showMuteButton()
        } else {
            ui?.showUnmuteButton()
        }
        muted = !muted
        channel?.mute(muted)
    }

    internal fun toggleMutedTest() {
        if (muted) {
            ui?.showMuteButtonTest()
            // 現在選択されている msid (connectionID を内部的には MediaStream id として扱っている) の音量を 3 にする
            // 3 でも変化を感じられたので
            ui?.getSelectedMsid()?.let {
                channel?.setAudioVolume(it, 3.0)
            }
        } else {
            ui?.showUnmuteButtonTest()
            // 現在選択されている msid の音量を 0.0 にする
            ui?.getSelectedMsid()?.let {
                channel?.setAudioVolume(it, 0.5)
            }
        }
        muted = !muted
        channel?.mute(muted)
    }
}

class VideoChatRoomActivityUI(
    val activity: VideoChatRoomActivity,
    val channelName: String,
    val resources: Resources,
    val videoViewWidth: Int,
    val videoViewHeight: Int,
    val videoViewMargin: Int,
    val density: Float
) {

    private val renderersLayoutCalculator: RendererLayoutCalculator
    private var binding: ActivityVideoChatRoomBinding

    init {
        binding = ActivityVideoChatRoomBinding.inflate(activity.layoutInflater)
        activity.setContentView(binding.root)
        binding.channelNameText.text = channelName
        this.renderersLayoutCalculator = RendererLayoutCalculator(
            width = SoraScreenUtil.size(activity).x - dp2px(20 * 2),
            height = SoraScreenUtil.size(activity).y - dp2px(20 * 2 + 100)
        )
        binding.toggleMuteButton.setOnClickListener { activity.toggleMuted() }
        // (仮)setVolume で音量を切り替えるためのボタン
        binding.toggleMuteButtonTest.setOnClickListener { activity.toggleMutedTest() }
        binding.switchCameraButton.setOnClickListener { activity.switchCamera() }
        binding.closeButton.setOnClickListener { activity.close() }
        binding.msidSelection.name.text = "音量調整する connectionID"
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

    internal fun showUnmuteButton() {
        binding.toggleMuteButton.setImageDrawable(
            resources.getDrawable(R.drawable.ic_mic_white_48dp, null)
        )
    }

    internal fun showMuteButton() {
        binding.toggleMuteButton.setImageDrawable(
            resources.getDrawable(R.drawable.ic_mic_off_black_48dp, null)
        )
    }

    internal fun showUnmuteButtonTest() {
        binding.toggleMuteButtonTest.setImageDrawable(
            resources.getDrawable(R.drawable.ic_mic_white_48dp, null)
        )
    }

    internal fun showMuteButtonTest() {
        binding.toggleMuteButtonTest.setImageDrawable(
            resources.getDrawable(R.drawable.ic_mic_off_black_48dp, null)
        )
    }

    /**
     * 音量調整対象の connectionID 一覧を更新する
     */
    internal fun setMsids(msids: List<String>) {
        SoraLogger.d("kensaku", "setMsids: $msids")
        binding.msidSelection.spinner.setItems(msids)
    }

    internal fun getSelectedMsid(): String {
        return binding.msidSelection.spinner.getItems<String>()[binding.msidSelection.spinner.selectedIndex]
    }

    private fun dp2px(d: Int): Int = (density * d).toInt()

    private fun rendererLayoutParams(): RelativeLayout.LayoutParams {
        val params = RelativeLayout.LayoutParams(dp2px(videoViewWidth), dp2px(videoViewHeight))
        val margin = dp2px(videoViewMargin)
        params.setMargins(margin, margin, margin, margin)
        return params
    }
}
