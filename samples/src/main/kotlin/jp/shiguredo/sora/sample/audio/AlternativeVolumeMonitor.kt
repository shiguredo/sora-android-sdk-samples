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
        val rtpLevel: Int, // RTP準拠の0-127レベル
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
        // RTP Audio Level準拠の定数
        private const val MIN_AUDIO_LEVEL = -127.0
        private const val MAX_AUDIO_LEVEL = 0.0
        private const val SILENCE_LEVEL = 127 // デジタル無音時のレベル
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

        // RTP Audio Level準拠の計算
        val rtpLevel = calculateRtpAudioLevel(waveform)

        val volumeLevel = VolumeLevel(rmsVolume, peakVolume, rmsDb, peakDb, rtpLevel)
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
            val amplitude = kotlin.math.abs(sample.toInt() - 128)
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
     * RTP Audio Level (RFC 6465) 準拠の音量計算
     * dBovベースで0-127の範囲で表現
     */
    private fun calculateRtpAudioLevel(waveform: ByteArray): Int {
        if (waveform.isEmpty()) return SILENCE_LEVEL

        // PCMサンプルを-128から127の範囲として扱う
        val samples = IntArray(waveform.size) { i ->
            waveform[i].toInt() - 128
        }

        return calculateAudioLevel(samples, 0, samples.size, 127)
    }

    /**
     * RFC 6465のJavaコードを基にしたKotlin実装
     * PCMサンプルからRTP Audio Levelを計算
     */
    private fun calculateAudioLevel(
        samples: IntArray,
        offset: Int,
        length: Int,
        overload: Int
    ): Int {
        // RMS（Root Mean Square）を計算
        var rms = 0.0
        var actualLength = 0

        for (i in offset until minOf(offset + length, samples.size)) {
            val sample = samples[i].toDouble()
            val normalizedSample = sample / overload
            rms += normalizedSample * normalizedSample
            actualLength++
        }

        rms = if (actualLength == 0) 0.0 else sqrt(rms / actualLength)

        // dBovでの音量レベルを計算
        val db = if (rms > 0) {
            val calculatedDb = 20 * log10(rms)
            // 最小・最大レベル内に制限
            when {
                calculatedDb < MIN_AUDIO_LEVEL -> MIN_AUDIO_LEVEL
                calculatedDb > MAX_AUDIO_LEVEL -> MAX_AUDIO_LEVEL
                else -> calculatedDb
            }
        } else {
            MIN_AUDIO_LEVEL
        }

        // RFC 6465準拠: RTP Audio Level = -dBov値
        // dBovは負の値なので、符号を反転させて0-127の範囲にする
        return kotlin.math.round(-db).toInt().coerceIn(0, 127)
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
