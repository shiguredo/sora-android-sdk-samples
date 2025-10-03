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
            // 音量バーを描画 (0.0 - 10.0 スケール)
            drawVolumeBar(canvas, width, height, level)

            // テキスト情報を描画
            drawVolumeText(canvas, width, height, level)
        } else {
            // 音量データがない場合
            drawNoDataText(canvas, width, height)
        }
    }

    private fun drawVolumeBar(canvas: Canvas, width: Float, height: Float, level: AlternativeVolumeMonitor.VolumeLevel) {
        val barHeight = height * 0.30f
        val barY = height * 0.10f
        val barWidth = width * 0.80f
        val barX = width * 0.10f

        // ピークインジケータ: peakVolume (0-1) を webrtc スケールへ換算し位置を描画
        val peakRatio = level.peakVolume.coerceIn(0f, 1f)
        val peakX = barX + (barWidth * peakRatio)
        paint.color = Color.WHITE
        canvas.drawRect(peakX - 2f, barY, peakX + 2f, barY + barHeight, paint)

        // 目盛 (0, 5, 10) を簡易表示
        textPaint.textSize = 12f
        textPaint.color = textColor
        listOf(0f, 5f, 10f).forEach { tick ->
            val tx = barX + barWidth * (tick / 10f)
            canvas.drawText(tick.toInt().toString(), tx, barY + barHeight + 16f, textPaint)
        }
    }

    private fun drawVolumeText(canvas: Canvas, width: Float, height: Float, level: AlternativeVolumeMonitor.VolumeLevel) {
        val centerX = width / 2f

        // トラックID (短縮)
        textPaint.textSize = 18f
        textPaint.color = textColor
        canvas.drawText("Track: ${trackId.take(8)}", centerX, height * 0.60f, textPaint)

        // dB 情報 (RMS / Peak)
        textPaint.textSize = 14f
        val dbText = "RMS ${String.format("%.1f", level.rmsDb)} dB  |  Peak ${String.format("%.1f", level.peakDb)} dB"
        canvas.drawText(dbText, centerX, height * 0.93f, textPaint)
    }

    private fun drawNoDataText(canvas: Canvas, width: Float, height: Float) {
        textPaint.textSize = 18f
        textPaint.color = Color.GRAY
        canvas.drawText("No Audio Data", width / 2f, height / 2f, textPaint)
        textPaint.color = textColor
    }

    private fun getVolumeColor(ratio: Float): Int {
        return when {
            ratio < 0.3f -> lowVolumeColor
            ratio < 0.7f -> mediumVolumeColor
            else -> highVolumeColor
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 220
        val desiredHeight = 100

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
