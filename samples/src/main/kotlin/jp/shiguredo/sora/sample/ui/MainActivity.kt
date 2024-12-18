package jp.shiguredo.sora.sample.ui

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.databinding.ActivityMainBinding
import jp.shiguredo.sora.sample.screencast.SoraScreencastService
import jp.shiguredo.sora.sdk.util.SoraLogger
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.OnShowRationale
import permissions.dispatcher.PermissionRequest
import permissions.dispatcher.RuntimePermissions

@RuntimePermissions
class MainActivity : AppCompatActivity() {

    companion object {
        val TAG = MainActivity::class.simpleName
    }

    private lateinit var binding: ActivityMainBinding

    // スクリーンキャストが開始された通知を受け取る BroadcastReceiver
    // スクリーンキャストを 1つのアプリ で開始した場合はこのアプリだと画面内に動きがないので映像が飛ばない
    // 強制的に映像を飛ばすため SoraScreencastService より開始したことを示す Intent を送って invalidate を実行する
    private val invalidateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // スクリーンキャストが実際に行われるまではタイムラグが発生しているので、
            // あまりよくはないが 1000ms 後に invalidate を実行する
            Handler(Looper.getMainLooper()).postDelayed({
                // この関数で画面が再描画されスクリーンキャストに映る
                window.decorView.rootView.invalidate()
            }, 1000)
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {

        SoraLogger.enabled = true
        SoraLogger.libjingle_enabled = true

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = FeatureListAdapter(
            arrayListOf(
                Feature(
                    title = "ビデオチャット",
                    description = "ビデオチャットのデモです。複数人でのグループチャットも可能です。"
                ),
                Feature(
                    title = "ボイスチャット",
                    description = "ボイスチャットのデモです。複数人でのグループチャットも可能です。"
                ),
                Feature(
                    title = "サイマルキャスト",
                    description = "サイマルキャストのデモです。"
                ),
                Feature(
                    title = "スポットライト",
                    description = "スポットライトのデモです。スポットライト数を固定したチャットが可能です。"
                ),
                Feature(
                    title = "リアルタイムメッセージング",
                    description = "リアルタイムメッセージングのデモです"
                ),
                Feature(
                    title = "スクリーンキャスト",
                    description = "スクリーンキャストのデモです。"
                ),
                Feature(
                    title = "ビデオエフェクト",
                    description = "エフェクト付きのビデオチャットのデモです"
                ),
            )
        )

        adapter.setOnItemClickListener(object : FeatureListAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                Log.d(TAG, "clicked:$position")
                goToDemo(position)
            }
        })

        val llm = LinearLayoutManager(this)
        binding.featureList.setHasFixedSize(true)
        binding.featureList.layoutManager = llm
        binding.featureList.adapter = adapter

        // スクリーンキャストが開始された通知を受け取る BroadcastReceiver を登録
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                invalidateReceiver, IntentFilter("ACTION_INVALIDATE_VIEW"),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(invalidateReceiver, IntentFilter("ACTION_INVALIDATE_VIEW"))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // スクリーンキャストが開始された通知を受け取る BroadcastReceiver を解除
        unregisterReceiver(invalidateReceiver)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    internal fun goToDemo(position: Int) {
        when (position) {
            0 -> goToVideoRoomDemoWithPermissionCheck()
            1 -> goToVoiceRoomDemoWithPermissionCheck()
            2 -> goToSimulcastWithPermissionCheck()
            3 -> goToSpotlightWithPermissionCheck()
            4 -> goToMessaging()
            5 -> goToScreencastActivityWithPermissionCheck()
            6 -> goToEffectedVideoRoomDemoWithPermissionCheck()
            else -> {
                Log.w(TAG, "must not come here")
            }
        }
    }

    @TargetApi(21)
    @NeedsPermission(Manifest.permission.RECORD_AUDIO)
    fun goToScreencastActivity() {
        /*
         * 1つのアプリ でこのサンプルを指定してスクリーンキャストを実行した場合、
         * android:launchMode="singleInstance" で別タスクとした ScreencastSetupActivity は
         * スクリーンキャストできないのでスクリーンキャスト実行中は遷移しないようにする
         * 画面全体の場合や他のアプリを選択した場合は必要ないのに遷移しなくなるが許容する
         */
        if (SoraScreencastService.isRunning()) {
            Toast.makeText(this, "スクリーンキャストは実行中です", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, ScreencastSetupActivity::class.java)
        startActivity(intent)
    }

    @NeedsPermission(value = [Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO])
    fun goToVideoRoomDemo() {
        val intent = Intent(this, VideoChatRoomSetupActivity::class.java)
        startActivity(intent)
    }

    @NeedsPermission(value = [Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO])
    fun goToEffectedVideoRoomDemo() {
        val intent = Intent(this, EffectedVideoChatSetupActivity::class.java)
        startActivity(intent)
    }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO)
    fun goToVoiceRoomDemo() {
        val intent = Intent(this, VoiceChatRoomSetupActivity::class.java)
        startActivity(intent)
    }

    @NeedsPermission(value = [Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO])
    fun goToSpotlight() {
        val intent = Intent(this, SpotlightRoomSetupActivity::class.java)
        startActivity(intent)
    }

    @NeedsPermission(value = [Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO])
    fun goToSimulcast() {
        val intent = Intent(this, SimulcastSetupActivity::class.java)
        startActivity(intent)
    }

    fun goToMessaging() {
        val intent = Intent(this, MessagingActivity::class.java)
        startActivity(intent)
    }

    @OnShowRationale(value = [Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO])
    fun showRationaleForCameraAndAudio(request: PermissionRequest) {
        Log.d(TAG, "showRationaleForCameraAndAudio")
        showRationaleDialog(
            "ビデオチャットを利用するには、カメラとマイクの使用許可が必要です", request
        )
    }

    @OnShowRationale(Manifest.permission.RECORD_AUDIO)
    fun showRationaleForAudio(request: PermissionRequest) {
        showRationaleDialog(
            "ボイスチャット・スクリーンキャストを利用するには、マイクの使用許可が必要です", request
        )
    }

    @OnPermissionDenied(value = [Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO])
    fun onCameraAndAudioDenied() {
        Log.d(TAG, "onCameraAndAudioDenied")
        Snackbar.make(
            binding.rootLayout,
            "ビデオチャットを利用するには、カメラとマイクの使用を許可してください",
            Snackbar.LENGTH_LONG
        )
            .setAction("OK") { }
            .show()
    }

    @OnPermissionDenied(Manifest.permission.RECORD_AUDIO)
    fun onAudioDenied() {
        Snackbar.make(
            binding.rootLayout,
            "ボイスチャットを利用するには、マイクの使用を許可してください",
            Snackbar.LENGTH_LONG
        )
            .setAction("OK") { }
            .show()
    }

    private fun showRationaleDialog(message: String, request: PermissionRequest) {
        AlertDialog.Builder(this)
            .setPositiveButton(getString(R.string.permission_button_positive)) { _, _ -> request.proceed() }
            .setNegativeButton(getString(R.string.permission_button_negative)) { _, _ -> request.cancel() }
            .setCancelable(false)
            .setMessage(message)
            .show()
    }
}
