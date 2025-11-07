package jp.shiguredo.sora.sample.ui.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * マイクのソフト／ハードミュート制御をまとめたユーティリティ。
 * ソフトミュートとハードミュートを独立して制御する。
 */
class MicMuteController(
    private val scope: CoroutineScope,
    private val setSoftMute: (Boolean) -> Unit,
    private val setHardMute: suspend (Boolean) -> Boolean,
    private val showSoftMuteOn: () -> Unit,
    private val showSoftMuteOff: () -> Unit,
    private val showHardMuteOn: () -> Unit,
    private val showHardMuteOff: () -> Unit,
    private val log: ((String) -> Unit)? = null,
) {
    private var isSoftMuted: Boolean = false
    private var isHardMuted: Boolean = false

    /**
     * ソフトミュートの切り替え
     */
    fun toggleSoftMuted() {
        val newState = !isSoftMuted
        log?.invoke("toggleSoftMuted: $isSoftMuted -> $newState")
        setSoftMute(newState)
        isSoftMuted = newState
        if (isSoftMuted) {
            showSoftMuteOn()
        } else {
            showSoftMuteOff()
        }
    }

    /**
     * ハードミュートの切り替え
     */
    fun toggleHardMuted() {
        scope.launch {
            val newState = !isHardMuted
            log?.invoke("toggleHardMuted: $isHardMuted -> $newState")
            val success = setHardMute(newState)
            if (success) {
                isHardMuted = newState
                if (isHardMuted) {
                    showHardMuteOn()
                } else {
                    showHardMuteOff()
                }
            } else {
                log?.invoke("toggleHardMuted: setHardMute failed")
                // 失敗時は現在の状態を再表示
                if (isHardMuted) {
                    showHardMuteOn()
                } else {
                    showHardMuteOff()
                }
            }
        }
    }
}
