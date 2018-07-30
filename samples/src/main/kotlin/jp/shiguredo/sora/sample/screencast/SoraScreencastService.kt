package jp.shiguredo.sora.sample.screencast

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.media.projection.MediaProjection
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.ui.util.SoraScreenUtil
import jp.shiguredo.sora.sdk.channel.SoraMediaChannel
import jp.shiguredo.sora.sdk.channel.option.SoraAudioOption
import jp.shiguredo.sora.sdk.channel.option.SoraMediaOption
import jp.shiguredo.sora.sdk.channel.option.SoraVideoOption
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import jp.shiguredo.sora.sdk.util.SoraLogger
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk21.listeners.onClick
import org.webrtc.*

@TargetApi(21)
class SoraScreencastService : Service() {

    companion object {
        val TAG = SoraScreencastService::class.simpleName

        private var running = false
        fun isRunning() = running
    }

    private var localAudioTrack: AudioTrack? = null
    private var mediaChannel: SoraMediaChannel? = null
    private var capturer: VideoCapturer? = null
    private var req: ScreencastRequest? = null
    private var egl: EglBase? = null
    private var layout: View? = null
    private var uiContainer: ScreencastUIContainer? = null

    private var capturing = false

    private val channelListener = object : SoraMediaChannel.Listener {

        override fun onConnect(mediaChannel: SoraMediaChannel) {
            SoraLogger.d(TAG, "[screencast] @onConnected")
        }

        override fun onClose(mediaChannel: SoraMediaChannel) {
            SoraLogger.d(TAG, "[screencast] @onClose")
            closeChannel()
        }

        override fun onError(mediaChannel: SoraMediaChannel, reason: SoraErrorReason) {
            SoraLogger.d(TAG, "[screencast] @onError")
        }

        override fun onAddLocalStream(mediaChannel: SoraMediaChannel, ms: MediaStream) {
            SoraLogger.d(TAG, "[screencast] @onAddLocalStream")
            if (ms.audioTracks.size > 0) {
                localAudioTrack = ms.audioTracks[0]
                localAudioTrack?.setEnabled(!muted)
            }
            startCapturer()
        }

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        SoraScreencastService.running = true

        egl = EglBase.create()

        req = intent.getParcelableExtra("SCREENCAST_REQUEST")
        if (req == null) {
            SoraLogger.w(TAG, "request not found")
            stopSelf()
            return Service.START_NOT_STICKY
        }

        setupUI()
        startNotification(startId)
        openChannel()

        return START_NOT_STICKY
    }

    private fun startNotification(startId: Int) {
        val notificationChannelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel()
            } else {
                ""
            }

