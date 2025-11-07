package jp.shiguredo.sora.sample.ui.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private var softMuteJob: Job? = null
    private var hardMuteJob: Job? = null
    private val softMuteLock = Mutex()
    private val hardMuteLock = Mutex()

    /**
     * ソフトミュートの切り替え
     */
    fun toggleSoftMuted() {
        scope.launch {
            softMuteLock.withLock {
                if (softMuteJob?.isActive == true) {
                    log?.invoke("toggleSoftMuted ignored: job running")
                    return@withLock
                }

                softMuteJob =
                    scope.launch {
                        try {
                            val newState = !isSoftMuted
                            log?.invoke("toggleSoftMuted: $isSoftMuted -> $newState")
                            setSoftMute(newState)
                            isSoftMuted = newState
                            if (isSoftMuted) {
                                showSoftMuteOn()
                            } else {
                                showSoftMuteOff()
                            }
                        } finally {
                            val currentJob = this
                            softMuteLock.withLock {
                                if (softMuteJob === currentJob) {
                                    softMuteJob = null
                                }
                            }
                        }
                    }
            }
        }
    }

    /**
     * ハードミュートの切り替え
     */
    fun toggleHardMuted() {
        scope.launch {
            hardMuteLock.withLock {
                if (hardMuteJob?.isActive == true) {
                    log?.invoke("toggleHardMuted ignored: job running")
                    return@withLock
                }

                hardMuteJob =
                    scope.launch {
                        try {
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
                        } finally {
                            val currentJob = this
                            hardMuteLock.withLock {
                                if (hardMuteJob === currentJob) {
                                    hardMuteJob = null
                                }
                            }
                        }
                    }
            }
        }
    }
}
