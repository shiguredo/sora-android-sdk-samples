package jp.shiguredo.sora.sample.ui.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * マイクのソフト／ハードミュート制御をまとめたユーティリティ。
 * UI 側はボタン表示やログ出力をコールバックで渡す。
 */
class MicMuteController(
    private val scope: CoroutineScope,
    private val setSoftMute: (Boolean) -> Unit,
    private val setHardMute: suspend (Boolean) -> Boolean,
    private val showMicOn: () -> Unit,
    private val showMicSoft: () -> Unit,
    private val showMicHard: () -> Unit,
    private val logMessage: ((String) -> Unit)? = null,
    private val logState: ((String) -> Unit)? = null,
) {
    private enum class MicState { ON, SOFT_MUTED, HARD_MUTED }

    private var state: MicState = MicState.ON
    private var toggleJob: Job? = null

    fun toggleMuted() {
        if (toggleJob?.isActive == true) {
            logMessage?.invoke("toggleMuted ignored: job running")
            return
        }

        toggleJob =
            scope.launch {
                try {
                    logState?.invoke("micState: ${state.name}")
                    when (state) {
                        MicState.ON -> {
                            setSoftMute(true)
                            state = MicState.SOFT_MUTED
                            showMicSoft()
                        }
                        MicState.SOFT_MUTED -> {
                            setSoftMute(false)
                            val success = setHardMute(true)
                            if (success) {
                                state = MicState.HARD_MUTED
                                showMicHard()
                            } else {
                                setSoftMute(true)
                                state = MicState.SOFT_MUTED
                                showMicSoft()
                            }
                        }
                        MicState.HARD_MUTED -> {
                            val success = setHardMute(false)
                            if (success) {
                                setSoftMute(false)
                                state = MicState.ON
                                showMicOn()
                            } else {
                                state = MicState.HARD_MUTED
                                showMicHard()
                            }
                        }
                    }
                } finally {
                    logState?.invoke("micState: ${state.name}")
                    toggleJob = null
                }
            }
    }
}
