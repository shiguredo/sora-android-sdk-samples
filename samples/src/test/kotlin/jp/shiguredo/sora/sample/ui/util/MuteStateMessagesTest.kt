package jp.shiguredo.sora.sample.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MuteStateMessagesTest {
    @Test
    fun hardMutedWinsOverSoftMuted() {
        assertEquals("カメラ: ハードミュート", MuteStateMessages.camera(hardMuted = true, softMuted = true))
        assertEquals("カメラ: ハードミュート", MuteStateMessages.camera(hardMuted = true, softMuted = false))
    }

    @Test
    fun softMutedReportsSoftMute() {
        assertEquals("カメラ: ソフトミュート", MuteStateMessages.camera(hardMuted = false, softMuted = true))
    }

    @Test
    fun notMutedReportsOn() {
        assertEquals("カメラ: ON", MuteStateMessages.camera(hardMuted = false, softMuted = false))
    }

    @Test
    fun micMessagesMatchMuteState() {
        assertEquals("マイク: ON", MuteStateMessages.MIC_ON)
        assertEquals("マイク: ソフトミュート", MuteStateMessages.MIC_SOFT_MUTED)
        assertEquals("マイク: ハードミュート", MuteStateMessages.MIC_HARD_MUTED)
    }
}
