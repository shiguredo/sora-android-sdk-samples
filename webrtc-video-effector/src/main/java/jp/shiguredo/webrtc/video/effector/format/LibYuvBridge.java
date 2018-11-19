package jp.shiguredo.webrtc.video.effector.format;

import org.webrtc.VideoFrame;

public class LibYuvBridge {

    static {
        System.loadLibrary("yuvconv");
    }

    public LibYuvBridge() {}

    public void i420ToRgba(VideoFrame.I420Buffer i420Buffer, int width, int height, byte[] out) {
        i420ToRgbaInternal(i420Buffer.getDataY().array(), i420Buffer.getStrideY(),
                i420Buffer.getDataU().array(), i420Buffer.getStrideU(),
                i420Buffer.getDataV().array(), i420Buffer.getStrideV(),
                width, height, out);
    }

    private byte[] tempBgr;

    public void rgbToYuv(byte[] rgb, int width, int height, byte[] yuv) {
        if (tempBgr == null || tempBgr.length < rgb.length) {
            tempBgr = new byte[rgb.length];
        }
        rgbToBgrInternal(rgb, width, height, tempBgr);
        bgrToYuvInternal(tempBgr, width, height, yuv);
    }

    private native void i420ToRgbaInternal(byte[] dataY, int strideY, byte[] dataU, int strideU,
                                           byte[] dataV, int strideV, int width, int height,
                                           byte[] out);
    private native void rgbToBgrInternal(byte[] rgb, int width, int height, byte[] bgr);
    private native void bgrToYuvInternal(byte[] bgr, int width, int height, byte[] yuv);
}
