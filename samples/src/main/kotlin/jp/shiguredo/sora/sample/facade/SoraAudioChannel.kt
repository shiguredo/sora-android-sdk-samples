package jp.shiguredo.sora.sample.facade

import android.content.Context
import android.os.Handler
import jp.shiguredo.sora.sample.option.SoraStreamType
import jp.shiguredo.sora.sdk.channel.SoraMediaChannel
import jp.shiguredo.sora.sdk.channel.data.ChannelAttendeesCount
import jp.shiguredo.sora.sdk.channel.option.SoraAudioOption
import jp.shiguredo.sora.sdk.channel.option.SoraMediaOption
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import jp.shiguredo.sora.sdk.util.SoraLogger
import org.webrtc.AudioTrack
import org.webrtc.MediaStream
import org.webrtc.PeerConnection

class SoraAudioChannel(
        private val context:           Context,
        private val handler:           Handler,
        private val signalingEndpoint: String,
        private val channelId:         String,
        private val signalingMetadata: String = "",
        private var streamType:        SoraStreamType,
        private var codec:             SoraAudioOption.Codec = SoraAudioOption.Codec.OPUS,
        private var sdpSemantics:      PeerConnection.SdpSemantics =
                PeerConnection.SdpSemantics.UNIFIED_PLAN,
        private var listener:          Listener?
) {

    val TAG = SoraAudioChannel::class.simpleName

    interface Listener {
        fun onConnect(channel: SoraAudioChannel) {}
        fun onClose(channel: SoraAudioChannel) {}
        fun onError(channel: SoraAudioChannel, reason: SoraErrorReason) {}
        fun onAttendeesCountUpdated(channel: SoraAudioChannel, attendees: ChannelAttendeesCount) {}
    }

    private val channelListener = object : SoraMediaChannel.Listener {

        override fun onConnect(mediaChannel: SoraMediaChannel) {
            SoraLogger.d(TAG, "[audio_channel] @onConnected")
            handler.post { listener?.onConnect(this@SoraAudioChannel) }
        }

        override fun onClose(mediaChannel: SoraMediaChannel) {
            SoraLogger.d(TAG, "[audio_channel] @onClose")
            disconnect()
        }

        override fun onError(mediaChannel: SoraMediaChannel, reason: SoraErrorReason) {
            SoraLogger.d(TAG, "[audio_channel] @onError")
            handler.post { listener?.onError(this@SoraAudioChannel, reason) }
            disconnect()
        }

        override fun onAddLocalStream(mediaChannel: SoraMediaChannel, ms: MediaStream) {
            SoraLogger.d(TAG, "[audio_channel] @onAddLocalStream")
            if (ms.audioTracks.size > 0) {
                localAudioTrack = ms.audioTracks[0]
            }
        }

        override fun onAttendeesCountUpdated(mediaChannel: SoraMediaChannel, attendees: ChannelAttendeesCount) {
            SoraLogger.d(TAG, "[audio_channel] @onAttendeesCountUpdated")
            handler.post { listener?.onAttendeesCountUpdated(this@SoraAudioChannel, attendees) }
        }

    }

    private var mediaChannel:    SoraMediaChannel? = null
    private var localAudioTrack: AudioTrack?  = null

    private var closed = false

    fun connect() {

        val mediaOption = SoraMediaOption().apply {

            if (streamType.hasUpstream()) {
                enableAudioUpstream()
            }

            if (streamType.hasDownstream()) {
                enableAudioDownstream()
            }

            if (streamType.hasMultistream()) {
                enableMultistream()
            }

            audioCodec = this@SoraAudioChannel.codec
            sdpSemantics = this@SoraAudioChannel.sdpSemantics
        }

        mediaChannel = SoraMediaChannel(
                context           = context,
                signalingEndpoint = signalingEndpoint,
                channelId         = channelId,
                signalingMetadata = signalingMetadata,
                mediaOption       = mediaOption,
                listener          = channelListener
        )

        mediaChannel!!.connect()
    }

    fun disconnect() {
        mediaChannel?.disconnect()
        mediaChannel = null
        if (!closed) {
            closed = true
            handler.post {
                listener?.onClose(this@SoraAudioChannel)
                localAudioTrack = null
            }
        }
    }

    fun dispose() {
        disconnect()
        listener = null
    }
}

