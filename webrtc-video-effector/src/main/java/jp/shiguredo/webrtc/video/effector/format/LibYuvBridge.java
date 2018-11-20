package jp.shiguredo.webrtc.video.effector.format;

import org.webrtc.VideoFrame;

public class LibYuvBridge {

    static {
        System.loadLibrary("yuvconv");
    }

    public LibYuvBridge() {}

    public void i420ToRgba(byte[] dataY, int strideY, byte[] dataU, int strideU,
            byte[] dataV, int strideV, int width, int height, byte[] outRgba) {
        i420ToRgbaInternal(dataY, strideY,
                dataU, strideU,
                dataV, strideV,
                width, height, outRgba);
    }

    public void rgbaToI420(byte[] rgba, int width, int height,
                           byte[] dataY, int strideY,
                           byte[] dataU, int strideU,
                           byte[] dataV, int strideV) {
        rgbaToI420Internal(rgba, width, height, dataY, strideY, dataU, strideU, dataV, strideV);
    }

    private native void i420ToRgbaInternal(byte[] dataY, int strideY, byte[] dataU, int strideU,
                                           byte[] dataV, int strideV, int width, int height,
                                           byte[] outRgba);

    private native void rgbaToI420Internal(byte[] rgba, int width, int height,
                                           byte[] dataY, int strideY,
                                           byte[] dataU, int strideU,
                                           byte[] dataV, int strideV);
}
