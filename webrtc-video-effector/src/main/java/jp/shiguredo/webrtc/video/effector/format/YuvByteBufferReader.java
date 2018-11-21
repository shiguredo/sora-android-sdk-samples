package jp.shiguredo.webrtc.video.effector.format;

import android.opengl.GLES20;

import org.webrtc.GlUtil;
import org.webrtc.VideoFrame;

import java.nio.ByteBuffer;

import jp.shiguredo.webrtc.video.effector.VideoEffectorLogger;

public class YuvByteBufferReader {

    public static final String TAG = YuvByteBufferReader.class.getSimpleName();

    public YuvByteBufferReader() {}

    private int textureId = -1;
    private int bufferId = -1;

    private int width = 0;
    private int height = 0;

    private LibYuvBridge libYuv = new LibYuvBridge();

    public void init() {
        textureId = GlUtil.generateTexture(GLES20.GL_TEXTURE_2D);

        final int buffers[] = new int[1];
        GLES20.glGenFramebuffers(1, buffers, 0);
        bufferId = buffers[0];
    }

    private void resizeTextureIfNeeded(int width, int height) {
        if (width == 0 || height == 0) {
            throw new IllegalArgumentException("invalid size of texture");
        }
        if (this.width == width && this.height == height) {
            // not changed,  do nothing
            return;
        }
        this.width  = width;
        this.height = height;
        resetTexture(width, height);
        GlUtil.checkNoGLES2Error("YuvByteBufferReader.resizeTextureIfNeeded");
    }

    private void resetTexture(int width, int height) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public int read(VideoFrame.I420Buffer i420Buffer, int width, int height) {
        resizeTextureIfNeeded(width, height);

        // TODO: direct buffer だと配列が取れない、コピー以外の方法はある?
        byte[] dataY = new byte[i420Buffer.getDataY().capacity()];
        i420Buffer.getDataY().get(dataY);
        byte[] dataU = new byte[i420Buffer.getDataU().capacity()];
        i420Buffer.getDataU().get(dataU);
        byte[] dataV = new byte[i420Buffer.getDataV().capacity()];
        i420Buffer.getDataV().get(dataV);

        ByteBuffer outRgba = ByteBuffer.allocate(width * height * 4);

        libYuv.i420ToRgba(
                dataY, i420Buffer.getStrideY(),
                dataU, i420Buffer.getStrideU(),
                dataV, i420Buffer.getStrideV(),
                width, height,
                outRgba.array());

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, width, height,
                                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, outRgba);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return textureId;
    }

    public void dispose() {
        GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
        GLES20.glDeleteFramebuffers(1, new int[]{bufferId}, 0);
    }
}
