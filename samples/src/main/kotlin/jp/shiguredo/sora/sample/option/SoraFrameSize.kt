package jp.shiguredo.sora.sample.option

import jp.shiguredo.sora.sdk.channel.option.SoraVideoOption

class SoraFrameSize {
    companion object {
        val landscape = mapOf(
            "QCIF: 176x144" to SoraVideoOption.FrameSize.Landscape.QCIF,
            "HQVGA: 240x160" to SoraVideoOption.FrameSize.Landscape.HQVGA,
            "QVGA: 320x240" to SoraVideoOption.FrameSize.Landscape.QVGA,
            "VGA: 640x480" to SoraVideoOption.FrameSize.Landscape.VGA,
            "HD: 1280x720" to SoraVideoOption.FrameSize.Landscape.HD,
            "FHD: 1920x1080" to SoraVideoOption.FrameSize.Landscape.FHD,
            "3840x1920" to SoraVideoOption.FrameSize.Landscape.Res3840x1920,
            "UHD: 3840x2160" to SoraVideoOption.FrameSize.Landscape.UHD3840x2160,
            "4096x2160" to SoraVideoOption.FrameSize.Landscape.UHD4096x2160,
        )
        val portrait = mapOf(
            "QCIF: 144x176" to SoraVideoOption.FrameSize.Portrait.QCIF,
            "HQVGA: 160x240" to SoraVideoOption.FrameSize.Portrait.HQVGA,
            "QVGA: 240x320" to SoraVideoOption.FrameSize.Portrait.QVGA,
            "VGA: 480x640" to SoraVideoOption.FrameSize.Portrait.VGA,
            "HD: 720x1280" to SoraVideoOption.FrameSize.Portrait.HD,
            "FHD: 1080x1920" to SoraVideoOption.FrameSize.Portrait.FHD,
            "3840x1920" to SoraVideoOption.FrameSize.Portrait.Res1920x3840,
            "UHD: 3840x2160" to SoraVideoOption.FrameSize.Portrait.UHD2160x3840,
            "4096x2160" to SoraVideoOption.FrameSize.Portrait.UHD2160x4096,
        )

        val all = landscape + portrait
    }
}
