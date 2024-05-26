package jp.shiguredo.sora.sample.facade

import android.content.Context
import android.media.MediaRecorder
import android.os.Handler
import jp.shiguredo.sora.sample.BuildConfig
import jp.shiguredo.sora.sample.camera.CameraVideoCapturerFactory
import jp.shiguredo.sora.sample.camera.DefaultCameraVideoCapturerFactory
import jp.shiguredo.sora.sample.option.SoraRoleType
import jp.shiguredo.sora.sample.stats.VideoUpstreamLatencyStatsCollector
import jp.shiguredo.sora.sample.ui.util.SoraRemoteRendererSlot
import jp.shiguredo.sora.sdk.channel.SoraMediaChannel
import jp.shiguredo.sora.sdk.channel.data.ChannelAttendeesCount
import jp.shiguredo.sora.sdk.channel.option.PeerConnectionOption
import jp.shiguredo.sora.sdk.channel.option.SoraAudioOption
import jp.shiguredo.sora.sdk.channel.option.SoraMediaOption
import jp.shiguredo.sora.sdk.channel.option.SoraSpotlightOption
import jp.shiguredo.sora.sdk.channel.option.SoraVideoOption
import jp.shiguredo.sora.sdk.channel.signaling.message.NotificationMessage
import jp.shiguredo.sora.sdk.channel.signaling.message.OfferMessage
import jp.shiguredo.sora.sdk.channel.signaling.message.PushMessage
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import jp.shiguredo.sora.sdk.util.SoraLogger
import org.webrtc.AudioTrack
import org.webrtc.CameraVideoCapturer
import org.webrtc.EglBase
import org.webrtc.MediaStream
import org.webrtc.ProxyType
import org.webrtc.RTCStatsReport
import org.webrtc.RtpParameters
import org.webrtc.SurfaceViewRenderer
import java.lang.Exception

