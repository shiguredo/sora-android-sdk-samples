package jp.shiguredo.sora.sample.ui

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sdk.util.SoraLogger
import org.jetbrains.anko.*
import org.jetbrains.anko.recyclerview.v7.recyclerView
import permissions.dispatcher.*

@RuntimePermissions
class MainActivity : AppCompatActivity() {

    val TAG = MainActivity::class.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {

        SoraLogger.enabled = true
        SoraLogger.libjingle_enabled = false

        super.onCreate(savedInstanceState)
        MainActivityUI().setContentView(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    internal fun goToDemo(position: Int) {
        when (position) {
            0 -> goToVideoRoomDemoWithPermissionCheck()
            1 -> goToVoiceRoomDemoWithPermissionCheck()
            2 -> goToScreencast()
            3 -> goToEffectedVideoRoomDemoWithPermissionCheck()
            else -> {
                Log.w(TAG, "must not come here")
            }
        }
    }

    private fun goToScreencast() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Snackbar.make(this.contentView!!,
                    getString(R.string.version_requirement_screencast),
                    Snackbar.LENGTH_LONG)
                    .setAction("OK") { }
                    .show()
        } else {
            goToScreencastAcitivity()
        }
    }

    @TargetApi(21)
    private fun goToScreencastAcitivity() {
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

    @OnShowRationale(value = [Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO])
    fun showRationaleForCameraAndAudio(request: PermissionRequest) {
        Log.d(TAG, "showRationaleForCameraAndAudio")
        showRationaleDialog(
                "ビデオチャットを利用するには、カメラとマイクの使用許可が必要です", request)
    }

    @OnShowRationale(Manifest.permission.RECORD_AUDIO)
    fun showRationaleForAudio(request: PermissionRequest) {
        showRationaleDialog(
                "ボイスチャットを利用するには、マイクの使用許可が必要です", request)
    }

    @OnPermissionDenied(value = [Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO])
    fun onCameraAndAudioDenied() {
        Log.d(TAG, "onCameraAndAudioDenied")
        Snackbar.make(this.contentView!!,
                "ビデオチャットを利用するには、カメラとマイクの使用を許可してください",
                Snackbar.LENGTH_LONG)
                .setAction("OK") { }
                .show()
    }

    @OnPermissionDenied(Manifest.permission.RECORD_AUDIO)
    fun onAudioDenied() {
        Snackbar.make(this.contentView!!,
                "ボイスチャットを利用するには、マイクの使用を許可してください",
                Snackbar.LENGTH_LONG)
                .setAction("OK") {  }
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

class MainActivityUI : AnkoComponent<MainActivity> {

    val TAG = MainActivityUI::class.simpleName

    val adapter = FeatureListAdapter(arrayListOf(
            Feature(title = "Video Chat Room",
                    description = "ビデオチャットのデモです。複数人でのグループチャットも可能です。"),
            Feature(title = "Voice Chat Room",
                    description = "ボイスチャットのデモです。複数人でのグループチャットも可能です。"),
            Feature(title = "Screencast",
                    description = "スクリーンキャストのデモです。"),
            Feature(title = "Effected Video Chat",
                    description = "エフェクト付きのビデオチャットのデモです")
    ))

    override fun createView(ui: AnkoContext<MainActivity>): View = with(ui) {

        adapter.setOnItemClickListener(object: FeatureListAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                Log.d(TAG, "clicked:" + position.toString())
                ui.owner.goToDemo(position)
            }
        })

        return verticalLayout {

            padding = dip(6)
            lparams(width = matchParent, height = matchParent)
            backgroundResource = R.drawable.app_background

            recyclerView {

                lparams(width = matchParent, height = wrapContent)
                layoutManager = LinearLayoutManager(ctx)
                adapter = this@MainActivityUI.adapter

            }
        }
    }
}
