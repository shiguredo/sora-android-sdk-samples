package jp.shiguredo.sora.sample.screencast

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.provider.Settings
import kotlin.reflect.KClass

@TargetApi(21)
class SoraScreencastServiceStarter(
    private val activity: Activity,
    private val signalingEndpoint: String,
    private val channelId: String,
    private val stateTitle: String,
    private val stateText: String,
    private val notificationIcon: Int,
    private val boundActivityName: String,
    private val signalingMetadata: String = "",
    private val videoScale: Float = 0.5f,
    private val videoFPS: Int = 30,
    private val videoCodec: String = "未指定",
    private val audioCodec: String = "OPUS",
    private val serviceClass: KClass<SoraScreencastService>,
) {
    companion object {
        const val REQ_CODE_SCREEN_CAPTURE = 4901
        const val REQ_CODE_OVERLAY = 4902
    }

    private var resultData: Intent? = null

    fun onActivityResult(
        reqCode: Int,
        data: Intent?,
    ) {
        when (reqCode) {
            REQ_CODE_SCREEN_CAPTURE -> handleScreenCaptureResult(data)
            REQ_CODE_OVERLAY -> startScreenCaptureService()
        }
    }

    fun start() {
        val manager: MediaProjectionManager =
            activity.application.getSystemService(
                Context.MEDIA_PROJECTION_SERVICE,
            ) as MediaProjectionManager
        activity.startActivityForResult(
            manager.createScreenCaptureIntent(),
            REQ_CODE_SCREEN_CAPTURE,
        )
    }

    private fun handleScreenCaptureResult(data: Intent?) {
        if (data == null) {
            return
        }

        this.resultData = data

        handleScreenCaptureResultWithOverlayPermission()
    }

    private fun handleScreenCaptureResultWithOverlayPermission() {
        if (Settings.canDrawOverlays(activity.applicationContext)) {
            startScreenCaptureService()
        } else {
            val uri = Uri.parse("package:${activity.packageName}")
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri)
            activity.startActivityForResult(intent, REQ_CODE_OVERLAY)
        }
    }

    private fun startScreenCaptureService() {
        val intent = Intent(activity, serviceClass.java)
        val request =
            ScreencastRequest(
                data = resultData!!,
                signalingEndpoint = signalingEndpoint,
                channelId = channelId,
                signalingMetadata = signalingMetadata,
                audioCodec = audioCodec,
                videoCodec = videoCodec,
                videoFPS = videoFPS,
                videoScale = videoScale,
                stateTitle = stateTitle,
                stateText = stateText,
                notificationIcon = notificationIcon,
                boundActivityName = boundActivityName,
            )
        intent.putExtra("SCREENCAST_REQUEST", request)
        activity.startService(intent)
        /*
         * startService を行った Activity は1つのアプリにもなれないので、
         * アプリ切り替えにも現れないよう finishAndRemoveTask でタスクごと消す
         * 配信する画面は SoraScreencastService が起動する
         */
        activity.finishAndRemoveTask()
    }
}
