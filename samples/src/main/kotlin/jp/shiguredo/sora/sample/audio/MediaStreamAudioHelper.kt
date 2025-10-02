package jp.shiguredo.sora.sample.audio

import org.webrtc.AudioSink
import org.webrtc.AudioTrack
import org.webrtc.MediaStream

/**
 * MediaStreamとAudioTrackにAudioSinkを適用するためのヘルパー関数
 */
object MediaStreamAudioHelper {

    /**
     * MediaStreamの全ての音声トラックにAudioSinkを追加
     */
    fun MediaStream.attachAudioSink(sink: AudioSink) {
        audioTracks.forEach { audioTrack ->
            try {
                audioTrack.addSink(sink)
            } catch (e: Exception) {
                // AudioSinkの追加に失敗した場合はログに記録
                android.util.Log.w("MediaStreamAudioHelper", "Failed to add AudioSink to track ${audioTrack.id()}", e)
            }
        }
    }

    /**
     * MediaStreamの全ての音声トラックからAudioSinkを削除
     */
    fun MediaStream.detachAudioSink(sink: AudioSink) {
        audioTracks.forEach { audioTrack ->
            try {
                audioTrack.removeSink(sink)
            } catch (e: Exception) {
                // AudioSinkの削除に失敗した場合はログに記録
                android.util.Log.w("MediaStreamAudioHelper", "Failed to remove AudioSink from track ${audioTrack.id()}", e)
            }
        }
    }

    /**
     * 特定のAudioTrackにAudioSinkを追加
     */
    fun AudioTrack.attachAudioSink(sink: AudioSink) {
        try {
            addSink(sink)
        } catch (e: Exception) {
            android.util.Log.w("MediaStreamAudioHelper", "Failed to add AudioSink to track ${id()}", e)
        }
    }

    /**
     * 特定のAudioTrackからAudioSinkを削除
     */
    fun AudioTrack.detachAudioSink(sink: AudioSink) {
        try {
            removeSink(sink)
        } catch (e: Exception) {
            android.util.Log.w("MediaStreamAudioHelper", "Failed to remove AudioSink from track ${id()}", e)
        }
    }
}
