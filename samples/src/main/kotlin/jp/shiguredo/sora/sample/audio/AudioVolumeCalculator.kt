package jp.shiguredo.sora.sample.audio

import kotlin.math.*

/**
 * 音声データから音量レベルを計算するユーティリティクラス
 */
object AudioVolumeCalculator {

    /**
     * PCM16LEデータからRMS音量を計算します
     * @param audioData PCM16LEバイト配列
     * @param numberOfChannels チャンネル数
     * @return RMS音量（0.0 - 1.0）
     */
    fun calculateRmsVolume(audioData: ByteArray, numberOfChannels: Int): Float {
        if (audioData.isEmpty()) return 0f

        var sumOfSquares = 0.0
        val sampleCount = audioData.size / 2 // 16bitなので2バイトで1サンプル

        for (i in 0 until sampleCount step numberOfChannels) {
            val byteIndex = i * 2
            if (byteIndex + 1 < audioData.size) {
                // リトルエンディアンで16bitサンプルを読み取り
                val sample = (audioData[byteIndex].toInt() and 0xFF) or
                           ((audioData[byteIndex + 1].toInt() shl 8))
                // 符号拡張
                val signedSample = if (sample > 32767) sample - 65536 else sample
                // 正規化（-1.0 to 1.0）
                val normalizedSample = signedSample / 32768.0
                sumOfSquares += normalizedSample * normalizedSample
            }
        }

        val rms = sqrt(sumOfSquares / (sampleCount / numberOfChannels))
        return rms.toFloat().coerceIn(0f, 1f)
    }

    /**
     * PCM16LEデータからピーク音量を計算します
     * @param audioData PCM16LEバイト配列
     * @param numberOfChannels チャンネル数
     * @return ピーク音量（0.0 - 1.0）
     */
    fun calculatePeakVolume(audioData: ByteArray, numberOfChannels: Int): Float {
        if (audioData.isEmpty()) return 0f

        var maxAmplitude = 0
        val sampleCount = audioData.size / 2

        for (i in 0 until sampleCount step numberOfChannels) {
            val byteIndex = i * 2
            if (byteIndex + 1 < audioData.size) {
                val sample = (audioData[byteIndex].toInt() and 0xFF) or
                           ((audioData[byteIndex + 1].toInt() shl 8))
                val signedSample = if (sample > 32767) sample - 65536 else sample
                maxAmplitude = max(maxAmplitude, abs(signedSample))
            }
        }

        return (maxAmplitude / 32768.0f).coerceIn(0f, 1f)
    }

    /**
     * 音量レベルをdB値に変換します
     * @param volume 音量レベル（0.0 - 1.0）
     * @return dB値
     */
    fun volumeToDb(volume: Float): Float {
        return if (volume > 0f) {
            20 * log10(volume)
        } else {
            -60f // 最小値として-60dBを設定
        }
    }
}
