package jp.shiguredo.sora.sample.ui

import android.annotation.TargetApi
import android.content.res.Resources
import android.graphics.Color
import android.media.effect.EffectFactory
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import jp.co.cyberagent.android.gpuimage.*
import jp.shiguredo.sora.sample.BuildConfig
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.camera.EffectCameraVideoCapturerFactory
import jp.shiguredo.sora.sample.facade.SoraVideoChannel
import jp.shiguredo.sora.sample.option.SoraStreamType
import jp.shiguredo.sora.sample.ui.util.RendererLayoutCalculator
import jp.shiguredo.sora.sample.ui.util.SoraScreenUtil
import jp.shiguredo.sora.sdk.channel.data.ChannelAttendeesCount
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import jp.shiguredo.webrtc.video.effector.RTCVideoEffector
import jp.shiguredo.webrtc.video.effector.VideoEffectorContext
import jp.shiguredo.webrtc.video.effector.filter.GPUImageFilterWrapper
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk21.listeners.onClick
import org.webrtc.SurfaceViewRenderer

class EffectedVideoChatActivity : AppCompatActivity() {

    val TAG = EffectedVideoChatActivity::class.simpleName

    private var channelName = ""

    private var streamType = SoraStreamType.BIDIRECTIONAL

    private var ui: EffectedVideoChatActivityUI? = null

    private var effector: RTCVideoEffector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setupWindow()

        ui = EffectedVideoChatActivityUI(
                resources       = resources,
                videoViewWidth  = 100,
                videoViewHeight = 100,
                videoViewMargin = 10,
                density         = this.resources.displayMetrics.density
        )

        ui?.setContentView(this)

        channelName = intent.getStringExtra("CHANNEL_NAME")

        ui?.setChannelName(channelName)

