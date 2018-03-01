package jp.shiguredo.webrtc.video.effector.format;

import android.opengl.GLES20;

import org.webrtc.GlUtil;

import java.nio.IntBuffer;

import jp.shiguredo.webrtc.video.effector.VideoEffectorLogger;

// Read RGB texture from YUV formatted bytes

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

    public int read(byte[] data, int width, int height) {
        VideoEffectorLogger.d(TAG, "read");
        resizeTextureIfNeeded(width, height);

        IntBuffer buf = IntBuffer.allocate(width * height);
        libYuv.yuvToRgba(data, width, height, buf.array());

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, width, height,
                                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return textureId;
    }

    public void dispose() {
        GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
        GLES20.glDeleteFramebuffers(1, new int[]{bufferId}, 0);
    }
}
