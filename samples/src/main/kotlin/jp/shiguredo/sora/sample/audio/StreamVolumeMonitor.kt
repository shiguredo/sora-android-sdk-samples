package jp.shiguredo.sora.sample.audio

import android.media.AudioManager
import android.media.audiofx.Visualizer
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

/**
 * StreamID別の音量監視実装
 * 実際のトラック別音量が取得できない環境でのシミュレーション
 */
class StreamVolumeMonitor(
    @Suppress("UNUSED_PARAMETER") private val audioManager: AudioManager
) {

    data class VolumeLevel(
        val rmsVolume: Float,
        val peakVolume: Float,
        val rmsDb: Float,
        val peakDb: Float,
        val timestamp: Long = System.currentTimeMillis()
    )

    interface VolumeListener {
        fun onVolumeChanged(streamId: String, volumeLevel: VolumeLevel)
    }

    private val volumeListeners = mutableSetOf<VolumeListener>()
    private val streamVolumeData = ConcurrentHashMap<String, VolumeLevel>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val registeredStreams = mutableSetOf<String>()

    private var visualizer: Visualizer? = null
    private var isMonitoring = false

    companion object {
        private const val TAG = "StreamVolumeMonitor"
    }

    /**
     * ストリームを登録
     */
    fun registerStream(streamId: String) {
        registeredStreams.add(streamId)
        Log.d(TAG, "ストリームを登録: $streamId")
    }

    /**
     * ストリームの登録を解除
     */
    fun unregisterStream(streamId: String) {
        registeredStreams.remove(streamId)
        streamVolumeData.remove(streamId)
        Log.d(TAG, "ストリームの登録を解除: $streamId")
    }

    /**
     * 音量監視を開始
     */
    fun startMonitoring() {
        if (isMonitoring) return

        try {
            visualizer = Visualizer(0).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]

                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        waveform?.let { processWaveform(it, samplingRate) }
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        // FFTデータは使用しない
                    }
                }, Visualizer.getMaxCaptureRate() / 10, true, false)

                enabled = true
            }

            isMonitoring = true
            Log.d(TAG, "音量監視を開始しました")

        } catch (e: Exception) {
            Log.e(TAG, "音量監視の開始に失敗しました", e)
        }
    }

    /**
     * 音量監視を停止
     */
    fun stopMonitoring() {
        if (!isMonitoring) return

        try {
            visualizer?.apply {
                enabled = false
                release()
            }
            visualizer = null
            isMonitoring = false
            streamVolumeData.clear()
            Log.d(TAG, "音量監視を停止しました")
        } catch (e: Exception) {
            Log.e(TAG, "音量監視の停止中にエラーが発生しました", e)
        }
    }

    private fun processWaveform(waveform: ByteArray, @Suppress("UNUSED_PARAMETER") samplingRate: Int) {
        val baseRmsVolume = calculateRmsVolume(waveform)
        val basePeakVolume = calculatePeakVolume(waveform)

        // 登録されたストリームごとに個別の音量レベルを生成
        registeredStreams.forEach { streamId ->
            // ストリームごとに異なる音量レベルをシミュレート
            val variationFactor = getStreamVariationFactor(streamId)
            val rmsVolume = (baseRmsVolume * variationFactor).coerceIn(0f, 1f)
            val peakVolume = (basePeakVolume * variationFactor * 1.2f).coerceIn(0f, 1f)
            val rmsDb = volumeToDb(rmsVolume)
            val peakDb = volumeToDb(peakVolume)

            val volumeLevel = VolumeLevel(rmsVolume, peakVolume, rmsDb, peakDb)
            streamVolumeData[streamId] = volumeLevel

            // メインスレッドでリスナーに通知
            mainHandler.post {
                volumeListeners.forEach { listener ->
                    listener.onVolumeChanged(streamId, volumeLevel)
                }
            }
        }
    }

    private fun getStreamVariationFactor(streamId: String): Float {
        // streamIdをベースにした一定の変動を生成（実際の音量差をシミュレート）
        val hash = streamId.hashCode()
        val baseVariation = 0.3f + (hash % 100) / 100f * 0.7f // 0.3 - 1.0の範囲
        val timeVariation = (sin(System.currentTimeMillis() / 1000.0 + hash) * 0.3f + 1.0f).toFloat()
        return (baseVariation * timeVariation).coerceIn(0.1f, 1.0f)
    }

    private fun calculateRmsVolume(waveform: ByteArray): Float {
        if (waveform.isEmpty()) return 0f

        var sumOfSquares = 0.0
        for (sample in waveform) {
            val normalizedSample = (sample.toInt() - 128) / 128.0
            sumOfSquares += normalizedSample * normalizedSample
        }

        val rms = sqrt(sumOfSquares / waveform.size)
        return rms.toFloat().coerceIn(0f, 1f)
    }

    private fun calculatePeakVolume(waveform: ByteArray): Float {
        if (waveform.isEmpty()) return 0f

        var maxAmplitude = 0
        for (sample in waveform) {
            val amplitude = abs(sample.toInt() - 128)
            maxAmplitude = max(maxAmplitude, amplitude)
        }

        return (maxAmplitude / 128.0f).coerceIn(0f, 1f)
    }

    private fun volumeToDb(volume: Float): Float {
        return if (volume > 0f) {
            20 * log10(volume)
        } else {
            -60f // 最小値として-60dBを設定
        }
    }

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
     * 指定されたストリームの最新の音量レベルを取得
     */
    fun getVolumeLevel(streamId: String): VolumeLevel? {
        return streamVolumeData[streamId]
    }

    /**
     * 全ストリームの音量データを取得
     */
    fun getAllVolumeLevels(): Map<String, VolumeLevel> {
        return streamVolumeData.toMap()
    }

    /**
     * 登録されたストリーム一覧を取得
     */
    fun getRegisteredStreams(): Set<String> {
        return registeredStreams.toSet()
    }
}
