package jp.shiguredo.webrtc.video.effector;

import org.webrtc.GlUtil;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.ThreadUtils;

import java.util.ArrayList;
import java.util.List;

import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.shiguredo.webrtc.video.effector.filter.FrameImageFilter;
import jp.shiguredo.webrtc.video.effector.filter.GPUImageFilterWrapper;
import jp.shiguredo.webrtc.video.effector.filter.MediaEffectFilter;
import jp.shiguredo.webrtc.video.effector.format.YuvByteBufferDumper;
import jp.shiguredo.webrtc.video.effector.format.YuvByteBufferReader;

public class RTCVideoEffector {

    public static final String TAG = RTCVideoEffector.class.getSimpleName();

    public RTCVideoEffector() {}

    private VideoEffectorContext context = new VideoEffectorContext();
    private List<FrameImageFilter> filters = new ArrayList<>();
    private boolean enabled = true;

    private YuvByteBufferReader yuvBytesReader;
    private YuvByteBufferDumper yuvBytesDumper;

    private SurfaceTextureHelper helper;

    void init(SurfaceTextureHelper helper) {

        VideoEffectorLogger.d(TAG, "init");

        this.helper = helper;

        yuvBytesReader = new YuvByteBufferReader();
        yuvBytesReader.init();

        yuvBytesDumper = new YuvByteBufferDumper();
        yuvBytesDumper.init();


        for (FrameImageFilter filter : filters) {
            filter.init();
        }

        GlUtil.checkNoGLES2Error("RTCVideoEffector.init");
    }

    public void addFilter(FrameImageFilter filter) {
        this.filters.add(filter);
    }

    public void addMediaEffectFilter(String name) {
        addMediaEffectFilter(name, null);
    }
    public void addMediaEffectFilter(String name,
                                     MediaEffectFilter.Listener listener) {
        this.filters.add(new MediaEffectFilter(name, listener));
    }

    public void addGPUImageFilter(GPUImageFilter filter) {
        this.filters.add(new GPUImageFilterWrapper(filter));
    }
    public void addGPUImageFilter(GPUImageFilter filter,
                                  GPUImageFilterWrapper.Listener listener) {
        this.filters.add(new GPUImageFilterWrapper(filter, listener));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }

    byte[] processByteBufferFrame(byte[] bytes, int width, int height,
                                  int rotation, long timestamp) {

        VideoEffectorLogger.d(TAG, "processByteBufferFrame");

        if (!needToProcessFrame()) {
            return bytes;
        }

        context.updateFrameInfo(bytes, width, height, rotation, timestamp);

        int stepTextureId = yuvBytesReader.read(bytes, width, height);

        // ビデオフレームの画像は回転された状態で来ることがある
        // グレースケールやセピアフィルタなど、画像全体に均質にかけるエフェクトでは問題にならないが
        // 座標を指定する必要のあるエフェクトでは、使いにくいものとなる。

        // そのため、場合によっては、フィルタをかける前後で回転の補正を行う必要がある
        // ただし、そのためのtexture間のコピーが二度発生することになる
        // 必要のないときはこの機能は使わないようにon/offできるようにしておきたい

        if (context.getFrameInfo().isRotated()) {
            // TODO
        }

        for (FrameImageFilter filter : filters) {
            if (filter.isEnabled()) {
                VideoEffectorLogger.d(TAG, "execute filter");
                stepTextureId = filter.filter(context, stepTextureId);
            }
        }

        if (context.getFrameInfo().isRotated()) {
            // TODO
        }

        return yuvBytesDumper.dump(stepTextureId, width, height);
    }

    boolean needToProcessFrame() {
        if (!enabled) {
            return false;
        }
        if (filters.size() > 0) {
            for (FrameImageFilter filter : this.filters) {
                if (filter.isEnabled()) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    public void dispose() {
        if (helper != null) {
            // This effector is not initialized
            return;
        }
        ThreadUtils.invokeAtFrontUninterruptibly(this.helper.getHandler(), new Runnable() {
            @Override
            public void run() {
                disposeInternal();
            }
        });
    }

    private void disposeInternal() {
        for (FrameImageFilter filter : filters) {
            filter.dispose();
        }
        yuvBytesReader.dispose();
        yuvBytesDumper.dispose();
    }
}
