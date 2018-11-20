#include <jni.h>
#include "libyuv.h"

JNIEXPORT void JNICALL
Java_jp_shiguredo_webrtc_video_effector_format_LibYuvBridge_i420ToRgbaInternal(
    JNIEnv *env,
    jobject obj,
    jbyteArray dataY, jint strideY,
    jbyteArray dataU, jint strideU,
    jbyteArray dataV, jint strideV,
    jint width, jint height,
    jbyteArray outRgba)
{
    uint8_t *dst_rgba = (uint8_t *)((*env)->GetPrimitiveArrayCritical(env, outRgba, 0));
    uint8_t *data_y = (uint8_t*) (*env)->GetPrimitiveArrayCritical(env, dataY, 0);
    uint8_t *data_u = (uint8_t*) (*env)->GetPrimitiveArrayCritical(env, dataU, 0);
    uint8_t *data_v = (uint8_t*) (*env)->GetPrimitiveArrayCritical(env, dataV, 0);

    int stride_y = strideY;
    int stride_u = strideU;
    int stride_v = strideV;

    int dst_stride_rgba = width * 4;
    int src_width = width;
    int src_height = height;

    /*
    LIBYUV_API
    int I420ToARGB(const uint8* src_y, int src_stride_y,
                   const uint8* src_u, int src_stride_u,
                   const uint8* src_v, int src_stride_v,
                   uint8* dst_argb, int dst_stride_argb,
                   int width, int height);
    */
    I420ToRGBA(data_y, stride_y,
               data_u, stride_u,
               data_v, stride_v,
               dst_rgba, dst_stride_rgba,
               src_width, src_height);

    (*env)->ReleasePrimitiveArrayCritical(env, outRgba, dst_rgba, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, dataY, data_y, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, dataU, data_u, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, dataV, data_v, 0);
}

JNIEXPORT void JNICALL
Java_jp_shiguredo_webrtc_video_effector_format_LibYuvBridge_rgbaToI420Internal(
        JNIEnv *env,
        jobject obj,
        jbyteArray rgbaArray,
        jint width,
        jint height,
        jbyteArray dataYArray,
        jint strideY,
        jbyteArray dataUArray,
        jint strideU,
        jbyteArray dataVArray,
        jint strideV)
{
    jbyte *rgba = (jbyte*) (*env)->GetPrimitiveArrayCritical(env, rgbaArray, 0);
    jbyte *data_y = (jbyte*) (*env)->GetPrimitiveArrayCritical(env, dataYArray, 0);
    jbyte *data_u = (jbyte*) (*env)->GetPrimitiveArrayCritical(env, dataUArray, 0);
    jbyte *data_v = (jbyte*) (*env)->GetPrimitiveArrayCritical(env, dataVArray, 0);

    /*
    // ABGR little endian (rgba in memory) to I420.
    LIBYUV_API
    int ABGRToI420(const uint8* src_frame, int src_stride_frame,
                   uint8* dst_y, int dst_stride_y,
                   uint8* dst_u, int dst_stride_u,
                   uint8* dst_v, int dst_stride_v,
                   int width, int height);

// ARGB little endian (bgra in memory) to I420.
    LIBYUV_API
    int ARGBToI420(const uint8* src_argb, int src_stride_argb,
                   uint8* dst_y, int dst_stride_y,
                   uint8* dst_u, int dst_stride_u,
                   uint8* dst_v, int dst_stride_v,
                   int width, int height);

    // RGBA little endian (abgr in memory) to I420.
    LIBYUV_API
    int RGBAToI420(const uint8* src_frame, int src_stride_frame,
                   uint8* dst_y, int dst_stride_y,
                   uint8* dst_u, int dst_stride_u,
                   uint8* dst_v, int dst_stride_v,
                   int width, int height);
    */

    RGBAToI420((uint8*) rgba, width * 4,
               (uint8*) data_y, strideY,
               (uint8*) data_u, strideU,
               (uint8*) data_v, strideV,
               width, height);

    (*env)->ReleasePrimitiveArrayCritical(env, rgbaArray, rgba, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, dataYArray, data_y, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, dataUArray, data_u, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, dataVArray, data_v, 0);
}
