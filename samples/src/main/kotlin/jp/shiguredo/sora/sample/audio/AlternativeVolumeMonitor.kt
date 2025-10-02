package jp.shiguredo.sora.sample.audio

import android.media.AudioManager
import android.media.audiofx.Visualizer
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

/**
 * AudioSinkが利用できない環境での代替音量監視実装
 * Visualizerを使用してシステム全体の音量を監視
 */
class AlternativeVolumeMonitor(
    private val audioManager: AudioManager
) {

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

    private var visualizer: Visualizer? = null
    private var isMonitoring = false

    companion object {
        private const val TAG = "AlternativeVolumeMonitor"
        private const val UPDATE_INTERVAL_MS = 50L
    }

    /**
     * 音量監視を開始
     */
    fun startMonitoring() {
        if (isMonitoring) return

        try {
            // 一般的なトラックIDとして固定値を使用
            val trackId = "system_audio"

            visualizer = Visualizer(0).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]

                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        waveform?.let { processWaveform(trackId, it, samplingRate) }
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        // FFTデータは使用しない
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, false)

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
            trackVolumeData.clear()
            Log.d(TAG, "音量監視を停止しました")
        } catch (e: Exception) {
            Log.e(TAG, "音量監視の停止中にエラーが発生しました", e)
        }
    }

    private fun processWaveform(trackId: String, waveform: ByteArray, samplingRate: Int) {
        val rmsVolume = calculateRmsVolume(waveform)
        val peakVolume = calculatePeakVolume(waveform)
        val rmsDb = volumeToDb(rmsVolume)
        val peakDb = volumeToDb(peakVolume)

        val volumeLevel = VolumeLevel(rmsVolume, peakVolume, rmsDb, peakDb)
        trackVolumeData[trackId] = volumeLevel

        // メインスレッドでリスナーに通知
        mainHandler.post {
            volumeListeners.forEach { listener ->
                listener.onVolumeChanged(trackId, volumeLevel)
            }
        }
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
}
