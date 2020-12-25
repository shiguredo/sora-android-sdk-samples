package jp.shiguredo.sora.sample.ui

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import jp.shiguredo.sora.sample.BuildConfig
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sdk2.*
import kotlinx.android.synthetic.main.activity_video_chat_room.*
import org.webrtc.CameraVideoCapturer

open class SampleAppActivity: AppCompatActivity() {

    companion object {
        private val TAG = SampleAppActivity::class.simpleName
    }

    // 本アプリ外の音声モードです。
    // ユーザーが本アプリから別のアプリに切り替えたとき、音声モードはこの値に変更されます。
    private var outsideAudioMode: Int = AudioManager.MODE_INVALID

    val channelName: String
        get() = intent.getStringExtra("CHANNEL_NAME") ?: getString(R.string.channelId) ?: ""

    val role: Role
        get() = when (intent.getStringExtra("ROLE")) {
            "SENDONLY" -> Role.SENDONLY
            "RECVONLY" -> Role.RECVONLY
            "SENDRECV" -> Role.SENDRECV
            else -> Role.SENDRECV
        }

    val multistreamEnabled: Boolean
        get() = when (intent.getStringExtra("MULTISTREAM")) {
            "有効" -> true
            else -> false
        }

    val activeSpeakerLimit: Int
        get() = intent.getStringExtra("SPOTLIGHT")?.toInt() ?: 3

    val videoFrameSize: VideoFrameSize
        get() =
            when (intent.getStringExtra("VIDEO_SIZE")) {
                // Portrait
                "VGA" -> VideoFrameSize.VGA.portrate
                "QQVGA" -> VideoFrameSize.QQVGA.portrate
                "QCIF" -> VideoFrameSize.QCIF.portrate
                "HQVGA" -> VideoFrameSize.HQVGA.portrate
                "QVGA" -> VideoFrameSize.QVGA.portrate
                "HD" -> VideoFrameSize.HD.portrate
                "FHD" -> VideoFrameSize.FHD.portrate
                "Res1920x3840" -> VideoFrameSize.Res3840x1920.portrate
                "UHD2160x3840" -> VideoFrameSize.UHD3840x2160.portrate
                "UHD2160x4096" -> VideoFrameSize.UHD4096x2160.portrate
                // Landscape
                "Res3840x1920" -> VideoFrameSize.Res3840x1920.landscape
                "UHD3840x2160" -> VideoFrameSize.UHD3840x2160.landscape
                // Default
                else -> VideoFrameSize.VGA.portrate
            }

    var videoEnabled: Boolean
        get() = when (intent.getStringExtra("VIDEO_ENABLED")) {
            "有効" -> true
            "無効" -> false
            else -> true
        }
        set(value) {
            intent.putExtra("VIDEO_ENABLED", value)
        }

    val videoCodec: VideoCodec
        get() = VideoCodec.valueOf(intent.getStringExtra("VIDEO_CODEC") ?: "VP9")

    val videoFps: Int
        get() = (intent.getStringExtra("FPS") ?: "30").toInt()

    val videoBitRate: Int?
        get() = when (val stringValue = intent.getStringExtra("VIDEO_BIT_RATE")) {
            "未指定" -> null
            else -> stringValue?.toInt()
        }

    val audioEnabled: Boolean
        get() = when (intent.getStringExtra("AUDIO_ENABLED")) {
            "有効" -> true
            "無効" -> false
            else -> true
        }

    val audioCodec: AudioCodec
        get() = AudioCodec.valueOf(intent.getStringExtra("AUDIO_CODEC") ?: "OPUS")

    val audioBitRate: Int?
        get() = when (val stringValue = intent.getStringExtra("AUDIO_BIT_RATE")) {
            "未指定" -> null
            else -> stringValue?.toInt()
        }

    val audioSound: AudioSound
        get() = when (intent.getStringExtra("AUDIO_STEREO")) {
            "モノラル" -> AudioSound.MONO
            "ステレオ" -> AudioSound.STEREO
            else -> AudioSound.MONO
        }

    val fixedResolution: Boolean
        get() = when (intent.getStringExtra("RESOLUTION_CHANGE")) {
            "可変" -> false
            "固定" -> true
            else -> false
        }

