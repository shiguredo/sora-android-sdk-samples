package jp.shiguredo.sora.sample.ui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import jp.shiguredo.sora.sample.databinding.ActivityScreencastGameBinding
import jp.shiguredo.sora.sample.screencast.SoraScreencastService
import jp.shiguredo.sora.sdk.util.SoraLogger

/**
 * スクリーンキャストで配信する箱積みゲームの画面です。
 *
 * 配信開始後に [SoraScreencastService] から起動されます。
 * 配信が停止すると [SoraScreencastService.ACTION_SCREENCAST_STOPPED] を受信して終了します。
 */
class ScreencastGameActivity : AppCompatActivity() {
    companion object {
        private val TAG = ScreencastGameActivity::class.simpleName
    }

    private lateinit var binding: ActivityScreencastGameBinding
    private var isReceiverRegistered = false

    private val screencastStoppedReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                SoraLogger.d(TAG, "stop broadcast received")
                if (!isFinishing) {
                    finish()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 配信していない状態でこの画面だけが復元された場合は、サンプル一覧に戻る
        if (!SoraScreencastService.isRunning()) {
            SoraLogger.w(TAG, "screencast is not running")
            finish()
            return
        }

        binding = ActivityScreencastGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindow()
        registerScreencastStoppedReceiver()
        SoraLogger.d(TAG, "onCreate")
    }

    override fun onResume() {
        super.onResume()
        binding.root.startRendering()
    }

    override fun onPause() {
        binding.root.stopRendering()
        super.onPause()
    }

    override fun onDestroy() {
        if (isReceiverRegistered) {
            unregisterReceiver(screencastStoppedReceiver)
            isReceiverRegistered = false
        }
        super.onDestroy()
    }

    private fun setupWindow() {
        // ゲーム画面が前面にある間は画面を消灯させない
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerScreencastStoppedReceiver() {
        val filter = IntentFilter(SoraScreencastService.ACTION_SCREENCAST_STOPPED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screencastStoppedReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screencastStoppedReceiver, filter)
        }
        isReceiverRegistered = true
    }
}
