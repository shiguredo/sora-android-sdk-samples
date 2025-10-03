package jp.shiguredo.sora.sample.audio

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import org.webrtc.AudioSink
import org.webrtc.AudioTrack
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.log10

/**
 * 受信音声トラックの音量レベルを監視するAudioSink実装
 */
class VolumeMonitoringSink : AudioSink {

    // インスタンスを識別するためのID
    private val instanceId = System.currentTimeMillis().toString() + "-" + hashCode()

    data class VolumeLevel(
        val rmsVolume: Float,
        val peakVolume: Float,
        val rmsDb: Float,
        val peakDb: Float,
        val timestamp: Long = System.currentTimeMillis()
    )

    interface VolumeListener {
        fun onVolumeChanged(trackId: String, volumeLevel: VolumeLevel)
    }

    private val volumeListeners = mutableSetOf<VolumeListener>()
    private val trackVolumeData = ConcurrentHashMap<String, VolumeLevel>()
    private val mainHandler = Handler(Looper.getMainLooper())

    // 音量更新の頻度制御（50ms間隔で更新）
    private val updateInterval = 50L
    private var lastUpdateTime = 0L

    init {
        android.util.Log.d("VolumeMonitoringSink", "[kensaku] VolumeMonitoringSink作成: instanceId=$instanceId")
    }

    override fun onData(
        audioTrack: AudioTrack,
        audioData: ByteBuffer,
        bitsPerSample: Int,
        sampleRate: Int,
        numberOfChannels: Int,
        numberOfFrames: Int
    ) {
        val trackId = audioTrack.id()
        val currentTime = System.currentTimeMillis()

        // デバッグログ（頻度制限付き）
        if (currentTime - lastUpdateTime >= updateInterval) {
            android.util.Log.d("VolumeMonitoringSink", "[kensaku] onData呼び出し: instanceId=$instanceId, trackId=$trackId, frames=$numberOfFrames, channels=$numberOfChannels, sampleRate=$sampleRate")
        }

        // 更新頻度を制限
        if (currentTime - lastUpdateTime < updateInterval) {
            return
        }
        lastUpdateTime = currentTime

        // ByteBufferからバイト配列に変換
        val audioBytes = ByteArray(audioData.remaining())
        audioData.get(audioBytes)
        audioData.rewind() // 再利用のためにバッファを巻き戻し

        // 音量レベルを計算
        val rmsVolume = AudioVolumeCalculator.calculateRmsVolume(audioBytes, numberOfChannels)
        val peakVolume = AudioVolumeCalculator.calculatePeakVolume(audioBytes, numberOfChannels)
        val rmsDb = AudioVolumeCalculator.volumeToDb(rmsVolume)
        val peakDb = AudioVolumeCalculator.volumeToDb(peakVolume)

        val volumeLevel = VolumeLevel(rmsVolume, peakVolume, rmsDb, peakDb, currentTime)

        android.util.Log.d("VolumeMonitoringSink", "[kensaku] 音量計算完了: trackId=$trackId, rmsVolume=$rmsVolume, peakVolume=$peakVolume")

        // データを保存
        trackVolumeData[trackId] = volumeLevel

        // メインスレッドでリスナーに通知
        mainHandler.post {
            volumeListeners.forEach { listener ->
                listener.onVolumeChanged(trackId, volumeLevel)
            }
        }
    }

    override fun getPreferredNumberOfChannels(): Int = -1 // チャンネル数の指定なし

    /**
     * 音量リスナーを追加
     */
    fun addVolumeListener(listener: VolumeListener) {
        volumeListeners.add(listener)
    }

    /**
     * 音量リスナーを削除
     */
    fun removeVolumeListener(listener: VolumeListener) {
        volumeListeners.remove(listener)
    }

    /**
     * 指定されたトラックの最新の音量レベルを取得
     */
    fun getVolumeLevel(trackId: String): VolumeLevel? {
        return trackVolumeData[trackId]
    }

    /**
     * 全トラックの音量データを取得
     */
    fun getAllVolumeLevels(): Map<String, VolumeLevel> {
        return trackVolumeData.toMap()
    }

    /**
     * 指定されたトラックの音量データをクリア
     */
    fun clearVolumeData(trackId: String) {
        trackVolumeData.remove(trackId)
    }

    /**
     * 全ての音量データをクリア
     */
    fun clearAllVolumeData() {
        trackVolumeData.clear()
    }
}
