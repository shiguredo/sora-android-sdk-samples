package jp.shiguredo.sora.sample.ui.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import jp.shiguredo.sora.sample.audio.AlternativeVolumeMonitor
import kotlin.math.*

/**
 * 音量レベルを視覚的に表示するカスタムビュー
 */
class VolumeIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var volumeLevel: AlternativeVolumeMonitor.VolumeLevel? = null
    private var trackId: String = ""

    // 色の定義
    private val lowVolumeColor = Color.parseColor("#4CAF50")    // 緑
    private val mediumVolumeColor = Color.parseColor("#FF9800")  // オレンジ
    private val highVolumeColor = Color.parseColor("#F44336")   // 赤
    private val backgroundColor = Color.parseColor("#E0E0E0")   // グレー
    private val textColor = Color.parseColor("#212121")        // ダークグレー

    init {
        paint.style = Paint.Style.FILL
        textPaint.color = textColor
        textPaint.textSize = 24f
        textPaint.textAlign = Paint.Align.CENTER
        backgroundPaint.color = backgroundColor
        backgroundPaint.style = Paint.Style.FILL
    }

    /**
     * 音量レベルを更新
     */
    fun updateVolumeLevel(trackId: String, volumeLevel: AlternativeVolumeMonitor.VolumeLevel) {
        this.trackId = trackId
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

        // 背景を描画
        canvas.drawRoundRect(0f, 0f, width, height, 8f, 8f, backgroundPaint)

        val level = volumeLevel
        if (level != null) {
            // 音量バーを描画
            drawVolumeBar(canvas, width, height, level)

            // テキスト情報を描画
            drawVolumeText(canvas, width, height, level)
        } else {
            // 音量データがない場合
            drawNoDataText(canvas, width, height)
        }
    }

    private fun drawVolumeBar(canvas: Canvas, width: Float, height: Float, level: AlternativeVolumeMonitor.VolumeLevel) {
        val barHeight = height * 0.3f
        val barY = height * 0.1f
        val barWidth = width * 0.8f
        val barX = width * 0.1f

        // RMS音量バーを描画
        val rmsWidth = barWidth * level.rmsVolume
        val rmsColor = getVolumeColor(level.rmsVolume)
        paint.color = rmsColor
        canvas.drawRoundRect(barX, barY, barX + rmsWidth, barY + barHeight, 4f, 4f, paint)

        // ピーク音量インジケーターを描画
        val peakX = barX + (barWidth * level.peakVolume)
        paint.color = Color.WHITE
        canvas.drawRect(peakX - 2f, barY, peakX + 2f, barY + barHeight, paint)
    }

    private fun drawVolumeText(canvas: Canvas, width: Float, height: Float, level: AlternativeVolumeMonitor.VolumeLevel) {
        val centerX = width / 2f

        // トラックID
        textPaint.textSize = 20f
        canvas.drawText("Track: ${trackId.take(8)}", centerX, height * 0.6f, textPaint)

        // RMS音量（dB）
        textPaint.textSize = 16f
        val rmsDbText = "RMS: ${String.format("%.1f", level.rmsDb)} dB"
        canvas.drawText(rmsDbText, centerX, height * 0.75f, textPaint)

        // ピーク音量（dB）
        val peakDbText = "Peak: ${String.format("%.1f", level.peakDb)} dB"
        canvas.drawText(peakDbText, centerX, height * 0.9f, textPaint)
    }

    private fun drawNoDataText(canvas: Canvas, width: Float, height: Float) {
        textPaint.textSize = 18f
        textPaint.color = Color.GRAY
        canvas.drawText("No Audio Data", width / 2f, height / 2f, textPaint)
        textPaint.color = textColor
    }

    private fun getVolumeColor(volume: Float): Int {
        return when {
            volume < 0.3f -> lowVolumeColor
            volume < 0.7f -> mediumVolumeColor
            else -> highVolumeColor
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 200
        val desiredHeight = 80

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
