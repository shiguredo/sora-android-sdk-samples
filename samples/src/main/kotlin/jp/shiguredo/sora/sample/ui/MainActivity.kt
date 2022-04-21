package jp.shiguredo.sora.sample.ui

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.databinding.ActivityMainBinding
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
                    title = "メッセージング",
                    description = "メッセージングのデモです"
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
        val intent = Intent(this, ScreencastSetupActivity::class.java)
        startActivity(intent)
    }

    @NeedsPermission(value = [Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.MANAGE_EXTERNAL_STORAGE])
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
