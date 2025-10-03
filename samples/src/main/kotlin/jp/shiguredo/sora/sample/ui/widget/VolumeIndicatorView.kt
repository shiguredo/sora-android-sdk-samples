package jp.shiguredo.sora.sample.ui.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import jp.shiguredo.sora.sample.audio.AlternativeVolumeMonitor
import kotlin.math.*

/**
 * 音量レベルを視覚的に表示するカスタムビュー
 * シンプルなゲージ表示で音量レベルを可視化
 */
class VolumeIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val gaugePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var volumeLevel: AlternativeVolumeMonitor.VolumeLevel? = null

    // 色の定義
    private val lowVolumeColor = Color.parseColor("#4CAF50")    // 緑
    private val mediumVolumeColor = Color.parseColor("#FF9800")  // オレンジ
    private val highVolumeColor = Color.parseColor("#F44336")   // 赤
    private val backgroundColor = Color.parseColor("#F5F5F5")   // 薄いグレー
    private val borderColor = Color.parseColor("#BDBDBD")       // ボーダーグレー

    init {
        gaugePaint.style = Paint.Style.FILL
        backgroundPaint.color = backgroundColor
        backgroundPaint.style = Paint.Style.FILL
        borderPaint.color = borderColor
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 2f
    }

    /**
     * 音量レベルを更新
     */
    fun updateVolumeLevel(trackId: String, volumeLevel: AlternativeVolumeMonitor.VolumeLevel) {
        this.volumeLevel = volumeLevel
        invalidate()
    }

    /**
     * 音量レベルをクリア
     */
    fun clearVolumeLevel() {
        this.volumeLevel = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val cornerRadius = height * 0.2f

        // 背景ゲージを描画（グレー）
        canvas.drawRoundRect(0f, 0f, width, height, cornerRadius, cornerRadius, backgroundPaint)
        canvas.drawRoundRect(0f, 0f, width, height, cornerRadius, cornerRadius, borderPaint)

        val level = volumeLevel
        if (level != null) {
            drawVolumeGauge(canvas, width, height, cornerRadius, level)
        }
    }

    private fun drawVolumeGauge(canvas: Canvas, width: Float, height: Float, cornerRadius: Float, level: AlternativeVolumeMonitor.VolumeLevel) {
        // RTP Audio Level (0-127) から音量比率を計算
        // 0が最大音量、127が無音なので反転が必要
        val rtpLevel = level.rtpLevel
        val volumeRatio = ((127 - rtpLevel) / 127f).coerceIn(0f, 1f)

        // 音量が非常に小さい場合（95%以上が無音に近い場合）は表示しない
        if (volumeRatio < 0.05f) {
            return
        }

        // ゲージの幅を計算（音量が大きいほど右に伸びる）
        val gaugeWidth = width * volumeRatio

        // 音量レベルに応じて色を決定
        gaugePaint.color = getVolumeColor(volumeRatio)

        // 音量ゲージを描画
        canvas.drawRoundRect(0f, 0f, gaugeWidth, height, cornerRadius, cornerRadius, gaugePaint)
    }

    private fun getVolumeColor(ratio: Float): Int {
        return when {
            ratio < 0.3f -> lowVolumeColor      // 低音量: 緑
            ratio < 0.7f -> mediumVolumeColor   // 中音量: オレンジ
            else -> highVolumeColor             // 高音量: 赤
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 200
        val desiredHeight = 20

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(desiredWidth, widthSize)
            else -> desiredWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }
}
