package jp.shiguredo.sora.sample.ui.util

/**
 * 映像の表示サイズ (px)。rotation を反映した幅と高さを持つ。
 */
data class VideoSize(
    val width: Int,
    val height: Int,
) {
    companion object {
        // rotation を反映した表示サイズを返す。rotation は 0 / 90 / 180 / 270 を想定する
        fun fromRotation(
            videoWidth: Int,
            videoHeight: Int,
            rotation: Int,
        ): VideoSize =
            if (rotation % 180 != 0) {
                VideoSize(videoHeight, videoWidth)
            } else {
                VideoSize(videoWidth, videoHeight)
            }
    }
}

/**
 * レイアウト上の矩形 (px)。
 */
data class LayoutRect(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

/**
 * タイル (格子の 1 マス) 内に映像を切り取らずに収める矩形を計算する。
 *
 * 映像のアスペクト比を保ち、余った領域が上下または左右に均等に空くように中央へ配置する。
 */
object VideoRectCalculator {
    // セル内に収まる映像の矩形を返す。left と top はセル原点からの相対座標になる
    fun calculate(
        cellWidth: Int,
        cellHeight: Int,
        videoWidth: Int,
        videoHeight: Int,
    ): LayoutRect {
        // 不正な値が渡された場合はタイル全体を返す
        if (cellWidth <= 0 || cellHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) {
            return LayoutRect(
                left = 0,
                top = 0,
                width = cellWidth.coerceAtLeast(0),
                height = cellHeight.coerceAtLeast(0),
            )
        }

        return if (videoWidth.toLong() * cellHeight > cellWidth.toLong() * videoHeight) {
            // 映像のほうが横長なので、幅をタイルに合わせて高さを求める
            val height = scale(cellWidth, videoHeight, videoWidth).coerceIn(1, cellHeight)
            LayoutRect(
                left = 0,
                top = (cellHeight - height) / 2,
                width = cellWidth,
                height = height,
            )
        } else {
            // 映像のほうが縦長なので、高さをタイルに合わせて幅を求める
            val width = scale(cellHeight, videoWidth, videoHeight).coerceIn(1, cellWidth)
            LayoutRect(
                left = (cellWidth - width) / 2,
                top = 0,
                width = width,
                height = cellHeight,
            )
        }
    }

    // セルのレイアウト座標を加えた映像の矩形を返す
    fun calculateInCell(
        cell: LayoutRect,
        video: VideoSize,
    ): LayoutRect {
        val rect = calculate(cell.width, cell.height, video.width, video.height)
        return LayoutRect(
            left = cell.left + rect.left,
            top = cell.top + rect.top,
            width = rect.width,
            height = rect.height,
        )
    }

    // base * numerator / denominator を四捨五入して返す
    private fun scale(
        base: Int,
        numerator: Int,
        denominator: Int,
    ): Int = ((base.toLong() * numerator + denominator / 2) / denominator).toInt()
}
