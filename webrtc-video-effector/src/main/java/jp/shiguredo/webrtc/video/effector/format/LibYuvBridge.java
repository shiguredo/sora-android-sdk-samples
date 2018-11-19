package jp.shiguredo.webrtc.video.effector.format;

public class LibYuvBridge {

    static {
        System.loadLibrary("yuvconv");
    }

    public LibYuvBridge() {}

    public void yuvToRgba(byte[] yuv, int width, int height, byte[] out) {
        yuvToRgbaInternal(yuv, width, height, out);
    }

    private byte[] tempBgr;

    public void rgbToYuv(byte[] rgb, int width, int height, byte[] yuv) {
        if (tempBgr == null || tempBgr.length < rgb.length) {
            tempBgr = new byte[rgb.length];
        }
        rgbToBgrInternal(rgb, width, height, tempBgr);
        bgrToYuvInternal(tempBgr, width, height, yuv);
    }

    private native void yuvToRgbaInternal(byte[] yuv, int width, int height, byte[] out);
    private native void rgbToBgrInternal(byte[] rgb, int width, int height, byte[] bgr);
    private native void bgrToYuvInternal(byte[] bgr, int width, int height, byte[] yuv);
}
