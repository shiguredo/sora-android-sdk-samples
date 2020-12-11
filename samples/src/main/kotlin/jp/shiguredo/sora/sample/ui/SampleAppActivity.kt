package jp.shiguredo.sora.sample.ui

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sdk2.*
import java.util.*

open class SampleAppActivity: AppCompatActivity() {

    val channelName: String
        get() = intent.getStringExtra("CHANNEL_NAME") ?: getString(R.string.channelId) ?: ""

    val role: Role
        get() = when (intent.getStringExtra("ROLE")) {
            "SENDONLY" -> Role.SENDONLY
            "RECVONLY" -> Role.RECVONLY
            "SENDRECV" -> Role.SENDRECV
            else       -> Role.SENDRECV
        }

    val multistreamEnabled: Boolean
        get() = when (intent.getStringExtra("MULTISTREAM")) {
            "有効" -> true
            else      -> false
        }

    val spotlight: Int
        get() = intent.getIntExtra("SPOTLIGHT", 0)

    val videoFrameSize: VideoFrameSize
        get() =
            when (intent.getStringExtra("VIDEO_SIZE")) {
                // Portrait
                "VGA"          -> VideoFrameSize.VGA.portrate
                "QQVGA"        -> VideoFrameSize.QQVGA.portrate
                "QCIF"         -> VideoFrameSize.QCIF.portrate
                "HQVGA"        -> VideoFrameSize.HQVGA.portrate
                "QVGA"         -> VideoFrameSize.QVGA.portrate
                "HD"           -> VideoFrameSize.HD.portrate
                "FHD"          -> VideoFrameSize.FHD.portrate
                "Res1920x3840" -> VideoFrameSize.Res3840x1920.portrate
                "UHD2160x3840" -> VideoFrameSize.UHD3840x2160.portrate
                "UHD2160x4096" -> VideoFrameSize.UHD4096x2160.portrate
                // Landscape
                "Res3840x1920" -> VideoFrameSize.Res3840x1920.landscape
                "UHD3840x2160" -> VideoFrameSize.UHD3840x2160.landscape
                // Default
                else           -> VideoFrameSize.VGA.portrate
            }

    val videoEnabled: Boolean
        get() = when (intent.getStringExtra("VIDEO_ENABLED")) {
            "有効" -> true
            "無効"  -> false
            else  -> true
        }

    val videoCodec: VideoCodec
        get() = VideoCodec.valueOf(intent.getStringExtra("VIDEO_CODEC") ?: "VP9")

    val videoFps: Int
        get() = (intent.getStringExtra("FPS") ?: "30").toInt()

    val videoBitRate: Int?
        get() = when (val stringValue = intent.getStringExtra("VIDEO_BIT_RATE")) {
            "未指定" -> null
            else -> stringValue?.toInt()
        }

    val audioEnabled: Boolean
        get() = when (intent.getStringExtra("AUDIO_ENABLED")) {
            "有効" -> true
            "無効"  -> false
            else  -> true
        }

    val audioCodec: AudioCodec
        get() = AudioCodec.valueOf(intent.getStringExtra("AUDIO_CODEC") ?: "OPUS")

    val audioBitRate: Int?
        get() = when (val stringValue = intent.getStringExtra("AUDIO_BIT_RATE")) {
            "未指定" -> null
            else -> stringValue?.toInt()
        }

    val audioSound: AudioSound
        get() = when (intent.getStringExtra("AUDIO_STEREO")) {
            "モノラル"   -> AudioSound.MONO
            "ステレオ" -> AudioSound.STEREO
            else     -> AudioSound.MONO
        }

    val fixedResolution: Boolean
        get() = when (intent.getStringExtra("RESOLUTION_CHANGE")) {
            "可変" -> false
            "固定"    -> true
            else       -> false
        }

    val cameraFacing: Boolean
        get() = when (intent.getStringExtra("CAMERA_FACING")) {
            "前面" -> true
            "背面"  -> false
            else    -> true
        }

    fun setRequestedOrientation() {
        // ステレオでは landscape にしたほうが内蔵マイクを使うときに自然な向きとなる。
        // それ以外は、リモート映像の分割が簡単になるように portrait で動かす。
        if (audioSound == AudioSound.STEREO) {
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        } else {
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    var mediaChannel: MediaChannel? = null

}