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
import android.os.Looper
import android.view.LayoutInflater
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.databinding.ScreencastServiceBinding
import jp.shiguredo.sora.sample.ui.util.SoraScreenUtil
import jp.shiguredo.sora.sdk.channel.SoraCloseEvent
import jp.shiguredo.sora.sdk.channel.SoraMediaChannel
import jp.shiguredo.sora.sdk.channel.option.SoraAudioOption
import jp.shiguredo.sora.sdk.channel.option.SoraMediaOption
import jp.shiguredo.sora.sdk.channel.option.SoraVideoOption
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import jp.shiguredo.sora.sdk.util.SoraLogger
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

        /**
         * 配信停止を配信画面に通知するブロードキャストのアクションです。
         */
        const val ACTION_SCREENCAST_STOPPED = "jp.shiguredo.sora.sample.ACTION_SCREENCAST_STOPPED"

        private var running = false

        fun isRunning() = running
    }

    private var localAudioTrack: AudioTrack? = null
    private var mediaChannel: SoraMediaChannel? = null
    private var capturer: VideoCapturer? = null
    private var req: ScreencastRequest? = null
    private var egl: EglBase? = null
    private var uiContainer: ScreencastUIContainer? = null
    private var binding: ScreencastServiceBinding? = null

    private var capturing = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isClosing = false

    private val channelListener =
        object : SoraMediaChannel.Listener {
            override fun onConnect(mediaChannel: SoraMediaChannel) {
                SoraLogger.d(TAG, "[screencast] @onConnected")
            }

            override fun onClose(
                mediaChannel: SoraMediaChannel,
                closeEvent: SoraCloseEvent,
            ) {
                SoraLogger.d(TAG, "[screencast] @onClose $closeEvent")
                closeChannel()
            }

            override fun onError(
                mediaChannel: SoraMediaChannel,
                reason: SoraErrorReason,
                message: String,
            ) {
                SoraLogger.d(TAG, "[screencast] @onError [$reason]: $message")
            }

            override fun onAddLocalStream(
                mediaChannel: SoraMediaChannel,
                ms: MediaStream,
            ) {
                SoraLogger.d(TAG, "[screencast] @onAddLocalStream")
                if (ms.audioTracks.size > 0) {
                    localAudioTrack = ms.audioTracks[0]
                    localAudioTrack?.setEnabled(!muted)
                }
                startCapturer()
            }
        }

    override fun onStartCommand(
        intent: Intent,
        flags: Int,
        startId: Int,
    ): Int {
        // 同じ Service インスタンスへの二重開始は受け付けない
        if (SoraScreencastService.running || isClosing) {
            SoraLogger.w(TAG, "service is already running or closing")
            return Service.START_NOT_STICKY
        }
        SoraScreencastService.running = true

        egl = EglBase.create()
        req =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("SCREENCAST_REQUEST", ScreencastRequest::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("SCREENCAST_REQUEST")
            }
        if (req == null) {
            SoraLogger.w(TAG, "request not found")
            SoraScreencastService.running = false
            stopSelf()
            return Service.START_NOT_STICKY
        }

        setupUI()
        startNotification(startId)
        // 配信する画面を起動する
        launchActivity()
        openChannel()

        return START_NOT_STICKY
    }

    private fun startNotification(startId: Int) {
        val notificationChannelId = createNotificationChannel()

        val builder =
            NotificationCompat
                .Builder(this, notificationChannelId)
                .setContentTitle(req!!.stateTitle)
                .setContentText(req!!.stateText)
                .setSmallIcon(req!!.notificationIcon)
        createBoundActivityIntent()?.let {
            builder.setContentIntent(PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_MUTABLE))
        }
        startForeground(startId, builder.build())
    }

    private fun createNotificationChannel(): String {
        val notificationChannelId = "jp.shiguredo.sora.sample"
        val notificationChannelName = "Sora SDK Sample"
        val channel =
            NotificationChannel(
                notificationChannelId,
                notificationChannelName,
                NotificationManager.IMPORTANCE_NONE,
            )
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return notificationChannelId
    }

    private fun setupUI() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        binding = ScreencastServiceBinding.inflate(inflater)
        binding!!.root.setPadding(0, SoraScreenUtil.statusBarHeight(this), 0, 0)
        binding!!.launchActivityView.setImageResource(req!!.notificationIcon)
        binding!!.togglePositionButton.setImageResource(R.drawable.ic_unfold_more_white_48dp)
        binding!!.stateText.text = "${req!!.stateTitle}\n${req!!.stateText}"
        binding!!.toggleMutedButton.setImageResource(R.drawable.ic_mic_white_48dp)
        binding!!.closeChannelButton.setImageResource(R.drawable.ic_close_white_48dp)

        binding!!.togglePositionButton.setOnClickListener { toggleNavigationBarPosition() }
        binding!!.launchActivityView.setOnClickListener { launchActivity() }
        binding!!.toggleMutedButton.setOnClickListener { toggleMuted() }
        binding!!.closeChannelButton.setOnClickListener { closeChannel() }

        uiContainer = ScreencastUIContainer(this, binding!!.root)
    }

    /**
     * 配信画面を起動します。配信開始時と、オーバーレイの起動ボタンから呼び出されます。
     */
    private fun launchActivity() {
        val intent = createBoundActivityIntent() ?: return
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        try {
            startActivity(intent)
            SoraLogger.d(TAG, "launchActivity")
        } catch (e: Exception) {
            SoraLogger.w(TAG, "failed to launch activity: ${e.message}")
        }
    }

    private fun createBoundActivityIntent(): Intent? {
        val activityName = req?.boundActivityName ?: return null
        return try {
            Intent(this, Class.forName(activityName))
        } catch (e: ClassNotFoundException) {
            SoraLogger.w(TAG, "bound activity not found: $activityName")
            null
        }
    }

    override fun onDestroy() {
        closeChannel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private var muted = true

    private fun toggleMuted() {
        muted = !muted
        localAudioTrack?.setEnabled(!muted)
        val resourceId =
            if (muted) {
                R.drawable.ic_mic_white_48dp
            } else {
                R.drawable.ic_mic_off_black_48dp
            }
        binding?.toggleMutedButton?.setImageResource(resourceId)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE,
            Configuration.ORIENTATION_PORTRAIT,
            -> changeCaptureFormat()
            else -> Unit
        }
    }

    private fun changeCaptureFormat() {
        capturer?.let {
            val size = SoraScreenUtil.size(this)
            it.changeCaptureFormat(
                Math.round(size.x * req!!.videoScale),
                Math.round(size.y * req!!.videoScale),
                req!!.videoFPS,
            )
        }
    }

    internal fun startCapturer() {
        capturer?.let {
            if (!capturing) {
                capturing = true
                SoraLogger.d(TAG, "startCapture")
                val size = SoraScreenUtil.size(this)
                /*
                 * Pixel シリーズでアプリ単位の共有時にこのサンプルを選び、
                 * 下記の処理を呼び出した場合は失敗してしまう。
                 * それに対する対策として ScreencastSetupActivity を
                 * android:launchMode="singleInstance" にしている
                 */
                it.startCapture(
                    Math.round(size.x * req!!.videoScale),
                    Math.round(size.y * req!!.videoScale),
                    req!!.videoFPS,
                )
            }
        }
    }

    private fun stopCapturer() {
        capturer?.let {
            if (capturing) {
                capturing = false
                SoraLogger.d(TAG, "stopCapture")
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

        val mediaOption =
            SoraMediaOption().apply {
                // when audio stream is disabled, it'll crush.
                enableAudioUpstream()
                enableVideoUpstream(capturer!!, egl?.eglBaseContext)
                videoCodec = SoraVideoOption.Codec.valueOf(req!!.videoCodec!!)
                audioCodec = SoraAudioOption.Codec.valueOf(req!!.audioCodec!!)

                audioOption =
                    SoraAudioOption().apply {
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
        mediaChannel =
            SoraMediaChannel(
                context = this,
                signalingEndpointCandidates = signalingEndpointCandidates,
                channelId = req!!.channelId,
                signalingMetadata = signalingMetadata,
                mediaOption = mediaOption,
                listener = channelListener,
            )
        mediaChannel!!.connect()
    }

    private fun closeChannel() {
        mainHandler.post {
            if (isClosing) {
                return@post
            }
            isClosing = true
            SoraLogger.d(TAG, "closeChannel")
            SoraScreencastService.running = false
            val stoppedIntent = Intent(ACTION_SCREENCAST_STOPPED)
            stoppedIntent.setPackage(applicationContext.packageName)
            sendBroadcast(stoppedIntent)
            mediaChannel?.disconnect()
            mediaChannel = null
            stopCapturer()
            egl?.release()
            egl = null
            uiContainer?.clear()
            NetworkMonitor.getInstance().stopMonitoring()
            stopSelf()
        }
    }
}
