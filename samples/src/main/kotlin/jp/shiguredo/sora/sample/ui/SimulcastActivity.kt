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
import jp.shiguredo.sora.sdk.channel.option.SoraVideoOption
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import jp.shiguredo.sora.sdk.util.SoraLogger
import kotlinx.android.synthetic.main.activity_simulcast.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.webrtc.SurfaceViewRenderer
import java.io.BufferedOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.*

class SimulcastActivity : AppCompatActivity() {

    companion object {
        private val TAG = SimulcastActivity::class.simpleName
    }

    private var channelName = ""
    private var spotlight = 0
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
    private var fps: Int = 30
    private var fixedResolution = false

    private var oldAudioMode: Int = AudioManager.MODE_INVALID

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

        ui = SimulcastActivityUI(
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
            Toast.makeText(this@SimulcastActivity, "Error: ${reason.name}", Toast.LENGTH_LONG).show()
            close()
        }

        override fun onWarning(channel: SoraVideoChannel, reason: SoraErrorReason) {
            Toast.makeText(this@SimulcastActivity, "Error: ${reason.name}", Toast.LENGTH_LONG).show()
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
                spotlight         = null,
                videoEnabled      = videoEnabled,
                videoWidth        = videoWidth,
                videoHeight       = videoHeight,
                simulcast         = true,
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

    internal fun changeRid(rid: String) {
        val connectionId = channel?.mediaChannel?.connectionId
        if (connectionId == null) {
            SoraLogger.d(TAG, "cannot change rid: connection ID is not found")
            return
        }

        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                val host = URI(BuildConfig.SIGNALING_ENDPOINT).host
                val url = URL("http://$host:3000")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("x-sora-target", "Sora_20201005.RequestRtpStream")
                conn.doOutput = true
                conn.connect()
                val buffer = BufferedOutputStream(conn.outputStream)
                buffer.write("{\n".toByteArray())
                buffer.write("    \"channel_id\": \"$channelName\",\n".toByteArray())
                buffer.write("    \"recv_connection_id\": \"$connectionId\",\n".toByteArray())
                buffer.write("    \"rid\": \"$rid\"\n".toByteArray())
                buffer.write("}".toByteArray())
                buffer.flush()
                buffer.close()
                conn.outputStream.close()

                val status = conn.responseCode
                SoraLogger.d(TAG, "change rid: response $status")
            }
        }
    }

}

class SimulcastActivityUI(
        val activity:        SimulcastActivity,
        val channelName:     String,
        val resources:       Resources,
        val videoViewWidth:  Int,
        val videoViewHeight: Int,
        val videoViewMargin: Int,
        val density:         Float
) {

    private val renderersLayoutCalculator: RendererLayoutCalculator

    init {
        activity.setContentView(R.layout.activity_simulcast)
        activity.channelNameText.text = channelName
        this.renderersLayoutCalculator = RendererLayoutCalculator(
                width = SoraScreenUtil.size(activity).x - dp2px(20 * 2),
                height = SoraScreenUtil.size(activity).y - dp2px(20 * 2 + 100)
        )
        activity.toggleMuteButton.setOnClickListener { activity.toggleMuted() }
        activity.switchCameraButton.setOnClickListener { activity.switchCamera() }
        activity.closeButton.setOnClickListener { activity.close() }
        activity.r0Button.setOnClickListener { activity.changeRid("r0") }
        activity.r1Button.setOnClickListener { activity.changeRid("r1") }
        activity.r2Button.setOnClickListener { activity.changeRid("r2") }
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
