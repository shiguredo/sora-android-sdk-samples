package jp.shiguredo.sora.sample.ui

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import jp.shiguredo.sora.sample.BuildConfig
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.facade.SoraVideoChannel
import jp.shiguredo.sora.sample.option.SoraStreamType
import jp.shiguredo.sora.sample.ui.util.RendererLayoutCalculator
import jp.shiguredo.sora.sample.ui.util.SoraScreenUtil
import jp.shiguredo.sora.sdk.channel.data.ChannelAttendeesCount
import jp.shiguredo.sora.sdk.channel.option.SoraAudioOption
import jp.shiguredo.sora.sdk.channel.option.SoraVideoOption
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import kotlinx.android.synthetic.main.activity_video_chat_room.*
import org.webrtc.PeerConnection
import org.webrtc.SurfaceViewRenderer
import java.util.*

class VideoChatRoomActivity : AppCompatActivity() {

    companion object {
        val TAG = VideoChatRoomActivity::class.simpleName
    }

    private var channelName = ""
    private var spotlight = 0
    private var videoEnabled = true
    private var videoCodec:  SoraVideoOption.Codec = SoraVideoOption.Codec.VP9
    private var audioCodec:  SoraAudioOption.Codec = SoraAudioOption.Codec.OPUS
    private var audioEnabled = true
    private var bitRate: Int? = null
    private var videoWidth: Int = SoraVideoOption.FrameSize.Portrait.VGA.x
    private var videoHeight: Int = SoraVideoOption.FrameSize.Portrait.VGA.y
    private var simulcast = false
    private var fps: Int = 30
    private var fixedResolution = false
    private var clientId: String? = null
    private var sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

    private var oldAudioMode: Int = AudioManager.MODE_INVALID

    private var streamType = SoraStreamType.BIDIRECTIONAL

    private var ui: VideoChatRoomActivityUI? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setupWindow()

        channelName = intent.getStringExtra("CHANNEL_NAME") ?: "shino"

        spotlight = intent.getIntExtra("SPOTLIGHT", 0)

        videoEnabled = when (intent.getStringExtra("VIDEO_ENABLED")) {
            "YES" -> true
            "NO"  -> false
            else  -> true
        }

        videoCodec = SoraVideoOption.Codec.valueOf(intent.getStringExtra("VIDEO_CODEC") ?: "VP8")

        audioCodec = SoraAudioOption.Codec.valueOf(intent.getStringExtra("AUDIO_CODEC") ?: "OPUS")

        streamType = when (intent.getStringExtra("STREAM_TYPE")) {
            "BIDIRECTIONAL" -> SoraStreamType.BIDIRECTIONAL
            "SINGLE-UP"     -> SoraStreamType.SINGLE_UP
            "SINGLE-DOWN"   -> SoraStreamType.SINGLE_DOWN
            "MULTI-DOWN"    -> SoraStreamType.MULTI_DOWN
            else            -> SoraStreamType.SINGLE_UP
        }

        audioEnabled = when (intent.getStringExtra("AUDIO_ENABLED")) {
            "YES" -> true
            "NO"  -> false
            else  -> true
        }

        fps = (intent.getStringExtra("FPS") ?: "30").toInt()

        var videoSize = when (intent.getStringExtra("VIDEO_SIZE")) {
            // Portrait
            "VGA"          -> SoraVideoOption.FrameSize.Portrait.VGA
            "QQVGA"        -> SoraVideoOption.FrameSize.Portrait.QQVGA
            "QCIF"         -> SoraVideoOption.FrameSize.Portrait.QCIF
            "HQVGA"        -> SoraVideoOption.FrameSize.Portrait.HQVGA
            "QVGA"         -> SoraVideoOption.FrameSize.Portrait.QVGA
            "HD"           -> SoraVideoOption.FrameSize.Portrait.HD
            "FHD"          -> SoraVideoOption.FrameSize.Portrait.FHD
            "Res1920x3840" -> SoraVideoOption.FrameSize.Portrait.Res1920x3840
            "UHD2160x3840" -> SoraVideoOption.FrameSize.Portrait.UHD2160x3840
            "UHD2160x4096" -> SoraVideoOption.FrameSize.Portrait.UHD2160x4096
            // Landscape
            "Res3840x1920" -> SoraVideoOption.FrameSize.Landscape.Res3840x1920
            "UHD3840x2160" -> SoraVideoOption.FrameSize.Landscape.UHD3840x2160
            // Default
            else           -> SoraVideoOption.FrameSize.Portrait.HD
        }
        videoWidth = videoSize.x
        videoHeight = videoSize.y

        simulcast = when (intent.getStringExtra("SIMULCAST")) {
            "ENABLED" -> true
            else      -> false
        }

        fixedResolution = when (intent.getStringExtra("RESOLUTION_CHANGE")) {
            "VARIABLE" -> false
            "FIXED"    -> true
            else       -> false
        }

        bitRate = when (intent.getStringExtra("BITRATE")) {
            "UNDEFINED" -> null
            else -> (intent.getStringExtra("BITRATE") ?: "5000").toInt()
        }

