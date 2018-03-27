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
    private SurfaceTextureHelper surfaceTextureHelper;
    private RTCVideoEffector videoEffector;

    public CapturerObserverProxy(final SurfaceTextureHelper surfaceTextureHelper,
                                 VideoCapturer.CapturerObserver observer,
                                 RTCVideoEffector effector) {

        this.surfaceTextureHelper = surfaceTextureHelper;
        this.originalObserver = observer;
        this.videoEffector = effector;

        final Handler handler = this.surfaceTextureHelper.getHandler();
        ThreadUtils.invokeAtFrontUninterruptibly(handler, new Runnable() {
            @Override
            public void run() {
                videoEffector.init(surfaceTextureHelper);
            }
        });
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
            final VideoFrame.I420Buffer buffer = frame.getBuffer().toI420();
            frame.release();

            final int height = buffer.getHeight();
            final int width = buffer.getWidth();
            final int strideY = buffer.getStrideY();
            final int strideU = buffer.getStrideU();
            final int strideV = buffer.getStrideV();

            final int chromaWidth = (width + 1) / 2;
            final int chromaHeight = (height + 1) / 2;
            final int dstSize = width * height + chromaWidth * chromaHeight * 2;
            ByteBuffer dst = ByteBuffer.allocateDirect(dstSize);
            // TODO: libyuv に I420ToARGB があるので NV12 に変換する必要はないかもしれなが、
            //       下回りの byte[] を取得する方法がわからない
            YuvHelper.I420ToNV12(buffer.getDataY(), strideY, buffer.getDataU(), strideU,
                    buffer.getDataV(), strideV, dst, width, height);
            buffer.release();

            byte[] filteredBytes =
                    this.videoEffector.processByteBufferFrame(dst.array(), width, height,
                            frame.getRotation(), frame.getTimestampNs());

            final int offsetY = 0;
            final int lengthY = strideY * height;
            VideoEffectorLogger.d(TAG, "lengthY = " + lengthY);
            final int offsetU = offsetY + lengthY;
            final int lengthU = strideU * chromaHeight;
            final int offsetV = offsetU + lengthU;
            final int lengthV = strideV * chromaHeight;

            final ByteBuffer dataY = ByteBuffer.allocateDirect(lengthY);
            dataY.mark();
            dataY.put(filteredBytes, offsetY, lengthY);
            dataY.reset();
            final ByteBuffer dataU = ByteBuffer.allocateDirect(lengthU);
            dataU.mark();
            dataU.put(filteredBytes, offsetU, lengthU);
            dataU.reset();
            final ByteBuffer dataV = ByteBuffer.allocateDirect(lengthV);
            dataV.mark();
            dataV.put(filteredBytes, offsetV, lengthV);
            dataV.reset();

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
}
