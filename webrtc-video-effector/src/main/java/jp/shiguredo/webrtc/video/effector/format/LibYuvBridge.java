package jp.shiguredo.webrtc.video.effector.format;

import org.webrtc.VideoFrame;

import java.nio.ByteBuffer;

/** I420 と RGBA の間の変換を受け持つクラス。
 *
 * メモリイメージとして RGBA にしたい。libyuv の RGBA 変換はメモリ上のバイト順が
 * 逆順になる。
 * I420ToARGB() の出力はバイト順として B G R A、I420ToABGR() は R G B A  である。
 * 関数の命名として、Java の世界はメモリ順で命名し、native method では libyuv に
 * 合わせて逆順とする。
 */
public class LibYuvBridge {

    static {
        System.loadLibrary("yuvconv");
    }

    public LibYuvBridge() {}

    public void i420ToRgba(ByteBuffer dataYBuffer, int strideY,
                           ByteBuffer dataUBuffer, int strideU,
                           ByteBuffer dataVBuffer, int strideV,
                           int width, int height,
                           byte[] outRgba) {
        i420ToAbgrInternal(
                dataYBuffer, strideY,
                dataUBuffer, strideU,
                dataVBuffer, strideV,
                width, height,
                outRgba);
    }

    public void rgbaToI420(byte[] rgba,
                           int width, int height,
                           byte[] dataY, int strideY,
                           byte[] dataU, int strideU,
                           byte[] dataV, int strideV) {
        abgrToI420Internal(
                rgba,
                width, height,
                dataY, strideY, dataU, strideU, dataV, strideV);
    }

    private native void i420ToAbgrInternal(
            ByteBuffer dataYBuffer, int strideY,
            ByteBuffer dataUBuffer, int strideU,
            ByteBuffer dataVBuffer, int strideV,
            int width, int height,
            byte[] outRgba);

    private native void abgrToI420Internal(
            byte[] rgba,
            int width, int height,
            byte[] dataY, int strideY,
            byte[] dataU, int strideU,
            byte[] dataV, int strideV);
}