        clientId = when (intent.getStringExtra("CLIENT_ID")) {
            "NONE"        -> null
            "BUILD MODEL" -> Build.MODEL
            "時雨堂"      -> "🍖時雨堂🍗"
            "RANDOM UUID" -> UUID.randomUUID().toString()
            else -> null
        }
        sdpSemantics = when (intent.getStringExtra("SDP_SEMANTICS")) {
            "Unified Plan" -> PeerConnection.SdpSemantics.UNIFIED_PLAN
            "Plan B"       -> PeerConnection.SdpSemantics.PLAN_B
            else           -> PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        ui = VideoChatRoomActivityUI(
                activity        = this,
                channelName     = channelName,
                resources       = resources,
                videoViewWidth  = 100,
                videoViewHeight = 100,
                videoViewMargin = 10,
                density         = this.resources.displayMetrics.density
        )

        connectChannel()
    }

    private fun setupWindow() {
        supportActionBar?.hide()

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
        Log.d(TAG, "AudioManager mode change: ${oldAudioMode} => MODE_IN_COMMUNICATION(3)")
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE)
                as AudioManager
        Log.d(TAG, "AudioManager mode change: MODE_IN_COMMUNICATION(3) => ${oldAudioMode}")
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
            close()
        }

        override fun onAddLocalRenderer(channel: SoraVideoChannel, renderer: SurfaceViewRenderer) {
            ui?.addLocalRenderer(renderer)
        }

        override fun onAddRemoteRenderer(channel: SoraVideoChannel, renderer: SurfaceViewRenderer) {
            ui?.addRenderer(renderer)
        }

        override fun onRemoveRemoteRenderer(channel: SoraVideoChannel, renderer: SurfaceViewRenderer) {
            ui?.removeRenderer(renderer)
        }

        override fun onAttendeesCountUpdated(channel: SoraVideoChannel, attendees: ChannelAttendeesCount) {
            // nop
        }
    }

    private fun connectChannel() {
        Log.d(TAG, "openChannel")

        channel = SoraVideoChannel(
                context           = this,
                handler           = Handler(),
                signalingEndpoint = BuildConfig.SIGNALING_ENDPOINT,
                channelId         = channelName,
                signalingMetadata = "",
                spotlight         = spotlight,
                videoEnabled      = videoEnabled,
                videoWidth        = videoWidth,
                videoHeight       = videoHeight,
                simulcast         = simulcast,
                videoFPS          = fps,
                fixedResolution   = fixedResolution,
                videoCodec        = videoCodec,
                videoBitrate      = bitRate,
                audioEnabled      = audioEnabled,
                audioCodec        = audioCodec,
                sdpSemantics      = sdpSemantics,
                streamType        = streamType,
                clientId          = clientId,
                listener          = channelListener,
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

}

class VideoChatRoomActivityUI(
        val activity:        VideoChatRoomActivity,
        val channelName:     String,
        val resources:       Resources,
        val videoViewWidth:  Int,
        val videoViewHeight: Int,
        val videoViewMargin: Int,
        val density:         Float
) {

    private val renderersLayoutCalculator: RendererLayoutCalculator

    init {
        activity.setContentView(R.layout.activity_video_chat_room)
        activity.channelNameText.text = channelName
        this.renderersLayoutCalculator = RendererLayoutCalculator(
                width = SoraScreenUtil.size(activity).x - dp2px(20 * 2),
                height = SoraScreenUtil.size(activity).y - dp2px(20 * 2 + 100)
        )
        activity.toggleMuteButton.setOnClickListener { activity.toggleMuted() }
        activity.switchCameraButton.setOnClickListener { activity.switchCamera() }
        activity.closeButton.setOnClickListener { activity.close() }
    }

    internal fun changeState(colorCode: String) {
        activity.channelNameText.setBackgroundColor(Color.parseColor(colorCode))
    }

    internal fun addLocalRenderer(renderer: SurfaceViewRenderer) {
        renderer.layoutParams =
                FrameLayout.LayoutParams(dp2px(100), dp2px(100))
        activity.localRendererContainer.addView(renderer)
        renderer.setMirror(true)
    }

    internal fun addRenderer(renderer: SurfaceViewRenderer) {
        renderer.layoutParams = rendererLayoutParams()
        activity.rendererContainer.addView(renderer)
        renderersLayoutCalculator.add(renderer)
    }

    internal fun removeRenderer(renderer: SurfaceViewRenderer) {
        activity.rendererContainer.removeView(renderer)
        renderersLayoutCalculator.remove(renderer)
    }

    internal fun showUnmuteButton() {
        activity.toggleMuteButton.setImageDrawable(
                resources.getDrawable(R.drawable.ic_mic_white_48dp, null))
    }

    internal fun showMuteButton() {
        activity.toggleMuteButton.setImageDrawable(
                resources.getDrawable(R.drawable.ic_mic_off_black_48dp, null))
    }

    private fun dp2px(d: Int): Int = (density * d).toInt()

    private fun rendererLayoutParams(): RelativeLayout.LayoutParams {
        val params = RelativeLayout.LayoutParams(dp2px(videoViewWidth), dp2px(videoViewHeight))
        val margin = dp2px(videoViewMargin)
        params.setMargins(margin, margin, margin, margin)
        return params
    }
}
