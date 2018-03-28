package jp.shiguredo.webrtc.video.effector;

import android.os.Handler;

import org.webrtc.JavaI420Buffer;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.YuvHelper;

import java.nio.ByteBuffer;

public class CapturerObserverProxy implements VideoCapturer.CapturerObserver {
    public static final String TAG = CapturerObserverProxy.class.getSimpleName();

    private VideoCapturer.CapturerObserver originalObserver;
    private RTCVideoEffector videoEffector;

    public CapturerObserverProxy(final SurfaceTextureHelper surfaceTextureHelper,
                                 VideoCapturer.CapturerObserver observer,
                                 RTCVideoEffector effector) {

        this.originalObserver = observer;
        this.videoEffector = effector;

        final Handler handler = surfaceTextureHelper.getHandler();
        ThreadUtils.invokeAtFrontUninterruptibly(handler, () ->
                videoEffector.init(surfaceTextureHelper)
        );
    }

    @Override
    public void onCapturerStarted(boolean success) {
        this.originalObserver.onCapturerStarted(success);
    }

    @Override
    public void onCapturerStopped() {
        this.originalObserver.onCapturerStopped();
    }

    @Override
    public void onFrameCaptured(VideoFrame frame) {
        if (this.videoEffector.needToProcessFrame()) {
            // TODO(shino): libwebrtc 66.8.1, Android 7.0/Xperia Z4 では frame.getBuffer() は
            // org.webrtc.NV21Buffer で実装されている。
            // NV21Buffer には private data :: byte[] があるがアクセスは出来ない。
            // VideoEffectorLogger.d(TAG, "frame.getBuffer: " + frame.getBuffer());

            final VideoFrame.I420Buffer i420Buffer = frame.getBuffer().toI420();
            // TODO: JavaI420Buffer は direct ByteBuffer を3つ別に持っている。
            // それらが一直線かどうかは不明。実装次第だが toI420 でメモリコピーは不要。
            // VideoEffectorLogger.d(TAG, "frame.getBuffer() = " + frame.getBuffer());
            // VideoEffectorLogger.d(TAG, "i420Buffer = " + i420Buffer);
            frame.release();

            final int height = i420Buffer.getHeight();
            final int width = i420Buffer.getWidth();
            final int strideY = i420Buffer.getStrideY();
            final int strideU = i420Buffer.getStrideU();
            final int strideV = i420Buffer.getStrideV();

            final int chromaWidth = (width + 1) / 2;
            final int chromaHeight = (height + 1) / 2;
            final int dstSize = width * height + chromaWidth * chromaHeight * 2;
            ByteBuffer dst = ByteBuffer.allocateDirect(dstSize);
            // TODO: libyuv には I420ToARGB があるので NV12 に変換する必要はないかもしれない。
            //       そもそも i420Buffer の byte[] が一直線の保証はないし、
            //       下回りの byte[] を取得する方法も無い
            YuvHelper.I420ToNV12(i420Buffer.getDataY(), strideY,
                    i420Buffer.getDataU(), strideU,
                    i420Buffer.getDataV(), strideV,
                    dst, width, height);
            i420Buffer.release();

            byte[] effectedBytes =
                    this.videoEffector.processByteBufferFrame(dst.array(), width, height,
                            frame.getRotation(), frame.getTimestampNs());

            final int offsetY = 0;
            final int lengthY = strideY * height;
            final int offsetU = offsetY + lengthY;
            final int lengthU = strideU * chromaHeight;
            final int offsetV = offsetU + lengthU;
            final int lengthV = strideV * chromaHeight;

            // JavaI420Buffer.wrap には direct ByteBuffer を渡す必要がある。
            // VideoEffector#processByteBufferFrame の IN/OUT を ByteBuffer にすると
            // ミスマッチが減りそう。
            final ByteBuffer dataY = copy(effectedBytes, offsetY, lengthY);
            final ByteBuffer dataU = copy(effectedBytes, offsetU, lengthU);
            final ByteBuffer dataV = copy(effectedBytes, offsetV, lengthV);
            VideoFrame.I420Buffer filteredBuffer = JavaI420Buffer.wrap(
                    width, height,
                    dataY, strideY, dataU, strideU, dataV, strideV,
                    null);

            VideoFrame filteredVideoFrame = new VideoFrame(
                    filteredBuffer, frame.getRotation(), frame.getTimestampNs());
            filteredBuffer.release();
            this.originalObserver.onFrameCaptured(filteredVideoFrame);
        } else {
            this.originalObserver.onFrameCaptured(frame);
        }
    }

    private ByteBuffer copy(byte[] src, int offset, int length) {
        ByteBuffer data = ByteBuffer.allocateDirect(length);
        data.mark();
        data.put(src, offset, length);
        data.reset();
        return data;
    }
}
