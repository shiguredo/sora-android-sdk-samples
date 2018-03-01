package jp.shiguredo.sora.sample.ui

import android.annotation.TargetApi
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
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
import org.jetbrains.anko.*
import org.webrtc.SurfaceViewRenderer

class VideoChatRoomActivity : AppCompatActivity() {

    val TAG = VideoChatRoomActivity::class.simpleName

    private var channelName = ""

    private var videoCodec:  SoraVideoOption.Codec = SoraVideoOption.Codec.VP9
    private var audioCodec:  SoraAudioOption.Codec = SoraAudioOption.Codec.OPUS
    private var audioEnabled = true
    private var bitRate: Int? = null
    private var videoWidth: Int = SoraVideoOption.FrameSize.Portrait.VGA.x
    private var videoHeight: Int = SoraVideoOption.FrameSize.Portrait.VGA.y
    private var fps: Int = 30

    private var streamType = SoraStreamType.BIDIRECTIONAL

    private var ui: VideoChatRoomActivityUI? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setupWindow()

        ui = VideoChatRoomActivityUI(
                resources       = resources,
                videoViewWidth  = 100,
                videoViewHeight = 100,
                videoViewMargin = 10,
                density         = this.resources.displayMetrics.density
        )

        ui?.setContentView(this)

        channelName = intent.getStringExtra("CHANNEL_NAME")

        videoCodec = SoraVideoOption.Codec.valueOf(
                intent.getStringExtra("VIDEO_CODEC"))

        audioCodec = SoraAudioOption.Codec.valueOf(
                intent.getStringExtra("AUDIO_CODEC"))

        when (intent.getStringExtra("STREAM_TYPE")) {
            "BIDIRECTIONAL" -> { streamType = SoraStreamType.BIDIRECTIONAL }
            "SINGLE-UP"     -> { streamType = SoraStreamType.SINGLE_UP     }
            "SINGLE-DOWN"   -> { streamType = SoraStreamType.SINGLE_DOWN   }
            "MULTI-DOWN"    -> { streamType = SoraStreamType.MULTI_DOWN    }
            else            -> { streamType = SoraStreamType.BIDIRECTIONAL }
        }

        audioEnabled = when (intent.getStringExtra("AUDIO_ENABLED")) {
            "YES" -> true
            "NO"  -> false
            else  -> true
        }

        fps = intent.getStringExtra("FPS").toInt()

        listOf("VGA", "QQVGA", "QCIF", "HQVGA", "QVGA", "HD", "FHD")

        val size = when (intent.getStringExtra("VIDEO_SIZE")) {
            "VGA" -> {
                videoWidth = SoraVideoOption.FrameSize.Portrait.VGA.x
                videoHeight = SoraVideoOption.FrameSize.Portrait.VGA.y
            }
            "QQVGA" -> {
                videoWidth = SoraVideoOption.FrameSize.Portrait.QQVGA.x
                videoHeight = SoraVideoOption.FrameSize.Portrait.QQVGA.y
            }
            "QCIF" -> {
                videoWidth = SoraVideoOption.FrameSize.Portrait.QCIF.x
                videoHeight = SoraVideoOption.FrameSize.Portrait.QCIF.y
            }
            "HQVGA" -> {
                videoWidth = SoraVideoOption.FrameSize.Portrait.HQVGA.x
                videoHeight = SoraVideoOption.FrameSize.Portrait.HQVGA.y
            }
            "QVGA" -> {
                videoWidth = SoraVideoOption.FrameSize.Portrait.QVGA.x
                videoHeight = SoraVideoOption.FrameSize.Portrait.QVGA.y
            }
            "HD" -> {
                videoWidth = SoraVideoOption.FrameSize.Portrait.HD.x
                videoHeight = SoraVideoOption.FrameSize.Portrait.HD.y
            }
            "FHD" -> {
                videoWidth = SoraVideoOption.FrameSize.Portrait.FHD.x
                videoHeight = SoraVideoOption.FrameSize.Portrait.FHD.y
            }
            else -> { }
        }

        val bitRateStr = intent.getStringExtra("BITRATE")
        bitRate = when (bitRateStr) {
            "UNDEFINED" -> null
            else        -> bitRateStr.toInt()
        }

        ui?.setChannelName(channelName)

        connectChannel()
    }

    private fun setupWindow() {
        supportActionBar?.hide()

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setWindowVisibility()
        } else {
            setLegacyWindowVisibility()
        }
    }

    fun setLegacyWindowVisibility() {
        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun setWindowVisibility() {
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
                videoWidth        = videoWidth,
                videoHeight       = videoHeight,
                videoFPS          = fps,
                videoCodec        = videoCodec,
                videoBitrate      = bitRate,
                audioEnabled      = audioEnabled,
                audioCodec        = audioCodec,
                streamType        = streamType,
                listener          = channelListener,
                needLocalRenderer = true
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

    internal fun mute() {
        if (muted) {
           ui?.showDisabledMuteButton()
        } else {
            ui?.showEnabledMuteButton()
        }
        muted = !muted
        channel?.mute(muted)
    }

}

class VideoChatRoomActivityUI(
        val resources:       Resources,
        val videoViewWidth:  Int,
        val videoViewHeight: Int,
        val videoViewMargin: Int,
        val density:         Float
) : AnkoComponent<VideoChatRoomActivity> {

    private var channelText:       TextView?   = null
    private var rendererContainer: RelativeLayout? = null

    private var numberOfSendersText:   TextView? = null
    private var numberOfReceiversText: TextView? = null

    private var muteButton: ImageButton? = null
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

    internal fun showEnabledMuteButton() {
        muteButton?.image = resources.getDrawable(R.drawable.ic_mic_white_48dp, null)
        muteButton?.background = resources.getDrawable(R.drawable.enabled_button_background, null)
    }

    internal fun showDisabledMuteButton() {
        muteButton?.image = resources.getDrawable(R.drawable.ic_mic_off_white_48dp, null)
        muteButton?.background = resources.getDrawable(R.drawable.button_background, null)
    }

    override fun createView(ui: AnkoContext<VideoChatRoomActivity>): View = with(ui) {

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

                    lparams {
                        width = matchParent
                        height = dip(50)
                    }

                    backgroundColor = Color.parseColor("#FFC107")

                    this.gravity = Gravity.CENTER
                    text = "Channel"
                    textColor = Color.WHITE
                    textSize = 20f
                    padding = dip(10)
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

                    muteButton = imageButton {
                        lparams {
                            width = dip(50)
                            height = dip(50)
                            rightMargin = dip(10)
                        }

                        image = resources.getDrawable(R.drawable.ic_mic_white_48dp, null)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        background = resources.getDrawable(R.drawable.enabled_button_background, null)

                        onClick {
                            ui.owner.mute()
                        }
                    }

                    imageButton {

                        lparams {
                            width = dip(50)
                            height = dip(50)
                            rightMargin = dip(10)
                        }

                        image = resources.getDrawable(R.drawable.ic_videocam_white_48dp, null)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        background = resources.getDrawable(R.drawable.enabled_button_background, null)

                        onClick {
                            ui.owner.switchCamera()
                        }
                    }


                    imageButton {

                        lparams {
                            width = dip(50)
                            height = dip(50)
                            rightMargin = dip(10)
                        }

                        image = resources.getDrawable(R.drawable.ic_close_white_48dp, null)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        background = resources.getDrawable(R.drawable.close_button_background, null)

                        onClick {
                            ui.owner.close()
                        }
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