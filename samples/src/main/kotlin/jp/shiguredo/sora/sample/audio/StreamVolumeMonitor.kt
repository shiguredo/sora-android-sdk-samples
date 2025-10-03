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
        val rtpLevel: Int, // RTP準拠の0-127レベル
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
        // RTP Audio Level準拠の定数
        private const val MIN_AUDIO_LEVEL = -127.0
        private const val MAX_AUDIO_LEVEL = 0.0
        private const val SILENCE_LEVEL = 127 // デジタル無音時のレベル
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
        // 登録されたストリームごとに個別の音量レベルを生成
        registeredStreams.forEach { streamId ->
            // ストリームごとに異なる音量レベルをシミュレート
            val variationFactor = getStreamVariationFactor(streamId)

            // RTP Audio Level準拠の計算を使用
            val rtpLevel = calculateRtpAudioLevel(waveform, variationFactor)

            // 既存の計算も維持（互換性のため）
            val baseRmsVolume = calculateRmsVolume(waveform)
            val basePeakVolume = calculatePeakVolume(waveform)
            val rmsVolume = (baseRmsVolume * variationFactor).coerceIn(0f, 1f)
            val peakVolume = (basePeakVolume * variationFactor * 1.2f).coerceIn(0f, 1f)
            val rmsDb = volumeToDb(rmsVolume)
            val peakDb = volumeToDb(peakVolume)

            val volumeLevel = VolumeLevel(rmsVolume, peakVolume, rmsDb, peakDb, rtpLevel)
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
     * RTP Audio Level (RFC 6465) 準拠の音量計算
     * dBovベースで0-127の範囲で表現
     */
    private fun calculateRtpAudioLevel(waveform: ByteArray, variationFactor: Float): Int {
        if (waveform.isEmpty()) return SILENCE_LEVEL

        // PCMサンプルを-128から127の範囲として扱う
        val samples = IntArray(waveform.size) { i ->
            ((waveform[i].toInt() - 128) * variationFactor).toInt()
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

    private fun volumeToRtpLevel(volume: Float): Int {
        // 既存の簡易計算（後方互換性のため残す）
        return (volume * 127).toInt().coerceIn(0, 127)
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
