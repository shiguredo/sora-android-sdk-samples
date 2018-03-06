package jp.shiguredo.webrtc.video.effector;

import android.os.Handler;

import org.webrtc.SurfaceTextureHelper;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;

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
        this.originalObserver.onFrameCaptured(frame);
    }
}
