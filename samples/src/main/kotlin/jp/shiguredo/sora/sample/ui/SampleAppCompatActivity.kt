package jp.shiguredo.sora.sample.ui

import androidx.appcompat.app.AppCompatActivity
import jp.shiguredo.sora.sdk2.VideoFrameSize

open class SampleAppCompatActivity: AppCompatActivity() {

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

}