        val activityIntent = createBoundActivityIntent()
        val pendingIntent  = PendingIntent.getActivity(this, 0, activityIntent, 0)
        val notification = NotificationCompat.Builder(this, notificationChannelId)
                .setContentTitle(req!!.stateTitle)
                .setContentText(req!!.stateText)
                .setContentIntent(pendingIntent)
                .setSmallIcon(req!!.notificationIcon)
                .build()
        startForeground(startId, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = "jp.shiguredo.sora.sample"
        val channelName = "Sora SDK Sample"
        val channel = NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE)
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return channelId
    }

    fun setupUI() {
        layout = createLayout()
        uiContainer = ScreencastUIContainer(this, layout!!, matchParent, wrapContent)
    }

    private fun launchActivity() {
        val intent = createBoundActivityIntent()
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    internal fun createBoundActivityIntent(): Intent {
        val intent = Intent(this, Class.forName(req!!.boundActivityName))
        intent.putExtra("CHANNEL_ID", req!!.channelId)
        return intent
    }

    private var navigationBar: LinearLayout? = null
    private var toggleMuteButton: ImageButton? = null

    // TODO remove Anko dependency
    internal fun createLayout() : View {

        return frameLayout {

            lparams {
                width   = matchParent
                height  = wrapContent
                setPadding(0, SoraScreenUtil.statusBarHeight(this@SoraScreencastService), 0, 0)
            }

            navigationBar = linearLayout {

                backgroundColor = Color.argb(200, 0, 0, 0)

                lparams {
                    width  = matchParent
                    height = wrapContent
                }

                verticalGravity = Gravity.CENTER_VERTICAL

                padding = dip(10)


                imageButton {
                    image = resources.getDrawable(R.drawable.ic_unfold_more_white_48dp, null)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    background = resources.getDrawable(R.drawable.button_background, null)

                    onClick {
                        toggleNavigationBarPosition()
                    }
                }.lparams {
                    width = dip(50)
                    height = dip(50)
                    rightMargin = dip(10)
                }

                imageView {
                    image = resources.getDrawable(req!!.notificationIcon, null)

                    onClick {
                       launchActivity()
                    }
                }.lparams {
                    width = dip(50)
                    height = dip(50)
                }

                textView {
                    padding = dip(10)
                    text = req!!.stateTitle + "\n" + req!!.stateText
                    textSize = 14.0f
                    textColor = Color.WHITE
                    maxLines = 2
                }.lparams {
                    height = wrapContent
                    width  = matchParent
                    weight = 1f
                }

                toggleMuteButton = imageButton {
                    image = resources.getDrawable(R.drawable.ic_mic_white_48dp, null)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    background = resources.getDrawable(R.drawable.enabled_button_background, null)

                    onClick {
                        toggleMuted()
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
                        closeChannel()
                    }
                }.lparams {
                    width = dip(50)
                    height = dip(50)
                }

            }

        }
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        closeChannel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private var muted = true
    private fun toggleMuted() {
        if (muted) {
            localAudioTrack?.setEnabled(true)
            toggleMuteButton?.image = resources.getDrawable(R.drawable.ic_mic_off_black_48dp, null)
        } else {
            localAudioTrack?.setEnabled(false)
            toggleMuteButton?.image = resources.getDrawable(R.drawable.ic_mic_white_48dp, null)
        }
        muted = !muted
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> changeCaptureFormat()
            Configuration.ORIENTATION_PORTRAIT  -> changeCaptureFormat()
            else -> { /* do nothing */ }
        }
    }

    private fun changeCaptureFormat() {
        capturer?.let {
            val size = SoraScreenUtil.size(this)
            it.changeCaptureFormat(
                    Math.round(size.x * req!!.videoScale),
                    Math.round(size.y * req!!.videoScale),
                    req!!.videoFPS
            )
        }
    }

    internal fun startCapturer() {
        capturer?.let {
            if (!capturing) {
                capturing = true
                SoraLogger.d(TAG, "startCapture")
                val size = SoraScreenUtil.size(this)
                it.startCapture(
                        Math.round(size.x * req!!.videoScale),
                        Math.round(size.y * req!!.videoScale),
                        req!!.videoFPS
                )
            }
        }
    }

    internal fun stopCapturer() {
        capturer?.let {
            if (capturing) {
                capturing = false
                SoraLogger.d(TAG, "startCapture")
                it.stopCapture()
            }
        }
    }

    private fun toggleNavigationBarPosition() {
        uiContainer?.togglePosition()
    }

    private val mediaProjectionCallback: MediaProjection.Callback =
            object : MediaProjection.Callback() {
                override fun onStop() {
                    SoraLogger.w(TAG, "projection onStop")
                    closeChannel()
                }
            }

    internal fun openChannel() {

        capturer = ScreenCapturerAndroid(req!!.data, mediaProjectionCallback)

        if (capturer == null) {
            SoraLogger.w(TAG, "failed to obtain screen capturer")
            return
        }

        val mediaOption = SoraMediaOption().apply {
            // when audio stream is disabled, it'll crush.
            enableAudioUpstream()
            enableVideoUpstream(capturer!!, egl?.eglBaseContext)
            videoCodec = SoraVideoOption.Codec.valueOf(req!!.videoCodec)
            audioCodec = SoraAudioOption.Codec.valueOf(req!!.audioCodec)

            if (req!!.multistream) {
                enableMultistream()
            }
        }
        mediaChannel = SoraMediaChannel(
                context           = this,
                signalingEndpoint = req!!.signalingEndpoint,
                channelId         = req!!.channelId,
                signalingMetadata = req!!.signalingMetadata,
                mediaOption       = mediaOption,
                listener          = channelListener
        )
        mediaChannel!!.connect()
    }

    internal fun closeChannel() {
        runOnUiThread {
            mediaChannel?.disconnect()
            mediaChannel = null
            stopCapturer()
            egl?.release()
            egl = null
            uiContainer?.clear()
            NetworkMonitor.getInstance().stopMonitoring()
            SoraScreencastService.running = false
            stopSelf()
        }
    }
}

