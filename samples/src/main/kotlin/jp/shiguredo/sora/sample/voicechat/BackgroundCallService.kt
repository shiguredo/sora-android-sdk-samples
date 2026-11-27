package jp.shiguredo.sora.sample.voicechat

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import jp.shiguredo.sora.sample.R

/**
 * ボイスチャットをバックグラウンドで継続するための Foreground Service です。
 *
 * 通話そのものは Activity が保持し、この Service は microphone タイプの FGS、
 * 常駐通知、PARTIAL_WAKE_LOCK だけを管理します。通知の遷移先は起動時に渡された
 * Intent を使用します。
 */
class BackgroundCallService : Service() {
    companion object {
        private val TAG = BackgroundCallService::class.simpleName

        /**
         * FGS の開始に失敗したことを Activity に通知するアクションです。
         */
        internal const val ACTION_BACKGROUND_CALL_START_FAILED =
            "jp.shiguredo.sora.sample.ACTION_BACKGROUND_CALL_START_FAILED"

        private const val EXTRA_CONTENT_INTENT = "CONTENT_INTENT"
        private const val NOTIFICATION_CHANNEL_ID = "jp.shiguredo.sora.sample.background_call"
        private const val NOTIFICATION_ID = 1001
        private const val WAKE_LOCK_TAG = "jp.shiguredo.sora.sample:background_call"

        @Volatile
        private var running = false

        fun isRunning(): Boolean = running

        fun start(
            context: Context,
            contentIntent: Intent,
        ) {
            val serviceIntent =
                Intent(context, BackgroundCallService::class.java).apply {
                    putExtra(EXTRA_CONTENT_INTENT, contentIntent)
                }
            ContextCompat.startForegroundService(context, serviceIntent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BackgroundCallService::class.java))
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var contentIntent: Intent? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        Log.d(TAG, "onStartCommand")
        contentIntent =
            intent?.let {
                IntentCompat.getParcelableExtra(it, EXTRA_CONTENT_INTENT, Intent::class.java)
            } ?: contentIntent
        val foregroundServiceType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                0
            }
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                foregroundServiceType,
            )
        } catch (e: Exception) {
            // FGS を開始できないとバックグラウンド通話を継続できないため、Service を終了する
            Log.e(TAG, "failed to start foreground service", e)
            sendBroadcast(
                Intent(ACTION_BACKGROUND_CALL_START_FAILED).apply {
                    setPackage(packageName)
                },
            )
            stopSelf()
            return START_NOT_STICKY
        }

        acquireWakeLock()
        running = true
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        running = false
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.background_call_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.background_call_notification_channel_description)
                setShowBadge(false)
            }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val builder =
            NotificationCompat
                .Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mic_white_48dp)
                .setContentTitle(getString(R.string.background_call_notification_title))
                .setContentText(getString(R.string.background_call_notification_text))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        contentIntent?.let {
            val launchIntent =
                Intent(it).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            builder.setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }
        return builder.build()
    }

    // 通話が続く間は WakeLock を保持する必要があるため、タイムアウトを指定しない
    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) {
            return
        }
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock =
            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
                acquire()
            }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }
}
