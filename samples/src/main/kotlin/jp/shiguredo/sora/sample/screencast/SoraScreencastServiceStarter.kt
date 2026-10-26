package jp.shiguredo.sora.sample.screencast

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.provider.Settings
import jp.shiguredo.sora.sdk.util.SoraLogger
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
        private val TAG = SoraScreencastServiceStarter::class.simpleName

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
            REQ_CODE_OVERLAY -> handleOverlayPermissionResult()
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
            notifyStartFailed(SoraScreencastService.START_FAILURE_REASON_CANCELED)
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

    private fun handleOverlayPermissionResult() {
        if (Settings.canDrawOverlays(activity.applicationContext)) {
            startScreenCaptureService()
            return
        }
        notifyStartFailed(SoraScreencastService.START_FAILURE_REASON_PERMISSION)
    }

    private fun startScreenCaptureService() {
        val data =
            resultData ?: run {
                notifyStartFailed(SoraScreencastService.START_FAILURE_REASON_UNKNOWN)
                return
            }
        val intent = Intent(activity, serviceClass.java)
        val request =
            ScreencastRequest(
                data = data,
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
        try {
            activity.startService(intent)
        } catch (e: RuntimeException) {
            SoraLogger.w(TAG, "failed to start screencast service", e)
            notifyStartFailed(SoraScreencastService.START_FAILURE_REASON_UNKNOWN)
            return
        }
        /*
         * 接続設定画面は開始結果を受け取るまで残す
         * 配信する画面は SoraScreencastService が起動する
         */
    }

    private fun notifyStartFailed(reason: String) {
        val intent = Intent(SoraScreencastService.ACTION_SCREENCAST_START_FAILED)
        intent.setPackage(activity.packageName)
        intent.putExtra(SoraScreencastService.EXTRA_START_FAILURE_REASON, reason)
        activity.sendBroadcast(intent)
    }
}
