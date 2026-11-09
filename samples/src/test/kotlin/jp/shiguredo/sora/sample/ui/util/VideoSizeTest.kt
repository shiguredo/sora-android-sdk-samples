package jp.shiguredo.sora.sample.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoSizeTest {
    @Test
    fun rotation0KeepsSize() {
        assertEquals(VideoSize(width = 1920, height = 1080), VideoSize.fromRotation(1920, 1080, 0))
    }

    @Test
    fun rotation90SwapsSize() {
        assertEquals(VideoSize(width = 1080, height = 1920), VideoSize.fromRotation(1920, 1080, 90))
    }

    @Test
    fun rotation180KeepsSize() {
        assertEquals(VideoSize(width = 1920, height = 1080), VideoSize.fromRotation(1920, 1080, 180))
    }

    @Test
    fun rotation270SwapsSize() {
        assertEquals(VideoSize(width = 1080, height = 1920), VideoSize.fromRotation(1920, 1080, 270))
    }
}
