package jp.shiguredo.sora.sample.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.util.Log
import org.webrtc.CapturerObserver
import org.webrtc.JavaI420Buffer
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoFrame
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

// 7 色横カラーバー + 経過秒 + チェッカーパターンを生成するダミー VideoCapturer 実装
// カメラを使わずに検証用の映像を Sora に送信するためのもの
internal class DummyVideoCapturer : VideoCapturer {
    companion object {
        private const val TAG = "DummyVideoCapturer"

        // Y プレーン輝度値 (16-235 が TV レンジ)
        private const val Y_BLACK = 16.toByte()
        private const val Y_WHITE = 235.toByte()

        // U/V プレーン無彩色 (128 = グレー)
        private const val UV_NEUTRAL = 128.toByte()
    }

    private data class YuvColor(
        val y: Int,
        val u: Int,
        val v: Int,
    )

    private val colorTable =
        listOf(
            YuvColor(235, 128, 128), // 白
            YuvColor(210, 16, 146), // 黄
            YuvColor(170, 166, 16), // シアン
            YuvColor(145, 54, 34), // 緑
            YuvColor(106, 202, 222), // マゼンタ
            YuvColor(81, 90, 240), // 赤
            YuvColor(41, 240, 110), // 青
        )

    private val colorYBytes = colorTable.map { it.y.toByte() }.toByteArray()
    private val colorUBytes = colorTable.map { it.u.toByte() }.toByteArray()
    private val colorVBytes = colorTable.map { it.v.toByte() }.toByteArray()

    private val isRunning = AtomicBoolean(false)

    private var handler: Handler? = null
    private var observer: CapturerObserver? = null
    private var width: Int = 0
    private var height: Int = 0
    private var fps: Int = 0
    private val frameIndex = AtomicLong(0)
    private var startTimeMs: Long = 0
    private var startTextBitmap: Bitmap? = null
    private var elapsedBitmap: Bitmap? = null
    private var lastFpsLogTimeMs: Long = 0
    private var fpsFrameCount: Int = 0
    private var cachedPaint: Paint? = null

    private val isDisposed = AtomicBoolean(false)

    private val generateFrameRunnable =
        object : Runnable {
            override fun run() {
                if (!isRunning.get() || isDisposed.get()) {
                    return
                }
                generateFrame()
                val delayMs = 1000L / fps.coerceAtLeast(1)
                handler?.postDelayed(this, delayMs)
            }
        }

    override fun initialize(
        surfaceTextureHelper: SurfaceTextureHelper?,
        applicationContext: Context?,
        capturerObserver: CapturerObserver?,
    ) {
        handler = surfaceTextureHelper?.handler
        observer = capturerObserver
        Log.d(TAG, "initialize: handler=${handler != null}")
    }

    override fun startCapture(
        width: Int,
        height: Int,
        fps: Int,
    ) {
        this.width = width
        this.height = height
        this.fps = fps
        frameIndex.set(0)
        startTimeMs = System.currentTimeMillis()
        startTextBitmap?.recycle()
        startTextBitmap = null
        elapsedBitmap?.recycle()
        elapsedBitmap = null
        lastFpsLogTimeMs = 0
        fpsFrameCount = 0
        isRunning.set(true)
        if (handler == null) {
            Log.w(TAG, "startCapture: handler is null, cannot start frame generation")
            observer?.onCapturerStarted(false)
            return
        }
        val delayMs = 1000L / fps.coerceAtLeast(1)
        Log.d(TAG, "startCapture: ${width}x$height@${fps}fps delayMs=$delayMs")
        handler?.postDelayed(generateFrameRunnable, delayMs)
        observer?.onCapturerStarted(true)
    }

    override fun stopCapture() {
        Log.d(TAG, "stopCapture")
        isRunning.set(false)
        handler?.removeCallbacks(generateFrameRunnable)
        observer?.onCapturerStopped()
    }

    override fun changeCaptureFormat(
        width: Int,
        height: Int,
        fps: Int,
    ) {
        this.width = width
        this.height = height
        this.fps = fps
    }

    override fun dispose() {
        if (!isDisposed.compareAndSet(false, true)) {
            Log.d(TAG, "dispose: 二重呼び出しのためスキップ")
            return
        }
        Log.d(TAG, "dispose: frameIndex=${frameIndex.get()}")
        isRunning.set(false)
        handler?.removeCallbacks(generateFrameRunnable)
        startTextBitmap?.recycle()
        startTextBitmap = null
        elapsedBitmap?.recycle()
        elapsedBitmap = null
        handler = null
        observer = null
    }

    override fun isScreencast(): Boolean = false

