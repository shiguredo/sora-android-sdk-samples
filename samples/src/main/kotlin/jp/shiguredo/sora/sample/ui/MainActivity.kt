package jp.shiguredo.sora.sample.ui

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.databinding.ActivityMainBinding
import jp.shiguredo.sora.sample.screencast.SoraScreencastService
import jp.shiguredo.sora.sdk.util.SoraLogger

class MainActivity : AppCompatActivity() {
    companion object {
        val TAG = MainActivity::class.simpleName
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        SoraLogger.enabled = true
        SoraLogger.libwebrtcLogEnabled = true

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter =
            FeatureListAdapter(
                arrayListOf(
                    Feature(
                        title = "ビデオチャット",
                        description = "ビデオチャットのデモです。複数人でのグループチャットも可能です。",
                    ),
                    Feature(
                        title = "ボイスチャット",
                        description = "ボイスチャットのデモです。複数人でのグループチャットも可能です。",
                    ),
                    Feature(
                        title = "サイマルキャスト",
                        description = "サイマルキャストのデモです。",
                    ),
                    Feature(
                        title = "スポットライト",
                        description = "スポットライトのデモです。スポットライト数を固定したチャットが可能です。",
                    ),
                    Feature(
                        title = "RPC チャット",
                        description = "RPC 機能を動かすためのサンプルです。1:1 のビデオチャット。",
                    ),
                    Feature(
                        title = "リアルタイムメッセージング",
                        description = "リアルタイムメッセージングのデモです",
                    ),
                    Feature(
                        title = "スクリーンキャスト",
                        description = "画面をタップして箱を積み上げるゲームを配信するデモです。",
                    ),
                ),
            )

        adapter.setOnItemClickListener(
            object : FeatureListAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    Log.d(TAG, "clicked:$position")
                    goToDemo(position)
                }
            },
        )

        val llm = LinearLayoutManager(this)
        binding.featureList.setHasFixedSize(true)
        binding.featureList.layoutManager = llm
        binding.featureList.adapter = adapter
    }

    // Activity Result API launchers
    private val requestAudioPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) {
                pendingActionAfterPermission?.invoke()
            } else {
                onAudioDenied()
            }
            pendingActionAfterPermission = null
        }

    private val requestCameraAndAudioPermissions =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { results ->
            val cameraGranted = results[Manifest.permission.CAMERA] == true
            val audioGranted = results[Manifest.permission.RECORD_AUDIO] == true
            if (cameraGranted && audioGranted) {
                pendingActionAfterPermission?.invoke()
            } else {
                onCameraAndAudioDenied()
            }
            pendingActionAfterPermission = null
        }

    private var pendingActionAfterPermission: (() -> Unit)? = null

    internal fun goToDemo(position: Int) {
        when (position) {
            0 -> goToVideoRoomDemoWithPermissionCheck()
            1 -> goToVoiceRoomDemoWithPermissionCheck()
            2 -> goToSimulcastWithPermissionCheck()
            3 -> goToSpotlightWithPermissionCheck()
            4 -> goToRpcChatWithPermissionCheck()
            5 -> goToMessaging()
            6 -> goToScreencastActivityWithPermissionCheck()
            else -> {
                Log.w(TAG, "must not come here")
            }
        }
    }

    @TargetApi(21)
    private fun goToScreencastActivity() {
        /*
         * アプリ単位でこのサンプルを指定してスクリーンキャストを実行した場合、
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

    private fun goToVideoRoomDemo() {
        val intent = Intent(this, VideoChatRoomSetupActivity::class.java)
        startActivity(intent)
    }

    private fun goToVoiceRoomDemo() {
        val intent = Intent(this, VoiceChatRoomSetupActivity::class.java)
        startActivity(intent)
    }

    private fun goToSpotlight() {
        val intent = Intent(this, SpotlightRoomSetupActivity::class.java)
        startActivity(intent)
    }

    private fun goToSimulcast() {
        val intent = Intent(this, SimulcastSetupActivity::class.java)
        startActivity(intent)
    }

    private fun goToRpcChat() {
        val intent = Intent(this, RpcChatSetupActivity::class.java)
        startActivity(intent)
    }

    fun goToMessaging() {
        val intent = Intent(this, MessagingActivity::class.java)
        startActivity(intent)
    }

    private fun onCameraAndAudioDenied() {
        Log.d(TAG, "onCameraAndAudioDenied")
        Snackbar
            .make(
                binding.rootLayout,
                "ビデオチャットを利用するには、カメラとマイクの使用を許可してください",
                Snackbar.LENGTH_LONG,
            ).setAction("OK") { }
            .show()
    }

    private fun onAudioDenied() {
        Snackbar
            .make(
                binding.rootLayout,
                "ボイスチャットを利用するには、マイクの使用を許可してください",
                Snackbar.LENGTH_LONG,
            ).setAction("OK") { }
            .show()
    }

    private fun showRationaleDialog(
        message: String,
        onProceed: () -> Unit,
    ) {
        AlertDialog
            .Builder(this)
            .setPositiveButton(getString(R.string.permission_button_positive)) { _, _ -> onProceed() }
            .setNegativeButton(getString(R.string.permission_button_negative)) { _, _ -> }
            .setCancelable(false)
            .setMessage(message)
            .show()
    }

    // ===== Permission check entry points (replace *WithPermissionCheck generated methods) =====
    private fun goToVideoRoomDemoWithPermissionCheck() {
        val cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (cameraGranted && audioGranted) {
            goToVideoRoomDemo()
            return
        }
        pendingActionAfterPermission = { goToVideoRoomDemo() }
        val needRationale =
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ||
                shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
        if (needRationale) {
            showRationaleDialog("ビデオチャットを利用するには、カメラとマイクの使用許可が必要です") {
                requestCameraAndAudioPermissions.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
            }
        } else {
            requestCameraAndAudioPermissions.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    private fun goToSpotlightWithPermissionCheck() {
        val cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (cameraGranted && audioGranted) {
            goToSpotlight()
            return
        }
        pendingActionAfterPermission = { goToSpotlight() }
        val needRationale =
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ||
                shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
        if (needRationale) {
            showRationaleDialog("ビデオチャットを利用するには、カメラとマイクの使用許可が必要です") {
                requestCameraAndAudioPermissions.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
            }
        } else {
            requestCameraAndAudioPermissions.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    private fun goToSimulcastWithPermissionCheck() {
        val cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (cameraGranted && audioGranted) {
            goToSimulcast()
            return
        }
        pendingActionAfterPermission = { goToSimulcast() }
        val needRationale =
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ||
                shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
        if (needRationale) {
            showRationaleDialog("ビデオチャットを利用するには、カメラとマイクの使用許可が必要です") {
                requestCameraAndAudioPermissions.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
            }
        } else {
            requestCameraAndAudioPermissions.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    private fun goToRpcChatWithPermissionCheck() {
        val cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (cameraGranted && audioGranted) {
            goToRpcChat()
            return
        }
        pendingActionAfterPermission = { goToRpcChat() }
        val needRationale =
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ||
                shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
        if (needRationale) {
            showRationaleDialog("RPC チャットを利用するには、カメラとマイクの使用許可が必要です") {
                requestCameraAndAudioPermissions.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
            }
        } else {
            requestCameraAndAudioPermissions.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    private fun goToVoiceRoomDemoWithPermissionCheck() {
        val audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (audioGranted) {
            goToVoiceRoomDemo()
            return
        }
        pendingActionAfterPermission = { goToVoiceRoomDemo() }
        if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            showRationaleDialog("ボイスチャット・スクリーンキャストを利用するには、マイクの使用許可が必要です") {
                requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        } else {
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun goToScreencastActivityWithPermissionCheck() {
        val audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (audioGranted) {
            goToScreencastActivity()
            return
        }
        pendingActionAfterPermission = { goToScreencastActivity() }
        if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            showRationaleDialog("ボイスチャット・スクリーンキャストを利用するには、マイクの使用許可が必要です") {
                requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        } else {
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
