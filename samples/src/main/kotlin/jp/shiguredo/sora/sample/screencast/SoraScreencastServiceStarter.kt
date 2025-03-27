package jp.shiguredo.sora.sample.screencast

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlin.reflect.KClass

@TargetApi(21)
class SoraScreencastServiceStarter(
    private val activity: Activity,
    private val signalingEndpoint: String,
    private val channelId: String,
    private val stateTitle: String,
    private val stateText: String,
    private val stateIcon: Int,
    private val notificationIcon: Int,
    private val signalingMetadata: String = "",
    private val videoScale: Float = 0.5f,
    private val videoFPS: Int = 30,
    private val videoCodec: String = "未指定",
    private val audioCodec: String = "OPUS",
    private val boundActivityName: String = activity.javaClass.canonicalName!!,
    private val serviceClass: KClass<SoraScreencastService>
) {
    companion object {
        const val REQ_CODE_SCREEN_CAPTURE = 4901
        const val REQ_CODE_OVERLAY = 4902
    }

    private var resultCode: Int? = null
    private var resultData: Intent? = null

    fun onActivityResult(reqCode: Int, resultCode: Int, data: Intent?): Boolean {
        return when (reqCode) {
            REQ_CODE_SCREEN_CAPTURE -> handleScreenCaptureResult(resultCode, data)
            REQ_CODE_OVERLAY -> startScreenCaptureService()
            else -> false
        }
    }

    fun start() {
        val manager: MediaProjectionManager = activity.application.getSystemService(
            Context.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager
        activity.startActivityForResult(
            manager.createScreenCaptureIntent(), REQ_CODE_SCREEN_CAPTURE
        )
    }

    private fun handleScreenCaptureResult(resultCode: Int, data: Intent?): Boolean {

        if (data == null) {
            return true
        }

        this.resultCode = resultCode
        this.resultData = data

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            startScreenCaptureService()
        } else {
            handleScreenCaptureResultOverMarshmallow()
        }
        return true
    }

    @TargetApi(23)
    private fun handleScreenCaptureResultOverMarshmallow() {
        if (Settings.canDrawOverlays(activity.applicationContext)) {
            startScreenCaptureService()
        } else {
            val uri = Uri.parse("package:${activity.packageName}")
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri)
            activity.startActivityForResult(intent, REQ_CODE_OVERLAY)
        }
    }

    private fun startScreenCaptureService(): Boolean {
        val intent = Intent(activity, serviceClass.java)
        val request = ScreencastRequest(
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
            stateIcon = stateIcon,
            notificationIcon = notificationIcon,
            boundActivityName = boundActivityName
        )
        intent.putExtra("SCREENCAST_REQUEST", request)
        activity.startService(intent)
        /*
         * startService を行った Activity は1つのアプリにもなれないので、
         * アプリ切り替えにも現れないよう finishAndRemoveTask でタスクごと消す
         */
        activity.finishAndRemoveTask()
        return true
    }
}
