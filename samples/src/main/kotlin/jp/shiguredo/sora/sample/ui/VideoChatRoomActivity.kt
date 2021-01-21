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
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import jp.shiguredo.sora.sample.BuildConfig
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.facade.SoraVideoChannel
import jp.shiguredo.sora.sample.option.SoraRoleType
import jp.shiguredo.sora.sample.ui.util.RendererLayoutCalculator
import jp.shiguredo.sora.sample.ui.util.SoraScreenUtil
import jp.shiguredo.sora.sdk.channel.data.ChannelAttendeesCount
import jp.shiguredo.sora.sdk.channel.option.SoraAudioOption
import jp.shiguredo.sora.sdk.channel.option.SoraSpotlightOption
import jp.shiguredo.sora.sdk.channel.option.SoraVideoOption
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import jp.shiguredo.sora.sdk.util.SoraLogger
import kotlinx.android.synthetic.main.activity_video_chat_room.*
import org.webrtc.SurfaceViewRenderer
import java.util.*

class VideoChatRoomActivity : AppCompatActivity() {

    companion object {
        private val TAG = VideoChatRoomActivity::class.simpleName
    }

    private var channelName = ""
    private var videoEnabled = true
    private var videoCodec:  SoraVideoOption.Codec = SoraVideoOption.Codec.VP9
    private var audioCodec:  SoraAudioOption.Codec = SoraAudioOption.Codec.OPUS
    private var audioEnabled = true
    private var audioBitRate: Int? = null
    private var audioStereo: Boolean = false
    private var videoBitRate: Int? = null
    private var videoWidth: Int = SoraVideoOption.FrameSize.Portrait.VGA.x
    private var videoHeight: Int = SoraVideoOption.FrameSize.Portrait.VGA.y
    private var multistream = true
    private var spotlight = false
    private var spotlightNumber: Int? = null
    private var spotlightLegacy = true
    private var fps: Int = 30
    private var fixedResolution = false
    private var cameraFacing = true
    private var clientId: String? = null

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
            "無効"  -> false
            else  -> true
        }

        videoCodec = SoraVideoOption.Codec.valueOf(intent.getStringExtra("VIDEO_CODEC") ?: "VP9")

        audioCodec = SoraAudioOption.Codec.valueOf(intent.getStringExtra("AUDIO_CODEC") ?: "OPUS")

        role = when (intent.getStringExtra("ROLE")) {
            "SENDONLY" -> SoraRoleType.SENDONLY
            "RECVONLY" -> SoraRoleType.RECVONLY
            "SENDRECV" -> SoraRoleType.SENDRECV
            else       -> SoraRoleType.SENDRECV
        }

        audioEnabled = when (intent.getStringExtra("AUDIO_ENABLED")) {
            "有効" -> true
            "無効"  -> false
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
            else           -> SoraVideoOption.FrameSize.Portrait.VGA
        }
        videoWidth = videoSize.x
        videoHeight = videoSize.y

        multistream = when (intent.getStringExtra("MULTISTREAM")) {
            "有効" -> true
            else      -> false
        }

        spotlight = when (intent.getStringExtra("SPOTLIGHT")) {
            "有効" -> true
            else      -> false
        }

        spotlightNumber = intent.getStringExtra("SPOTLIGHT_NUMBER")?.toInt()

        spotlightLegacy = when (intent.getStringExtra("SPOTLIGHT_LEGACY")) {
            "有効" -> true
            else      -> false
        }

        Log.d(TAG, "spotlight => $spotlight, $spotlightNumber, $spotlightLegacy")

        fixedResolution = when (intent.getStringExtra("RESOLUTION_CHANGE")) {
            "可変" -> false
            "固定"    -> true
            else       -> false
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
            "モノラル"   -> false
            "ステレオ" -> true
            else     -> false
        }

        cameraFacing = when (intent.getStringExtra("CAMERA_FACING")) {
            "前面" -> true
            "背面"  -> false
            else    -> true
        }

        clientId = when (intent.getStringExtra("CLIENT_ID")) {
            "なし"        -> null
            "端末情報" -> Build.MODEL
            "時雨堂"      -> "🍖時雨堂🍗"
            "ランダム" -> UUID.randomUUID().toString()
            else -> null
        }

        // ステレオでは landscape にしたほうが内蔵マイクを使うときに自然な向きとなる。
        // それ以外は、リモート映像の分割が簡単になるように portrait で動かす。
        if (audioStereo) {
            if(resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        } else {
            if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
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
                spotlightLegacy = spotlightLegacy,
                spotlightNumber = spotlightNumber,
                videoEnabled      = videoEnabled,
                videoWidth        = videoWidth,
                videoHeight       = videoHeight,
                videoFPS          = fps,
                fixedResolution   = fixedResolution,
                videoCodec        = videoCodec,
                videoBitRate      = videoBitRate,
                audioEnabled      = audioEnabled,
                audioCodec        = audioCodec,
                audioBitRate      = audioBitRate,
                audioStereo       = audioStereo,
                role              = role,
                multistream       = multistream,
                cameraFacing      = cameraFacing,
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
