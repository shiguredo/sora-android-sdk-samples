package jp.shiguredo.sora.sample.facade

import android.content.Context
import android.os.Handler
import jp.shiguredo.sora.sample.option.SoraRoleType
import jp.shiguredo.sora.sdk.channel.SoraCloseEvent
import jp.shiguredo.sora.sdk.channel.SoraMediaChannel
import jp.shiguredo.sora.sdk.channel.data.ChannelAttendeesCount
import jp.shiguredo.sora.sdk.channel.option.SoraAudioOption
import jp.shiguredo.sora.sdk.channel.option.SoraMediaOption
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import jp.shiguredo.sora.sdk.util.SoraLogger
import org.webrtc.AudioTrack
import org.webrtc.AudioTrackSink
import org.webrtc.MediaStream

class SoraAudioChannel(
    private val context: Context,
    private val handler: Handler,
    private val signalingEndpoint: String? = null,
    private val signalingEndpointCandidates: List<String> = emptyList(),
    private val channelId: String,
    private val dataChannelSignaling: Boolean? = null,
    private val ignoreDisconnectWebSocket: Boolean? = null,
    private val signalingMetadata: Any? = "",
    private var roleType: SoraRoleType,
    private var audioCodec: SoraAudioOption.Codec = SoraAudioOption.Codec.DEFAULT,
    private val audioBitRate: Int? = null,
    private var listener: Listener?,
) {
    companion object {
        private val TAG = SoraAudioChannel::class.simpleName
    }

    interface Listener {
        fun onConnect(channel: SoraAudioChannel) {}

        fun onClose(channel: SoraAudioChannel) {}

        fun onError(
            channel: SoraAudioChannel,
            reason: SoraErrorReason,
            message: String,
        ) {}

        fun onAttendeesCountUpdated(
            channel: SoraAudioChannel,
            attendees: ChannelAttendeesCount,
        ) {}

        fun onAudioVolumeUpdate(
            channel: SoraAudioChannel,
            streamId: String,
            volume: Float,
        ) {}
    }

    private val channelListener =
        object : SoraMediaChannel.Listener {
            override fun onConnect(mediaChannel: SoraMediaChannel) {
                SoraLogger.d(
                    TAG,
                    "[audio_channel] @onConnect contactSignalingEndpoint:${mediaChannel.contactSignalingEndpoint} " +
                        "connectedSignalingEndpoint:${mediaChannel.connectedSignalingEndpoint}",
                )
                handler.post { listener?.onConnect(this@SoraAudioChannel) }
            }

            override fun onClose(
                mediaChannel: SoraMediaChannel,
                closeEvent: SoraCloseEvent,
            ) {
                SoraLogger.d(TAG, "[audio_channel] @onClose $closeEvent")
                disconnect()
            }

            override fun onError(
                mediaChannel: SoraMediaChannel,
                reason: SoraErrorReason,
                message: String,
            ) {
                SoraLogger.d(TAG, "[audio_channel] @onError [$reason]: $message")
                handler.post { listener?.onError(this@SoraAudioChannel, reason, message) }
                disconnect()
            }

            override fun onAddLocalStream(
                mediaChannel: SoraMediaChannel,
                ms: MediaStream,
            ) {
                SoraLogger.d(TAG, "[audio_channel] @onAddLocalStream streamId=${ms.id}")
                if (ms.audioTracks.size > 0) {
                    localAudioTrack = ms.audioTracks[0]
                    val sink = createAudioVolumeSink(ms.id)
                    audioTrackSinks[ms.id] = sink
                    localAudioTrack?.addSink(sink)
                }
            }

            override fun onAddRemoteStream(
                mediaChannel: SoraMediaChannel,
                ms: MediaStream,
            ) {
                SoraLogger.d(TAG, "[audio_channel] @onAddRemoteStream streamId=${ms.id}")
                if (ms.audioTracks.size > 0) {
                    val remoteAudioTrack = ms.audioTracks[0]
                    val sink = createAudioVolumeSink(ms.id)
                    audioTrackSinks[ms.id] = sink
                    remoteAudioTrack.addSink(sink)
                }
            }

            override fun onAttendeesCountUpdated(
                mediaChannel: SoraMediaChannel,
                attendees: ChannelAttendeesCount,
            ) {
                SoraLogger.d(TAG, "[audio_channel] @onAttendeesCountUpdated")
                handler.post { listener?.onAttendeesCountUpdated(this@SoraAudioChannel, attendees) }
            }
        }

    private var mediaChannel: SoraMediaChannel? = null
    private var localAudioTrack: AudioTrack? = null
    private val audioTrackSinks = mutableMapOf<String, AudioTrackSink>()

    private var closed = false

    fun connect() {
        val mediaOption =
            SoraMediaOption().apply {
                if (roleType.hasUpstream()) {
                    enableAudioUpstream()
                }

                if (roleType.hasDownstream()) {
                    enableAudioDownstream()
                }

                audioCodec = this@SoraAudioChannel.audioCodec
                audioBitrate = this@SoraAudioChannel.audioBitRate
            }

        mediaChannel =
            SoraMediaChannel(
                context = context,
                signalingEndpoint = signalingEndpoint,
                signalingEndpointCandidates = signalingEndpointCandidates,
                channelId = channelId,
                dataChannelSignaling = dataChannelSignaling,
                ignoreDisconnectWebSocket = ignoreDisconnectWebSocket,
                signalingMetadata = signalingMetadata,
                mediaOption = mediaOption,
                listener = channelListener,
            )

        mediaChannel!!.connect()
    }

    fun disconnect() {
        audioTrackSinks.clear()
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

    private fun createAudioVolumeSink(streamId: String): AudioTrackSink {
        return object : AudioTrackSink {
            private var lastUpdateTime = 0L
            private val updateIntervalMs = 50L

            override fun onData(
                audioData: java.nio.ByteBuffer,
                bitsPerSample: Int,
                sampleRate: Int,
                numberOfChannels: Int,
                numberOfFrames: Int,
            ) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdateTime >= updateIntervalMs) {
                    val volume = calculatePeakVolume(audioData, numberOfChannels)
                    handler.post {
                        listener?.onAudioVolumeUpdate(
                            this@SoraAudioChannel,
                            streamId,
                            volume,
                        )
                    }
                    lastUpdateTime = currentTime
                }
            }

            private fun calculatePeakVolume(
                audioData: java.nio.ByteBuffer,
                numberOfChannels: Int,
            ): Float {
                if (audioData.remaining() == 0) return 0f

                // ByteBufferの位置を保存
                val originalPosition = audioData.position()
                var maxAmplitude = 0
                val sampleCount = audioData.remaining() / 2

                for (i in 0 until sampleCount step numberOfChannels) {
                    val byteIndex = originalPosition + i * 2
                    if (byteIndex + 1 < originalPosition + audioData.remaining()) {
                        val sample =
                            (audioData.get(byteIndex).toInt() and 0xFF) or
                                (audioData.get(byteIndex + 1).toInt() shl 8)
                        val signedSample = if (sample > 32767) sample - 65536 else sample
                        maxAmplitude = kotlin.math.max(maxAmplitude, kotlin.math.abs(signedSample))
                    }
                }

                return (maxAmplitude / 32768.0f).coerceIn(0f, 1f)
            }
        }
    }
}
