package jp.shiguredo.webrtc.video.effector;

import android.os.Handler;

import org.webrtc.CapturerObserver;
import org.webrtc.NV12Buffer;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.YuvHelper;

import java.nio.ByteBuffer;

public class CapturerObserverProxy implements CapturerObserver {
    public static final String TAG = CapturerObserverProxy.class.getSimpleName();

    private CapturerObserver originalObserver;
    private RTCVideoEffector videoEffector;

    public CapturerObserverProxy(final SurfaceTextureHelper surfaceTextureHelper,
                                 CapturerObserver observer,
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
            VideoFrame.I420Buffer effectedI420Buffer =
                    this.videoEffector.processByteBufferFrame(
                            frame.getBuffer().toI420(), frame.getRotation(), frame.getTimestampNs());

            VideoFrame effectedVideoFrame = new VideoFrame(
                    effectedI420Buffer, frame.getRotation(), frame.getTimestampNs());
            frame.release();

            this.originalObserver.onFrameCaptured(effectedVideoFrame);
        } else {
            this.originalObserver.onFrameCaptured(frame);
        }
    }
}
