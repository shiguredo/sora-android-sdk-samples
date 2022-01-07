package jp.shiguredo.sora.sample.ui

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.media.AudioManager
import android.media.effect.EffectFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import jp.co.cyberagent.android.gpuimage.filter.*
import jp.shiguredo.sora.sample.BuildConfig
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.camera.EffectCameraVideoCapturerFactory
import jp.shiguredo.sora.sample.facade.SoraVideoChannel
import jp.shiguredo.sora.sample.option.SoraRoleType
import jp.shiguredo.sora.sample.ui.util.RendererLayoutCalculator
import jp.shiguredo.sora.sample.ui.util.SoraScreenUtil
import jp.shiguredo.sora.sdk.channel.data.ChannelAttendeesCount
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import jp.shiguredo.webrtc.video.effector.RTCVideoEffector
import jp.shiguredo.webrtc.video.effector.VideoEffectorContext
import jp.shiguredo.webrtc.video.effector.filter.GPUImageFilterWrapper
import kotlinx.android.synthetic.main.activity_video_chat_room.*
import org.webrtc.SurfaceViewRenderer
import com.google.gson.*

class EffectedVideoChatActivity : AppCompatActivity() {

    companion object {
        private val TAG = EffectedVideoChatActivity::class.simpleName
    }

    private var channelName: String = ""
    private var role = SoraRoleType.SENDRECV
    private var ui: EffectedVideoChatActivityUI? = null
    private var effector: RTCVideoEffector? = null

    private var oldAudioMode: Int = AudioManager.MODE_NORMAL

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setupWindow()

        effector = RTCVideoEffector().apply {

            when (intent.getStringExtra("EFFECT")) {
                "グレースケール"  -> {
                    addMediaEffectFilter(EffectFactory.EFFECT_GRAYSCALE)
                }
                "ピクセル化" -> {
                    addGPUImageFilter(GPUImagePixelationFilter(),
                            object : GPUImageFilterWrapper.Listener {
                                override fun onInit(filter: GPUImageFilter) {
                                    (filter as GPUImagePixelationFilter).setPixel(30.0f)
                                }
                                override fun onUpdate(filter: GPUImageFilter, vctx: VideoEffectorContext) {
                                    // do nothing
                                }
                            })
                }
                "ポスタライズ" -> {
                    addGPUImageFilter(GPUImagePosterizeFilter(5))
                }
                "トゥーン調" -> {
                    addGPUImageFilter(GPUImageToonFilter())
                }
                "ハーフトーン" -> {
                    addGPUImageFilter(GPUImageHalftoneFilter())
                }
                "色調補正" -> {
                    addGPUImageFilter(GPUImageHueFilter(100.0f))
                }
                "エンボス" -> {
                    addGPUImageFilter(GPUImageEmbossFilter())
                }
                "セピア調" -> {
                    addGPUImageFilter(GPUImageSepiaToneFilter())
                }
                "なし" -> {
                    // For Debug
                    addGPUImageFilter(GPUImageFilter())
                }
                else -> {}
            }
        }

        channelName = intent.getStringExtra("CHANNEL_NAME") ?: getString(R.string.channelId)
        ui = EffectedVideoChatActivityUI(
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
        effector?.disable()
        effector = null
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
            Toast.makeText(this@EffectedVideoChatActivity, "Error: ${reason.name}", Toast.LENGTH_LONG).show()
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
        Log.d(TAG, "connectChannel")
        val signalingEndpointCandidates = BuildConfig.SIGNALING_ENDPOINT.split(",").map{ it.trim() }
        val signalingMetadata = Gson().fromJson(BuildConfig.SIGNALING_METADATA, Map::class.java)
        channel = SoraVideoChannel(
                context                     = this,
                handler                     = Handler(),
                signalingEndpointCandidates = signalingEndpointCandidates,
                channelId                   = channelName,
                signalingMetadata           = signalingMetadata,
                videoWidth                  = 480,
                videoHeight                 = 960,
                videoFPS                    = 30,
                role                        = role,
                capturerFactory             = EffectCameraVideoCapturerFactory(effector!!),
                listener                    = channelListener
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

class EffectedVideoChatActivityUI(
        val activity:        EffectedVideoChatActivity,
        val channelName:     String?,
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
        renderer.layoutParams = FrameLayout.LayoutParams(dp2px(100), dp2px(100))
        activity.localRendererContainer.addView(renderer)
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
        val params =
                RelativeLayout.LayoutParams(dp2px(videoViewWidth), dp2px(videoViewHeight))
        val margin = dp2px(videoViewMargin)
        params.setMargins(margin, margin, margin, margin)
        return params
    }
}