class SoraVideoChannel(
    private val context: Context,
    private val handler: Handler,
    private val signalingEndpoint: String? = null,
    private val signalingEndpointCandidates: List<String> = emptyList(),
    private val channelId: String,
    private val dataChannelSignaling: Boolean? = null,
    private val ignoreDisconnectWebSocket: Boolean? = null,
    private val signalingMetadata: Any? = "",
    private val signalingNotifyMetatada: Any? = null,
    private val clientId: String? = null,
    private val bundleId: String? = null,
    private val spotlight: Boolean = false,
    private val spotlightNumber: Int? = null,
    private val spotlightFocusRid: SoraVideoOption.SpotlightRid? = null,
    private val spotlightUnfocusRid: SoraVideoOption.SpotlightRid? = null,
    private var roleType: SoraRoleType = SoraRoleType.SENDRECV,
    private var multistream: Boolean = true,
    private var videoEnabled: Boolean = true,
    private val videoWidth: Int = SoraVideoOption.FrameSize.Portrait.VGA.x,
    private val videoHeight: Int = SoraVideoOption.FrameSize.Portrait.VGA.y,
    private val videoVp9Params: Any? = null,
    private val videoAv1Params: Any? = null,
    private val videoH264Params: Any? = null,
    private val simulcast: Boolean = false,
    private val simulcastMulticodec: Boolean? = null,
    private val simulcastRid: SoraVideoOption.SimulcastRid? = null,
    private val videoFPS: Int = 30,
    private val fixedResolution: Boolean = false,
    private val resolutionAdjustment: SoraVideoOption.ResolutionAdjustment? = null,
    private val cameraFacing: Boolean = true,
    private val videoCodec: SoraVideoOption.Codec = SoraVideoOption.Codec.VP9,
    private val audioCodec: SoraAudioOption.Codec = SoraAudioOption.Codec.OPUS,
    private val videoBitRate: Int? = null,
    private val audioBitRate: Int? = null,
    private val audioStereo: Boolean = false,
    private val needLocalRenderer: Boolean = true,
    private val audioEnabled: Boolean = true,
    private val audioStreamingLanguageCode: String? = null,
    private val capturerFactory: CameraVideoCapturerFactory =
        DefaultCameraVideoCapturerFactory(context, fixedResolution, cameraFacing),
    private var listener: Listener?
) {

    companion object {
        private val TAG = SoraVideoChannel::class.simpleName
    }

    private var egl: EglBase? = EglBase.create()

    interface Listener {
        fun onConnect(channel: SoraVideoChannel) {}
        fun onClose(channel: SoraVideoChannel) {}
        fun onError(channel: SoraVideoChannel, reason: SoraErrorReason) {}
        fun onWarning(channel: SoraVideoChannel, reason: SoraErrorReason) {}
        fun onAddRemoteRenderer(channel: SoraVideoChannel, renderer: SurfaceViewRenderer) {}
        fun onRemoveRemoteRenderer(channel: SoraVideoChannel, renderer: SurfaceViewRenderer) {}
        fun onAddLocalRenderer(channel: SoraVideoChannel, renderer: SurfaceViewRenderer) {}
        fun onAttendeesCountUpdated(channel: SoraVideoChannel, attendees: ChannelAttendeesCount) {}
    }

    private val statsCollector = VideoUpstreamLatencyStatsCollector()

    private val channelListener = object : SoraMediaChannel.Listener {

        override fun onConnect(mediaChannel: SoraMediaChannel) {
            SoraLogger.d(TAG, "[video_channel] @onConnect contactSignalingEndpoint:${mediaChannel.contactSignalingEndpoint} connectedSignalingEndpoint:${mediaChannel.connectedSignalingEndpoint}")
            handler.post {
                listener?.onConnect(this@SoraVideoChannel)
            }
        }

        override fun onClose(mediaChannel: SoraMediaChannel) {
            SoraLogger.d(TAG, "[video_channel] @onClose")
            disconnect()
        }

        override fun onError(mediaChannel: SoraMediaChannel, reason: SoraErrorReason) {
            SoraLogger.d(TAG, "[video_channel] @onError $reason")
            handler.post {
                listener?.onError(this@SoraVideoChannel, reason)
            }
        }

        override fun onError(mediaChannel: SoraMediaChannel, reason: SoraErrorReason, message: String) {
            SoraLogger.d(TAG, "[video_channel] @onError $reason: $message")
            handler.post {
                listener?.onError(this@SoraVideoChannel, reason)
            }
        }

        override fun onWarning(mediaChannel: SoraMediaChannel, reason: SoraErrorReason) {
            SoraLogger.d(TAG, "[video_channel] @onWarning $reason")
            handler.post {
                listener?.onWarning(this@SoraVideoChannel, reason)
            }
        }

        override fun onWarning(mediaChannel: SoraMediaChannel, reason: SoraErrorReason, message: String) {
            SoraLogger.d(TAG, "[video_channel] @onWarning $reason: $message")
            handler.post {
                listener?.onWarning(this@SoraVideoChannel, reason)
            }
        }

        override fun onSenderEncodings(mediaChannel: SoraMediaChannel, encodings: List<RtpParameters.Encoding>) {
            SoraLogger.d(TAG, "[video_channel] @onSenderEncodings: encodings=$encodings")
        }

        override fun onAddRemoteStream(mediaChannel: SoraMediaChannel, ms: MediaStream) {
            SoraLogger.d(TAG, "[video_channel] @onAddRemoteStream:${ms.id}")
            handler.post {
                remoteRenderersSlot?.onAddRemoteStream(ms)
            }
        }

        override fun onRemoveRemoteStream(mediaChannel: SoraMediaChannel, label: String) {
            SoraLogger.d(TAG, "[video_channel] @onRemoveRemoteStream:$label")
            handler.post {
                remoteRenderersSlot?.onRemoveRemoteStream(label)
            }
        }

        override fun onAddLocalStream(mediaChannel: SoraMediaChannel, ms: MediaStream) {
            SoraLogger.d(TAG, "[video_channel] @onAddLocalStream")

            if (ms.audioTracks.size > 0) {
                localAudioTrack = ms.audioTracks[0]
            }

            if (needLocalRenderer) {
                handler.post {
                    if (ms.videoTracks.size > 0) {
                        localRenderer = createSurfaceViewRenderer()
                        ms.videoTracks[0].addSink(localRenderer!!)
                        listener?.onAddLocalRenderer(this@SoraVideoChannel, localRenderer!!)
                    }
                }
            }
            handler.post {
                startCapturer()
            }
        }

        override fun onAttendeesCountUpdated(mediaChannel: SoraMediaChannel, attendees: ChannelAttendeesCount) {
            SoraLogger.d(TAG, "[video_channel] @onAttendeesCountUpdated")
            handler.post {
                listener?.onAttendeesCountUpdated(this@SoraVideoChannel, attendees)
            }
        }

        override fun onOfferMessage(mediaChannel: SoraMediaChannel, offer: OfferMessage) {
            SoraLogger.d(TAG, "[video_channel] @onOfferMessage $offer")
        }

        override fun onNotificationMessage(mediaChannel: SoraMediaChannel, notification: NotificationMessage) {
            SoraLogger.d(TAG, "[video_channel] @onNotificationmessage ${notification.eventType} $notification")
        }

        override fun onPushMessage(mediaChannel: SoraMediaChannel, push: PushMessage) {
            SoraLogger.d(TAG, "[video_channel] @onPushMessage $push")
        }

        override fun onPeerConnectionStatsReady(mediaChannel: SoraMediaChannel, statsReport: RTCStatsReport) {
            // statsReport.statsMap.entries.forEach {
            //     SoraLogger.d(TAG, "${it.key}=${it.value}")
            // }
            statsCollector.newStatsReport(statsReport)
        }
    }

    var mediaChannel: SoraMediaChannel? = null
    private var capturer: CameraVideoCapturer? = null

    private var capturing = false

    private var closed = false

    private var remoteRenderersSlot: SoraRemoteRendererSlot? = null
    private var localRenderer: SurfaceViewRenderer? = null
    private var localAudioTrack: AudioTrack? = null

    private val rendererSlotListener = object : SoraRemoteRendererSlot.Listener {

        override fun onAddRenderer(renderer: SurfaceViewRenderer) {
            handler.post {
                listener?.onAddRemoteRenderer(this@SoraVideoChannel, renderer)
            }
        }

        override fun onRemoveRenderer(renderer: SurfaceViewRenderer) {
            handler.post {
                listener?.onRemoveRemoteRenderer(this@SoraVideoChannel, renderer)
            }
        }
    }

    private fun createSurfaceViewRenderer(): SurfaceViewRenderer {
        val renderer = SurfaceViewRenderer(context)
        renderer.init(egl?.eglBaseContext, null)
        return renderer
    }

    fun connect() {

        remoteRenderersSlot = SoraRemoteRendererSlot(
            context = context,
            eglContext = egl!!.eglBaseContext,
            listener = rendererSlotListener
        )

        val mediaOption = SoraMediaOption().apply {
            if (roleType.hasUpstream()) {
                if (audioEnabled) {
                    enableAudioUpstream()
                }
                if (videoEnabled) {
                    capturer = capturerFactory.createCapturer()
                    enableVideoUpstream(capturer!!, egl!!.eglBaseContext)
                }
            }

            if (roleType.hasDownstream()) {
                if (audioEnabled) {
                    enableAudioDownstream()
                }
                if (videoEnabled || roleType == SoraRoleType.SENDRECV) {
                    enableVideoDownstream(egl!!.eglBaseContext)
                }
            }

            if (multistream) {
                enableMultistream()
            }

            if (this@SoraVideoChannel.simulcast) {
                enableSimulcast(simulcastRid)
            }

            if (this@SoraVideoChannel.simulcastMulticodec == true) {
                enableSimulcastMulticodec(simulcastRid)
            }

            if (this@SoraVideoChannel.spotlight) {
                val option = SoraSpotlightOption()
                option.spotlightNumber = spotlightNumber
                option.spotlightFocusRid = spotlightFocusRid
                option.spotlightUnfocusRid = spotlightUnfocusRid
                enableSpotlight(option, this@SoraVideoChannel.simulcast)
            }

            videoCodec = this@SoraVideoChannel.videoCodec
            videoBitrate = this@SoraVideoChannel.videoBitRate
            videoVp9Params = this@SoraVideoChannel.videoVp9Params
            videoAv1Params = this@SoraVideoChannel.videoAv1Params
            videoH264Params = this@SoraVideoChannel.videoH264Params

            audioCodec = this@SoraVideoChannel.audioCodec
            audioBitrate = this@SoraVideoChannel.audioBitRate

            audioOption = SoraAudioOption().apply {
                // 全部デフォルト値なので、実際には指定する必要はない
                useHardwareAcousticEchoCanceler = true
                useHardwareNoiseSuppressor = true

                audioProcessingEchoCancellation = true
                audioProcessingAutoGainControl = true
                audioProcessingHighpassFilter = true
                audioProcessingNoiseSuppression = true

                // 配信にステレオを使う場合の設定。
                // AndroidManifest で portrait 固定だが、ステレオで配信するときは landscape に
                // 変更したほうが良い。
                if (audioStereo) {
                    // libwebrtc の AGC が有効のときはステレオで出ないため無効化する
                    audioProcessingAutoGainControl = false

                    // MediaRecorder.AudioSource.MIC の場合、両側のマイクの真ん中あたりで
                    // 不連続に音量が下がるように聞こえる (Pixel 3 XL -> Sora で録音)。
                    // マイクから離れることに依る近接センサーの影響か??
                    // CAMCORDER にすると端末のマイクから離れたときの影響が小さい。
                    audioSource = MediaRecorder.AudioSource.CAMCORDER
                    useStereoInput = true
                }
            }

            if (resolutionAdjustment != null) {
                this.hardwareVideoEncoderResolutionAdjustment = resolutionAdjustment
            }

            // プロキシ
            if (BuildConfig.PROXY_HOSTNAME.isNotBlank()) {
                this.proxy.type = ProxyType.HTTPS
                this.proxy.hostname = BuildConfig.PROXY_HOSTNAME

                // エージェントは指定されている場合のみデフォルト値を上書きする
                if (BuildConfig.PROXY_AGENT.isNotBlank()) {
                    this.proxy.agent = BuildConfig.PROXY_AGENT
                }

                try {
                    this.proxy.port = BuildConfig.PROXY_PORT.toInt()
                } catch (e: Exception) {
                    SoraLogger.e(TAG, "failed to set SoraMediaOption.proxy.port", e)
                }

                if (BuildConfig.PROXY_USERNAME.isNotBlank()) {
                    this.proxy.username = BuildConfig.PROXY_USERNAME
                    this.proxy.password = BuildConfig.PROXY_PASSWORD
                }
            }
            audioStreamingLanguageCode = this@SoraVideoChannel.audioStreamingLanguageCode
        }

        val peerConnectionOption = PeerConnectionOption().apply {
            getStatsIntervalMSec = 5000
        }

        mediaChannel = SoraMediaChannel(
            context = context,
            signalingEndpoint = signalingEndpoint,
            signalingEndpointCandidates = signalingEndpointCandidates,
            channelId = channelId,
            dataChannelSignaling = dataChannelSignaling,
            ignoreDisconnectWebSocket = ignoreDisconnectWebSocket,
            signalingMetadata = signalingMetadata,
            signalingNotifyMetadata = signalingNotifyMetatada,
            mediaOption = mediaOption,
            listener = channelListener,
            clientId = clientId,
            bundleId = bundleId,
            peerConnectionOption = peerConnectionOption
        )
        mediaChannel!!.connect()
    }

    fun startCapturer() {
        capturer?.let {
            if (!capturing) {
                SoraLogger.d(TAG, "start capturer => width: $videoWidth, height: $videoHeight, fps: $videoFPS")
                capturing = true
                it.startCapture(videoWidth, videoHeight, videoFPS)
            }
        }
    }

    fun stopCapturer() {
        capturer?.let {
            if (capturing) {
                capturing = false
                it.stopCapture()
            }
        }
    }

    private val cameraSwitchHandler = object : CameraVideoCapturer.CameraSwitchHandler {

        override fun onCameraSwitchDone(isFront: Boolean) {
            SoraLogger.d(TAG, "camera switched.")
        }

        override fun onCameraSwitchError(msg: String?) {
            SoraLogger.w(TAG, "failed to switch camera $msg")
        }
    }

    fun changeCameraFormat(width: Int, height: Int, fps: Int) {
        capturer?.let {
            it.changeCaptureFormat(width, height, fps)
        }
    }

    fun switchCamera() {
        capturer?.let {
            it.switchCamera(cameraSwitchHandler)
        }
    }

    fun mute(mute: Boolean) {
        localAudioTrack?.setEnabled(!mute)
    }

    fun disconnect() {
        stopCapturer()
        mediaChannel?.disconnect()
        mediaChannel = null
        capturer = null

        if (!closed) {
            closed = true

            handler.post {
                listener?.onClose(this@SoraVideoChannel)

                localRenderer?.release()
                localRenderer = null

                localAudioTrack = null

                remoteRenderersSlot?.dispose()
                remoteRenderersSlot = null
            }
        }
    }

    fun dispose() {
        disconnect()
        egl?.release()
        egl = null
        listener = null
    }
}
