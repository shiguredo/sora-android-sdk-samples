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

class SoraVideoChannel(
        private val context:           Context,
        private val handler:           Handler,
        private val signalingEndpoint: String,
        private val channelId:         String,
        private val signalingMetadata: String = "",
        private val spotlight:         Int = 0,
        private var streamType:        SoraStreamType,
        private var videoEnabled:      Boolean = true,
        var         videoWidth:        Int = SoraVideoOption.FrameSize.Portrait.VGA.x,
        var         videoHeight:       Int = SoraVideoOption.FrameSize.Portrait.VGA.y,
        var         videoFPS:          Int =  30,
        var         videoCodec:        SoraVideoOption.Codec = SoraVideoOption.Codec.VP9,
        var         audioCodec:        SoraAudioOption.Codec = SoraAudioOption.Codec.OPUS,
        var         videoBitrate:      Int? = null,
        private var needLocalRenderer: Boolean = true,
        private var audioEnabled:      Boolean = true,
        private var sdpSemantics:      PeerConnection.SdpSemantics =
                PeerConnection.SdpSemantics.UNIFIED_PLAN,
        private var capturerFactory:   CameraVideoCapturerFactory =
                DefaultCameraVideoCapturerFactory(context),
        private var listener:          Listener?
) {

    companion object {
        val TAG = SoraVideoChannel::class.simpleName
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
            SoraLogger.d(TAG, "[video_channel] @onError")
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
            SoraLogger.d(TAG, "[video_channel] @onNotificationmessage ${notification}")
            // SoraLogger.d(TAG, "metadata: ${notification.metadata} of " +
            //         notification.metadata?.javaClass)
            // SoraLogger.d(TAG, "metadata_list: ${notification.metadataList} of " +
            //         notification.metadataList?.javaClass)
        }

        override fun onPushMessage(mediaChannel: SoraMediaChannel, push: PushMessage) {
            SoraLogger.d(TAG, "[video_channel] @onPushMessage ${push}")
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

            spotlight    = this@SoraVideoChannel.spotlight
            videoCodec   = this@SoraVideoChannel.videoCodec
            audioCodec   = this@SoraVideoChannel.audioCodec
            videoBitrate = this@SoraVideoChannel.videoBitrate
            sdpSemantics = this@SoraVideoChannel.sdpSemantics
        }

        mediaChannel = SoraMediaChannel(
                context            = context,
                signalingEndpoint  = signalingEndpoint,
                channelId          = channelId,
                signalingMetadata  = signalingMetadata,
                mediaOption        = mediaOption,
                listener           = channelListener
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

