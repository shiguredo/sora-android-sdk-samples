package jp.shiguredo.sora.sample.ui.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private val log: ((String) -> Unit)? = null,
) {
    private enum class MicState { ON, SOFT_MUTED, HARD_MUTED }

    private var state: MicState = MicState.ON
    private var toggleJob: Job? = null
    private val jobLock = Mutex()

    // マイクミュート制御で競合が起きないよう Mutex を導入し、toggleMuted のジョブ生成と終了処理を排他的に行っている
    //  -> ボタン連打等でUI(ボタン)とミュート状態の整合が崩れるようなケースを防ぐ
    // 実行中ジョブの存在をロック下で判定し、Job を生成するタイミングを一意に確保
    // 終了時も同じロックで toggleJob をクリアし、重複実行やレースを防止する
    fun toggleMuted() {
        scope.launch {
            jobLock.withLock {
                if (toggleJob?.isActive == true) {
                    log?.invoke("toggleMuted ignored: job running")
                    return@withLock
                }

                toggleJob =
                    scope.launch {
                        try {
                            log?.invoke("micState: ${state.name}")
                            when (state) {
                                MicState.ON -> {
                                    // 音声ON -> ソフトミュート
                                    setSoftMute(true)
                                    state = MicState.SOFT_MUTED
                                    showMicSoft()
                                }
                                MicState.SOFT_MUTED -> {
                                    // ソフトミュート -> ハードミュート
                                    val success = setHardMute(true)
                                    if (success) {
                                        setSoftMute(false)
                                        state = MicState.HARD_MUTED
                                        showMicHard()
                                    } else {
                                        state = MicState.SOFT_MUTED
                                        showMicSoft()
                                    }
                                }
                                MicState.HARD_MUTED -> {
                                    // ハードミュート -> 音声ON
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
                            log?.invoke("micState: ${state.name}")
                            // toggleJob を確実に解放する後片付け
                            val currentJob = this
                            jobLock.withLock {
                                if (toggleJob === currentJob) {
                                    toggleJob = null
                                }
                            }
                        }
                    }
            }
        }
    }
}