    val cameraFacing: Boolean
        get() = when (intent.getStringExtra("CAMERA_FACING")) {
            "前面" -> true
            "背面" -> false
            else -> true
        }

    fun setRequestedOrientation() {
        // ステレオでは landscape にしたほうが内蔵マイクを使うときに自然な向きとなる。
        // それ以外は、リモート映像の分割が簡単になるように portrait で動かす。
        if (audioSound == AudioSound.STEREO) {
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        } else {
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    var mediaChannel: MediaChannel? = null

    val audioManager: AudioManager
        get() = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        outsideAudioMode = audioManager.mode
        setupWindow()
        setRequestedOrientation()

        if (videoEnabled) {
            ui = createUI()
        } else {
            setContentView(R.layout.activity_voice_chat_room)
        }

        connect()
    }

    internal open fun createUI(): VideoChatActivityUI {
        return VideoChatActivityUI(
                activity = this,
                layout = R.layout.activity_video_chat_room,
                channelName = channelName,
                resources = resources,
                videoViewWidth = 100,
                videoViewHeight = 100,
                videoViewMargin = 10,
                density = this.resources.displayMetrics.density)
    }

    internal fun setupWindow() {
        supportActionBar?.hide()

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setWindowVisibility()
    }

    @Suppress("DEPRECATION")
    @TargetApi(Build.VERSION_CODES.KITKAT)
    internal fun setWindowVisibility() {
        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    override fun onResume() {
        super.onResume()
        this.volumeControlStream = AudioManager.STREAM_VOICE_CALL
        outsideAudioMode = audioManager.mode
        Log.d(TAG, "AudioManager mode change: $outsideAudioMode => MODE_IN_COMMUNICATION(3)")
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    }

    @SuppressLint("WrongConstant")
    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        Log.d(TAG, "AudioManager mode change: MODE_IN_COMMUNICATION(3) => ${outsideAudioMode}")
        audioManager.mode = outsideAudioMode
        close()
    }

    internal fun close() {
        Log.d(TAG, "close")
        disconnect()
        finish()
    }

    open fun onConnectionConfiguration(configuration: jp.shiguredo.sora.sdk2.Configuration) {}

    internal fun connect() {
        Log.d(TAG, "connect")

        val configuration = Configuration(this,
                BuildConfig.SIGNALING_ENDPOINT, channelName, role).also {
            it.multistreamEnabled = multistreamEnabled
            it.videoEnabled = videoEnabled
            it.videoCodec = videoCodec
            it.videoBitRate = videoBitRate
            it.videoFps = videoFps
            it.videoFrameSize = videoFrameSize
            it.audioEnabled = audioEnabled
            it.audioCodec = audioCodec
            it.audioBitRate = audioBitRate
            it.inputAudioSound = audioSound
            it.spotlightEnabled = activeSpeakerLimit != 0
            it.activeSpeakerLimit = activeSpeakerLimit
        }
        onConnectionConfiguration(configuration)

        Sora.connect(configuration) { result ->
            result
                    .onFailure {
                        Log.d(TAG, "connection failed => $it")
                    }.onSuccess {
                        Log.d(TAG, "connected")
                        this.mediaChannel = it
                        // TODO: get sender stream
                        it.streams.firstOrNull()?.videoRenderer = localRendererContainer
                    }
        }
    }

    internal fun disconnect() {
        Log.d(TAG, "disconnectChannel")
        mediaChannel?.disconnect()
    }

    private val cameraSwitchHandler = object : CameraVideoCapturer.CameraSwitchHandler {

        override fun onCameraSwitchDone(isFront: Boolean) {
            Log.d(TAG, "camera switched.")
        }

        override fun onCameraSwitchError(msg: String?) {
            Log.w(TAG, "failed to switch camera ${msg}")
        }
    }

    fun switchCamera() {
        Log.d(TAG, "switch camera => ${mediaChannel}, ${mediaChannel?.videoCapturer}")
        mediaChannel?.videoCapturer?.let {
            (it as? CameraVideoCapturer)?.switchCamera(cameraSwitchHandler)
        }
    }

    internal var ui: VideoChatActivityUI? = null

    internal var muted = false

    internal fun toggleMuted() {
        if (muted) {
            ui?.showMuteButton()
        } else {
            ui?.showUnmuteButton()
        }
        muted = !muted
        mediaChannel?.senderStream?.isMuted = muted
    }

}