        effector = RTCVideoEffector().apply {

            when (intent.getStringExtra("EFFECT")) {
                "GRAYSCALE"  -> {
                    addMediaEffectFilter(EffectFactory.EFFECT_GRAYSCALE)
                }
                "PIXELATION" -> {
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
                "POSTERIZE" -> {
                    addGPUImageFilter(GPUImagePosterizeFilter(5))
                }
                "TOON" -> {
                    addGPUImageFilter(GPUImageToonFilter())
                }
                "HALFTONE" -> {
                    addGPUImageFilter(GPUImageHalftoneFilter())
                }
                "HUE" -> {
                    addGPUImageFilter(GPUImageHueFilter(100.0f))
                }
                "EMBOSS" -> {
                    addGPUImageFilter(GPUImageEmbossFilter())
                }
                else -> {}
            }


        }

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

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
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
            ui?.setNumberOfReceivers(attendees.numberOfDownstreams)
            ui?.setNumberOfSenders(attendees.numberOfUpstreams)
        }
    }


    private fun connectChannel() {
        Log.d(TAG, "openChannel")

        channel = SoraVideoChannel(
                context           = this,
                signalingEndpoint = BuildConfig.SIGNALING_ENDPOINT,
                channelId         = channelName,
                signalingMetadata = "",
                videoWidth        = 480,
                videoHeight       = 960,
                videoFPS          = 30,
                streamType        = streamType,
                capturerFactory   = EffectCameraVideoCapturerFactory(effector!!),
                listener          = channelListener
                //needLocalRenderer = false
        )
        channel!!.connect()
    }

    private fun disconnectChannel() {
        Log.d(TAG, "closeChannel")
        channel?.disconnect()
    }

    private fun disposeChannel() {
        channel?.dispose()
    }

    internal fun switchCamera() {
        channel?.switchCamera()
    }

    private var muted = false

    internal fun toggleMute() {
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
        val resources: Resources,
        val videoViewWidth:  Int,
        val videoViewHeight: Int,
        val videoViewMargin: Int,
        val density:         Float
) : AnkoComponent<EffectedVideoChatActivity> {

    private var channelText:       TextView?   = null
    private var rendererContainer: RelativeLayout? = null

    private var numberOfSendersText:   TextView? = null
    private var numberOfReceiversText: TextView? = null

    private var toggleMuteButton: ImageButton? = null
    private var localRendererContainer: FrameLayout? = null

    private var renderersLayoutCalculator: RendererLayoutCalculator? = null

    internal fun setChannelName(name: String) {
        channelText?.text = name
    }

    internal fun setNumberOfSenders(num: Int) {
        numberOfSendersText?.text = num.toString()
    }

    internal fun setNumberOfReceivers(num: Int) {
        numberOfReceiversText?.text = num.toString()
    }

    internal fun changeState(colorCode: String) {
        channelText?.backgroundColor = Color.parseColor(colorCode)
    }

    private fun dpi2px(d: Int): Int = (density * d).toInt()

    private fun rendererLayoutParams(): RelativeLayout.LayoutParams {
        val params =
                RelativeLayout.LayoutParams(dpi2px(videoViewWidth), dpi2px(videoViewHeight))
        val margin = dpi2px(videoViewMargin)
        params.setMargins(margin, margin, margin, margin)
        return params
    }

    internal fun addLocalRenderer(renderer: SurfaceViewRenderer) {
        renderer.layoutParams =
                FrameLayout.LayoutParams(dpi2px(100), dpi2px(100))
        localRendererContainer?.addView(renderer)
        renderer.setMirror(true)
    }

    internal fun addRenderer(renderer: SurfaceViewRenderer) {
        renderer.layoutParams = rendererLayoutParams()
        rendererContainer?.addView(renderer)
        renderersLayoutCalculator?.add(renderer)
    }

    internal fun removeRenderer(renderer: SurfaceViewRenderer) {
        rendererContainer?.removeView(renderer)
        renderersLayoutCalculator?.remove(renderer)
    }

    internal fun showUnmuteButton() {
        toggleMuteButton?.image = resources.getDrawable(R.drawable.ic_mic_white_48dp, null)
    }

    internal fun showMuteButton() {
        toggleMuteButton?.image = resources.getDrawable(R.drawable.ic_mic_off_black_48dp, null)
    }

    override fun createView(ui: AnkoContext<EffectedVideoChatActivity>): View = with(ui) {

        renderersLayoutCalculator = RendererLayoutCalculator(
                width = SoraScreenUtil.size(ui.owner).x - dip(20 * 4),
                height = SoraScreenUtil.size(ui.owner).y - dip(20 * 4 + 100 + 50)
        )

        return verticalLayout {

            lparams {
                width = matchParent
                height = matchParent
            }

            backgroundColor = Color.BLACK

            padding = dip(20)

            verticalLayout {

                lparams {
                    width = matchParent
                    weight = 1f
                }

                backgroundColor = Color.parseColor("#222222")

                channelText = textView {

                    backgroundColor = Color.parseColor("#FFC107")

                    this.gravity = Gravity.CENTER
                    text = "Channel"
                    textColor = Color.WHITE
                    textSize = 20f
                    padding = dip(10)
                }.lparams {
                    width = matchParent
                    height = dip(50)
                }

                rendererContainer = relativeLayout {
                    lparams {
                        width = SoraScreenUtil.size(ui.owner).x - dip(20 * 4)
                        height = SoraScreenUtil.size(ui.owner).y - dip(20 * 4 + 100 + 50)
                        topMargin = dip(20)
                        leftMargin = dip(20)
                    }
                    backgroundColor = Color.parseColor("#000000")
                }

            }

            relativeLayout {

                backgroundColor = Color.argb(200, 0, 0, 0)

                lparams {
                    width  = matchParent
                    height = dip(100)
                }

                padding = dip(10)

                localRendererContainer = frameLayout {

                    backgroundColor = Color.GRAY

                }.lparams {
                    width = dip(80)
                    height = dip(80)
                    alignParentLeft()
                    centerVertically()
                }


                linearLayout {

                    toggleMuteButton = imageButton {
                        image = resources.getDrawable(R.drawable.ic_mic_off_black_48dp, null)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        background = resources.getDrawable(R.drawable.enabled_button_background, null)

                        onClick {
                            ui.owner.toggleMute()
                        }
                    }.lparams {
                        width = dip(50)
                        height = dip(50)
                        rightMargin = dip(10)
                    }



                    imageButton {
                        image = resources.getDrawable(R.drawable.ic_videocam_white_48dp, null)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        background = resources.getDrawable(R.drawable.enabled_button_background, null)

                        onClick {
                            ui.owner.switchCamera()
                        }
                    }.lparams {
                        width = dip(50)
                        height = dip(50)
                        rightMargin = dip(10)
                    }

                    imageButton {
                        image = resources.getDrawable(R.drawable.ic_close_white_48dp, null)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        background = resources.getDrawable(R.drawable.close_button_background, null)

                        onClick {
                            ui.owner.close()
                        }
                    }.lparams {
                        width = dip(50)
                        height = dip(50)
                        rightMargin = dip(10)
                    }

                }.lparams {
                    width = wrapContent
                    height = wrapContent
                    alignParentRight()
                    centerVertically()
                }

            }

        }
    }
}

