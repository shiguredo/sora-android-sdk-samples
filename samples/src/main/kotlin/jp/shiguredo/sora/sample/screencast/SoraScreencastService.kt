package jp.shiguredo.sora.sample.screencast

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.ui.util.SoraScreenUtil
import jp.shiguredo.sora.sdk.channel.SoraMediaChannel
import jp.shiguredo.sora.sdk.channel.option.SoraAudioOption
import jp.shiguredo.sora.sdk.channel.option.SoraMediaOption
import jp.shiguredo.sora.sdk.channel.option.SoraVideoOption
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import jp.shiguredo.sora.sdk.util.SoraLogger
import kotlinx.android.synthetic.main.screencast_service.view.*
import org.webrtc.AudioTrack
import org.webrtc.EglBase
import org.webrtc.MediaStream
import org.webrtc.NetworkMonitor
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.VideoCapturer

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
        val pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, 0)
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
        val notificationChannelId = "jp.shiguredo.sora.sample"
        val notificationChannelName = "Sora SDK Sample"
        val channel = NotificationChannel(
            notificationChannelId,
            notificationChannelName, NotificationManager.IMPORTANCE_NONE
        )
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return notificationChannelId
    }

    private fun setupUI() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        layout = inflater.inflate(R.layout.screencast_service, null)
        layout!!.setPadding(0, SoraScreenUtil.statusBarHeight(this), 0, 0)
        val navigationBar = layout!!.navigationBar
        navigationBar.launchActivityView.setImageResource(req!!.notificationIcon)
        navigationBar.togglePositionButton.setImageResource(R.drawable.ic_unfold_more_white_48dp)
        navigationBar.stateText.text = "${req!!.stateTitle}\n${req!!.stateText}"
        navigationBar.toggleMutedButton.setImageResource(R.drawable.ic_mic_white_48dp)
        navigationBar.closeChannelButton.setImageResource(R.drawable.ic_close_white_48dp)

        navigationBar.togglePositionButton.setOnClickListener { toggleNavigationBarPosition() }
        navigationBar.launchActivityView.setOnClickListener { launchActivity() }
        navigationBar.toggleMutedButton.setOnClickListener { toggleMuted() }
        navigationBar.closeChannelButton.setOnClickListener { closeChannel() }

        uiContainer = ScreencastUIContainer(this, layout!!)
    }

    private fun launchActivity() {
        val intent = createBoundActivityIntent()
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun createBoundActivityIntent(): Intent {
        val intent = Intent(this, Class.forName(req!!.boundActivityName!!))
        intent.putExtra("CHANNEL_ID", req!!.channelId)
        return intent
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
        muted = !muted
        localAudioTrack?.setEnabled(!muted)
        val navigationBar = layout!!.navigationBar
        val resourceId = if (muted) {
            R.drawable.ic_mic_white_48dp
        } else {
            R.drawable.ic_mic_off_black_48dp
        }
        navigationBar.toggleMutedButton.setImageResource(resourceId)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> changeCaptureFormat()
            Configuration.ORIENTATION_PORTRAIT -> changeCaptureFormat()
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

    private fun stopCapturer() {
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

    private fun openChannel() {

        capturer = ScreenCapturerAndroid(req!!.data, mediaProjectionCallback)

        if (capturer == null) {
            SoraLogger.w(TAG, "failed to obtain screen capturer")
            return
        }

        val mediaOption = SoraMediaOption().apply {
            // when audio stream is disabled, it'll crush.
            enableAudioUpstream()
            enableVideoUpstream(capturer!!, egl?.eglBaseContext)
            videoCodec = SoraVideoOption.Codec.valueOf(req!!.videoCodec!!)
            audioCodec = SoraAudioOption.Codec.valueOf(req!!.audioCodec!!)

            if (req!!.multistream) {
                enableMultistream()
            }

            audioOption = SoraAudioOption().apply {
                useHardwareAcousticEchoCanceler = true
                useHardwareNoiseSuppressor = true

                audioProcessingEchoCancellation = true
                audioProcessingAutoGainControl = true
                audioProcessingHighpassFilter = true
                audioProcessingNoiseSuppression = true
            }
        }

        val signalingEndpointCandidates = req!!.signalingEndpoint!!.split(",").map { it.trim() }
        val signalingMetadata = Gson().fromJson(req!!.signalingMetadata!!, Map::class.java)
        mediaChannel = SoraMediaChannel(
            context = this,
            signalingEndpointCandidates = signalingEndpointCandidates,
            channelId = req!!.channelId,
            signalingMetadata = signalingMetadata,
            mediaOption = mediaOption,
            listener = channelListener
        )
        mediaChannel!!.connect()
    }

    private fun closeChannel() {
        val handler = Handler()
        handler.post {
            SoraLogger.d(TAG, "closeChannel")
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
