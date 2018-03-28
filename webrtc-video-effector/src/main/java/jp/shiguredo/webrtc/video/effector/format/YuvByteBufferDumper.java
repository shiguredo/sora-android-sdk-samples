package jp.shiguredo.webrtc.video.effector.format;

// Dump YUV formatted bytes from RGB texture

import android.opengl.GLES20;

import java.nio.ByteBuffer;

import jp.shiguredo.webrtc.video.effector.VideoEffectorLogger;

public class YuvByteBufferDumper {

    public static final String TAG = YuvByteBufferDumper.class.getSimpleName();

    private int bufferId = -1;
    private LibYuvBridge libYuv = new LibYuvBridge();

    public YuvByteBufferDumper() {}

    public void init() {
        final int buffers[] = new int[1];
        GLES20.glGenFramebuffers(1, buffers, 0);
        bufferId = buffers[0];
    }

    public ByteBuffer dump(int lastTextureId, int width, int height) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, bufferId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                                GLES20.GL_TEXTURE_2D, lastTextureId, 0);

        final ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4);
        GLES20.glViewport(0, 0, width, height);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        buf.rewind();

        ByteBuffer yuv = ByteBuffer.allocateDirect(width*height*3/2);
        libYuv.rgbToYuv(buf.array(), width, height, yuv.array());
        return yuv;
    }

    public void dispose() {
        GLES20.glDeleteFramebuffers(1, new int[]{bufferId}, 0);
    }

}
