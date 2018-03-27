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
    public void onByteBufferFrameCaptured(byte[] bytes, int width, int height,
                                          int rotation, long timestamp) {

        if (this.videoEffector.needToProcessFrame()) {

            byte[] filteredBytes =
                    this.videoEffector.processByteBufferFrame(bytes, width, height,
                            rotation, timestamp);

            this.originalObserver.onByteBufferFrameCaptured(filteredBytes, width, height,
                    rotation, timestamp);
            surfaceTextureHelper.returnTextureFrame();

        } else {

            this.originalObserver.onByteBufferFrameCaptured(bytes, width, height,
                    rotation, timestamp);

        }
    }

    @Override
    public void onTextureFrameCaptured(int width, int height, int oesTextureId,
                                       float[] transformMatrix, int rotation, long timestamp) {

        this.originalObserver.onTextureFrameCaptured(width, height, oesTextureId,
                transformMatrix, rotation, timestamp);

    }

    @Override
    public void onFrameCaptured(VideoFrame frame) {
        if (this.videoEffector.needToProcessFrame()) {
            VideoEffectorLogger.d(TAG, "onFrameCaptured: " + frame.getClass().getName());
            VideoEffectorLogger.d(TAG, "onFrameCaptured: " + frame.getBuffer().getClass().getName());
            VideoEffectorLogger.d(TAG, "onFrameCaptured: " + frame.getBuffer().toI420().getClass().getName());
            final VideoFrame.I420Buffer buffer = frame.getBuffer().toI420();

            final int height = buffer.getHeight();
            final int width = buffer.getWidth();
            final int strideY = buffer.getStrideY();
            final int strideU = buffer.getStrideU();
            final int strideV = buffer.getStrideV();

            final int chromaWidth = (width + 1) / 2;
            final int chromaHeight = (height + 1) / 2;
            final int dstSize = width * height + chromaWidth * chromaHeight * 2;
            ByteBuffer dst = ByteBuffer.allocateDirect(dstSize);
            YuvHelper.I420ToNV12(buffer.getDataY(), strideY, buffer.getDataU(), strideU,
                    buffer.getDataV(), strideV, dst, width, height);

            byte[] bytes = dst.array();

            byte[] filteredBytes =
                    this.videoEffector.processByteBufferFrame(bytes, width, height,
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
                    // TODO: どうにかする
                    null);

            VideoFrame filteredVideoFrame = new VideoFrame(
                    filteredBuffer, frame.getRotation(), frame.getTimestampNs());
            this.originalObserver.onFrameCaptured(filteredVideoFrame);
        } else {
            this.originalObserver.onFrameCaptured(frame);
        }
    }
}
