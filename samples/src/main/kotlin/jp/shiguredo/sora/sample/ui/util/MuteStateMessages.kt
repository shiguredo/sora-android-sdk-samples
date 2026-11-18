package jp.shiguredo.sora.sample.ui.util

/**
 * カメラ・マイクのミュート状態を利用者に伝える文言。
 * サンプル間で文言がずれないようにここへ集約する。
 */
internal object MuteStateMessages {
    const val MIC_ON = "マイク: ON"
    const val MIC_SOFT_MUTED = "マイク: ソフトミュート"
    const val MIC_HARD_MUTED = "マイク: ハードミュート"

    fun camera(
        hardMuted: Boolean,
        softMuted: Boolean,
    ): String =
        when {
            hardMuted -> "カメラ: ハードミュート"
            softMuted -> "カメラ: ソフトミュート"
            else -> "カメラ: ON"
        }
}
