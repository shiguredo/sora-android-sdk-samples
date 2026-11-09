package jp.shiguredo.sora.sample.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoRectCalculatorTest {
    @Test
    fun landscapeVideoInPortraitCell() {
        // 1080 x 1955 のタイルに 1920 x 1080 の映像を収めると、上下に余白ができる
        val rect =
            VideoRectCalculator.calculate(
                cellWidth = 1080,
                cellHeight = 1955,
                videoWidth = 1920,
                videoHeight = 1080,
            )

        assertEquals(LayoutRect(left = 0, top = 673, width = 1080, height = 608), rect)
    }

    @Test
    fun portraitVideoInLandscapeCell() {
        // 1000 x 500 のタイルに 1080 x 1920 の映像を収めると、左右に余白ができる
        val rect =
            VideoRectCalculator.calculate(
                cellWidth = 1000,
                cellHeight = 500,
                videoWidth = 1080,
                videoHeight = 1920,
            )

        assertEquals(LayoutRect(left = 359, top = 0, width = 281, height = 500), rect)
    }

    @Test
    fun sameAspectRatioFillsCell() {
        // タイルと映像のアスペクト比が同じ場合は余白ができない
        val rect =
            VideoRectCalculator.calculate(
                cellWidth = 640,
                cellHeight = 360,
                videoWidth = 1920,
                videoHeight = 1080,
            )

        assertEquals(LayoutRect(left = 0, top = 0, width = 640, height = 360), rect)
    }

    @Test
    fun invalidVideoSizeReturnsCell() {
        // 不正な解像度が渡された場合はタイル全体を返す
        val rect =
            VideoRectCalculator.calculate(
                cellWidth = 100,
                cellHeight = 50,
                videoWidth = 0,
                videoHeight = 0,
            )

        assertEquals(LayoutRect(left = 0, top = 0, width = 100, height = 50), rect)
    }

    @Test
    fun roundingUsesNearestPixel() {
        // 101 x 100 のタイルに 1920 x 1080 の映像を収めると、高さ 56.8 は四捨五入で 57 になる
        val rect =
            VideoRectCalculator.calculate(
                cellWidth = 101,
                cellHeight = 100,
                videoWidth = 1920,
                videoHeight = 1080,
            )

        assertEquals(LayoutRect(left = 0, top = 21, width = 101, height = 57), rect)
    }

    @Test
    fun calculateInCellOffsetsByCellPosition() {
        // セルのレイアウト座標が映像矩形に加算される
        val rect =
            VideoRectCalculator.calculateInCell(
                cell = LayoutRect(left = 100, top = 200, width = 400, height = 300),
                video = VideoSize(width = 1920, height = 1080),
            )

        assertEquals(LayoutRect(left = 100, top = 237, width = 400, height = 225), rect)
    }
}
