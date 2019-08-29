package jp.shiguredo.sora.sample.facade

import android.content.Context
import jp.shiguredo.sora.sample.camera.CameraVideoCapturerFactory
import jp.shiguredo.sora.sample.camera.DefaultCameraVideoCapturerFactory
import jp.shiguredo.sora.sample.option.SoraStreamType
import jp.shiguredo.sora.sample.ui.util.SoraRemoteRendererSlot
import jp.shiguredo.sora.sdk.channel.SoraMediaChannel
import jp.shiguredo.sora.sdk.channel.data.ChannelAttendeesCount
import jp.shiguredo.sora.sdk.channel.option.SoraAudioOption
import jp.shiguredo.sora.sdk.channel.option.SoraMediaOption
import jp.shiguredo.sora.sdk.channel.option.SoraVideoOption
import jp.shiguredo.sora.sdk.channel.signaling.message.NotificationMessage
import jp.shiguredo.sora.sdk.channel.signaling.message.PushMessage
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import jp.shiguredo.sora.sdk.util.SoraLogger
import org.webrtc.*
import android.os.Handler
import jp.shiguredo.sora.sample.stats.VideoUpstreamLatencyStatsCollector
import jp.shiguredo.sora.sdk.channel.option.PeerConnectionOption

class SoraVideoChannel(
        private val context:                 Context,
        private val handler:                 Handler,
        private val signalingEndpoint:       String,
        private val channelId:               String,
        private val signalingMetadata:       Any? = "",
        private val signalingNotifyMetatada: Any? = null,
        private val clientId:                String? = null,
        private val spotlight:               Int = 0,
        private var streamType:              SoraStreamType,
        private var videoEnabled:            Boolean = true,
        private val videoWidth:              Int = SoraVideoOption.FrameSize.Portrait.VGA.x,
        private val videoHeight:             Int = SoraVideoOption.FrameSize.Portrait.VGA.y,
        private val simulcast:               Boolean = false,
        private val videoFPS:                Int =  30,
        private val fixedResolution:         Boolean = false,
        private val videoCodec:              SoraVideoOption.Codec = SoraVideoOption.Codec.VP9,
        private val audioCodec:              SoraAudioOption.Codec = SoraAudioOption.Codec.OPUS,
        private val videoBitRate:            Int? = null,
        private val audioBitRate:            Int? = null,
        private val needLocalRenderer:       Boolean = true,
        private val audioEnabled:            Boolean = true,
        private val sdpSemantics:            PeerConnection.SdpSemantics =
                PeerConnection.SdpSemantics.UNIFIED_PLAN,
        private val capturerFactory:         CameraVideoCapturerFactory =
                DefaultCameraVideoCapturerFactory(context, fixedResolution),
        private var listener:                Listener?
) {

    companion object {
        private val TAG = SoraVideoChannel::class.simpleName
    }

    private var egl: EglBase? = EglBase.create()

    interface Listener {
        fun onConnect(channel: SoraVideoChannel) {}
        fun onClose(channel: SoraVideoChannel) {}
        fun onError(channel: SoraVideoChannel, reason: SoraErrorReason) {}
        fun onAddRemoteRenderer(channel: SoraVideoChannel, renderer: SurfaceViewRenderer) {}
        fun onRemoveRemoteRenderer(channel: SoraVideoChannel, renderer: SurfaceViewRenderer) {}
        fun onAddLocalRenderer(channel: SoraVideoChannel, renderer: SurfaceViewRenderer) {}
        fun onAttendeesCountUpdated(channel: SoraVideoChannel, attendees: ChannelAttendeesCount) {}
    }

    private val statsCollector = VideoUpstreamLatencyStatsCollector()

    private val channelListener = object : SoraMediaChannel.Listener {

        override fun onConnect(mediaChannel: SoraMediaChannel) {
            SoraLogger.d(TAG, "[video_channel] @onConnected")
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

        override fun onAddRemoteStream(mediaChannel: SoraMediaChannel, ms: MediaStream) {
            SoraLogger.d(TAG, "[video_channel] @onAddRemoteStream:${ms.id}")
            handler.post {
                remoteRenderersSlot?.onAddRemoteStream(ms)
            }
        }

        override fun onRemoveRemoteStream(mediaChannel: SoraMediaChannel, label: String) {
            SoraLogger.d(TAG, "[video_channel] @onRemoveRemoteStream:${label}")
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

        override fun onNotificationMessage(mediaChannel: SoraMediaChannel, notification: NotificationMessage) {
            SoraLogger.d(TAG, "[video_channel] @onNotificationmessage ${notification.eventType} ${notification}")
        }

        override fun onPushMessage(mediaChannel: SoraMediaChannel, push: PushMessage) {
            SoraLogger.d(TAG, "[video_channel] @onPushMessage ${push}")
        }

        override fun onPeerConnectionStatsReady(mediaChannel: SoraMediaChannel, statsReport: RTCStatsReport) {
            // statsReport.statsMap.entries.forEach {
            //     SoraLogger.d(TAG, "${it.key}=${it.value}")
            // }
            statsCollector.newStatsReport(statsReport)
        }
    }

    private var mediaChannel:  SoraMediaChannel? = null
    private var capturer: CameraVideoCapturer? = null

    private var capturing = false

    private var closed    = false

    private var remoteRenderersSlot: SoraRemoteRendererSlot? = null
    private var localRenderer:       SurfaceViewRenderer? = null
    private var localAudioTrack:     AudioTrack? = null

    private val rendererSlotListener =  object : SoraRemoteRendererSlot.Listener {

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
                context    = context,
                eglContext = egl!!.eglBaseContext,
                listener   = rendererSlotListener
        )

        val mediaOption = SoraMediaOption().apply {

            if (streamType.hasUpstream()) {
                if (audioEnabled) {
                    enableAudioUpstream()
                }
                if (videoEnabled) {
                    capturer = capturerFactory.createCapturer()
                    enableVideoUpstream(capturer!!, egl!!.eglBaseContext)
                }
            }

            if (streamType.hasDownstream()) {
                if (audioEnabled) {
                    enableAudioDownstream()
                }
                enableVideoDownstream(egl!!.eglBaseContext)
            }

            if (streamType.hasMultistream()) {
                enableMultistream()
            }

            if(this@SoraVideoChannel.simulcast) {
                enableSimulcast()
                // hardware encoder では動かせていない、ソフトウェアを指定する
                videoEncoderFactory = SoftwareVideoEncoderFactory()
            }
            spotlight    = this@SoraVideoChannel.spotlight
            videoCodec   = this@SoraVideoChannel.videoCodec
            videoBitrate = this@SoraVideoChannel.videoBitRate

            audioCodec   = this@SoraVideoChannel.audioCodec
            audioBitrate = this@SoraVideoChannel.audioBitRate

            // 全部デフォルト値なので、実際には指定する必要はない
            audioOption = SoraAudioOption().apply {
                useHardwareAcousticEchoCanceler = true
                useHardwareNoiseSuppressor      = true

                audioProcessingEchoCancellation = true
                audioProcessingAutoGainControl  = true
                audioProcessingHighpassFilter   = true
                audioProcessingNoiseSuppression = true
            }
        }

        val peerConnectionOption = PeerConnectionOption().apply {
            getStatsIntervalMSec = 5000
        }

        mediaChannel = SoraMediaChannel(
                context                 = context,
                signalingEndpoint       = signalingEndpoint,
                channelId               = channelId,
                signalingMetadata       = signalingMetadata,
                signalingNotifyMetadata = signalingNotifyMetatada,
                mediaOption             = mediaOption,
                listener                = channelListener,
                clientId                = clientId,
                peerConnectionOption    = peerConnectionOption
        )
        mediaChannel!!.connect()
    }

    fun startCapturer() {
        capturer?.let {
            if (!capturing) {
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
            SoraLogger.w(TAG, "failed to switch camera ${msg}")
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