    private fun generateFrame() {
        val w = width
        val h = height
        if (w <= 0 || h <= 0) {
            Log.w(TAG, "generateFrame: 無効なサイズ w=$w h=$h")
            return
        }

        val buffer = JavaI420Buffer.allocate(w, h)
        val barWidth = w / colorTable.size + 1
        val shift = (frameIndex.get() * 4 % w).toInt()

        // 1 行分のカラーバーパターンを ByteArray に事前計算し、ByteBuffer.put(byte[]) で
        // 行単位の一括書き込み。スクロール時は wrap-around を 2 回の put で実現。
        // Direct ByteBuffer のため put(index, byte) のようなピクセル単位 JNI 呼び出しは回避する
        val yRow = ByteArray(w)
        for (x in 0 until w) {
            yRow[x] = colorYBytes[x / barWidth % colorTable.size]
        }
        val yBuffer = buffer.dataY
        val yStride = buffer.strideY
        for (y in 0 until h) {
            yBuffer.position(y * yStride)
            if (shift == 0) {
                yBuffer.put(yRow)
            } else {
                yBuffer.put(yRow, shift, w - shift)
                yBuffer.put(yRow, 0, shift)
            }
        }

        // U/V プレーン (4:2:0)
        val uvW = w / 2
        val uvH = h / 2
        val uRow = ByteArray(uvW)
        val vRow = ByteArray(uvW)
        for (x in 0 until uvW) {
            val colorIdx = (x * 2) / barWidth % colorTable.size
            uRow[x] = colorUBytes[colorIdx]
            vRow[x] = colorVBytes[colorIdx]
        }
        val uBuffer = buffer.dataU
        val vBuffer = buffer.dataV
        val uvStride = buffer.strideU
        val uvShift = shift / 2
        for (y in 0 until uvH) {
            val uvPos = y * uvStride
            if (uvShift == 0) {
                uBuffer.position(uvPos)
                uBuffer.put(uRow)
                vBuffer.position(uvPos)
                vBuffer.put(vRow)
            } else {
                uBuffer.position(uvPos)
                uBuffer.put(uRow, uvShift, uvW - uvShift)
                uBuffer.put(uRow, 0, uvShift)
                vBuffer.position(uvPos)
                vBuffer.put(vRow, uvShift, uvW - uvShift)
                vBuffer.put(vRow, 0, uvShift)
            }
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val startText = dateFormat.format(Date(startTimeMs))

        if (startTextBitmap == null) {
            startTextBitmap = createTextBitmap(startText, w, 0.035f, true)
        }
        drawTextFromBitmap(buffer, w, h, startTextBitmap!!, 5)

        val elapsed = System.currentTimeMillis() - startTimeMs
        val minutes = elapsed / 60_000
        val seconds = (elapsed % 60_000) / 1000
        val millis = elapsed % 1000
        val text =
            "${minutes.toString().padStart(4, '0')}:" +
                "${seconds.toString().padStart(2, '0')}." +
                "${millis.toString().padStart(3, '0')}"

        drawText(buffer, w, h, text, 50, 0.10f)
        drawCheckerboard(buffer, w, h)

        val timestampNs = System.nanoTime()
        val videoFrame = VideoFrame(buffer, 0, timestampNs)
        observer?.onFrameCaptured(videoFrame)
        videoFrame.release()

        frameIndex.incrementAndGet()
    }

    // テキストを Bitmap に描画し I420 バッファへ書き込む
    // 経過秒のように毎フレームテキストが変わる場合に使う
    private fun drawText(
        buffer: JavaI420Buffer,
        frameWidth: Int,
        frameHeight: Int,
        text: String,
        yPercent: Int,
        fontSizeFraction: Float,
    ) {
        val bitmap = createTextBitmap(text, frameWidth, fontSizeFraction, false)
        drawTextFromBitmap(buffer, frameWidth, frameHeight, bitmap, yPercent)
    }

    // 事前生成した Bitmap から I420 バッファへピクセルを転送する
    // 開始時刻のようにテキストが固定で Bitmap をキャッシュしている場合に使う
    private fun drawTextFromBitmap(
        buffer: JavaI420Buffer,
        frameWidth: Int,
        frameHeight: Int,
        textBitmap: Bitmap,
        yPercent: Int,
    ) {
        val bw = textBitmap.width
        val bh = textBitmap.height

        val startX = (frameWidth - bw) / 2
        val startY = (frameHeight * yPercent / 100) - bh / 2

        val pixels = IntArray(bw * bh)
        textBitmap.getPixels(pixels, 0, bw, 0, 0, bw, bh)

        val yBuffer = buffer.dataY
        val yStride = buffer.strideY
        val uBuffer = buffer.dataU
        val vBuffer = buffer.dataV
        val uStride = buffer.strideU
        val vStride = buffer.strideV

        // ARGB ピクセルを Y 値に変換し、行単位の ByteArray に構築して bulk put
        // U/V は一律 128 で埋めた ByteArray を行単位で一括書き込み
        val uvWidth = (bw + 1) / 2
        val uvRow = ByteArray(uvWidth) { UV_NEUTRAL }

        for (by in 0 until bh) {
            val fy = startY + by
            if (fy < 0 || fy >= frameHeight) continue
            val yRowOffset = fy * yStride

            val rowBytes = ByteArray(bw)
            for (bx in 0 until bw) {
                val pixel = pixels[by * bw + bx]
                val r = (pixel shr 16) and 0xFF
                rowBytes[bx] = (16 + r * (235 - 16) / 255).toByte()
            }
            yBuffer.position(yRowOffset + startX)
            yBuffer.put(rowBytes)

            if (fy % 2 == 0 && fy / 2 < frameHeight / 2) {
                val uvY = fy / 2
                val uvRowStart = uvY * uStride + startX / 2
                val len = minOf(uvWidth, frameWidth / 2 - startX / 2)
                if (len > 0) {
                    uBuffer.position(uvRowStart)
                    uBuffer.put(uvRow, 0, len)
                    vBuffer.position(uvRowStart)
                    vBuffer.put(uvRow, 0, len)
                }
            }
        }
    }

    private fun createTextBitmap(
        text: String,
        frameWidth: Int,
        fontSizeFraction: Float,
        antiAlias: Boolean,
    ): Bitmap {
        val paint =
            cachedPaint ?: Paint().apply {
                isAntiAlias = antiAlias
                color = Color.WHITE
                typeface = Typeface.MONOSPACE
                cachedPaint = this
            }
        // 経過秒テキスト用 Bitmap はサイズ一致時に再利用し、毎フレームの Bitmap.createBitmap を回避
        // 開始時刻テキストは不変のため初回生成後に startTextBitmap にキャッシュして描画をスキップ
        paint.isAntiAlias = antiAlias
        paint.textSize = (frameWidth * fontSizeFraction).toFloat().coerceAtLeast(12f)

        val metrics = paint.fontMetrics
        val textWidth = paint.measureText(text).toInt()
        val textHeight = (metrics.bottom - metrics.top).toInt()

        val reuse =
            if (antiAlias) {
                null
            } else {
                elapsedBitmap?.takeIf {
                    it.width == textWidth && it.height == textHeight
                }
            }
        val bitmap = reuse ?: Bitmap.createBitmap(textWidth, textHeight, Bitmap.Config.ARGB_8888)
        if (!antiAlias && reuse == null) {
            elapsedBitmap?.recycle()
            elapsedBitmap = bitmap
        }
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        canvas.drawText(text, 0f, -metrics.top, paint)
        return bitmap
    }

    // 白黒のチェッカーボードを描画する
    private fun drawCheckerboard(
        buffer: JavaI420Buffer,
        frameWidth: Int,
        frameHeight: Int,
    ) {
        val scroll = (frameIndex.get().toInt() * 2)
        val x0 = frameWidth / 2
        val y0 = frameHeight * 60 / 100
        val totalHeight = frameHeight - y0
        val bandHeight = maxOf(1, totalHeight / 4)
        val regionWidth = frameWidth - x0

        val blockSizes = intArrayOf(1, 2, 4, 8)

        // 各バンドの基準行パターンを ByteArray に事前計算し、行単位の bulk put で書き込む
        // スクロールと奇数行反転はバイト列の offset 指定で吸収し、ピクセル単位ループを回避する
        val bandPatterns =
            Array(4) { band ->
                val bs = blockSizes[band]
                ByteArray(regionWidth) { x ->
                    if ((x / bs) % 2 == 0) Y_WHITE else Y_BLACK
                }
            }

        val yBuffer = buffer.dataY
        val yStride = buffer.strideY
        val uBuffer = buffer.dataU
        val vBuffer = buffer.dataV
        val uStride = buffer.strideU
        val vStride = buffer.strideV

        val uvX0 = x0 / 2
        val uvRegionWidth = (regionWidth + 1) / 2

        for (y in y0 until frameHeight) {
            val band = minOf((y - y0) / bandHeight, 3)
            val bs = blockSizes[band]
            val by = (y - y0) / bs
            val pattern = bandPatterns[band]

            // 奇数行はパターン反転 = blockSize 分オフセット
            val adjustedScroll = (scroll + if (by % 2 != 0) bs else 0) % (bs * 2)
            val yRowOffset = y * yStride

            yBuffer.position(yRowOffset + x0)
            yBuffer.put(pattern, adjustedScroll, regionWidth - adjustedScroll)
            yBuffer.put(pattern, 0, adjustedScroll)

            if (y % 2 == 0 && y / 2 < frameHeight / 2) {
                val uvY = y / 2
                val uvRowOffset = uvY * uStride
                for (x in 0 until uvRegionWidth) {
                    uBuffer.put(uvRowOffset + uvX0 + x, UV_NEUTRAL)
                    vBuffer.put(uvRowOffset + uvX0 + x, UV_NEUTRAL)
                }
            }
        }
    }
}
