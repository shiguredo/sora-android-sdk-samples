#include <jni.h>
#include "libyuv.h"

JNIEXPORT void JNICALL
Java_jp_shiguredo_webrtc_video_effector_format_LibYuvBridge_i420ToRgbaInternal(
    JNIEnv *env,
    jobject obj,
    jbyteArray dataY, jint strideY, jbyteArray dataU, jint strideU,
    jbyteArray dataV, jint strideV, jint width, jint height,
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

    I420ToRGBA(data_y, stride_y, data_u, stride_u,
               data_v, stride_v,
               dst_rgba, dst_stride_rgba, src_width, src_height);

    (*env)->ReleasePrimitiveArrayCritical(env, outRgba, dst_rgba, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, dataY, data_y, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, dataU, data_u, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, dataV, data_v, 0);
}

// most of this code is borrowed from
// https://github.com/alzybaad/RGB2YUV/
JNIEXPORT void JNICALL
Java_jp_shiguredo_webrtc_video_effector_format_LibYuvBridge_rgbToBgrInternal(
        JNIEnv *env,
        jobject obj,
        jbyteArray rgbArray,
        jint width,
        jint height,
        jbyteArray bgrArray)
{
    jbyte *rgb = (jbyte*) (*env)->GetPrimitiveArrayCritical(env, rgbArray, 0);
    jbyte *bgr = (jbyte*) (*env)->GetPrimitiveArrayCritical(env, bgrArray, 0);

    ABGRToARGB((uint8*) rgb, width << 2, (uint8*) bgr, width << 2, width, height);

    (*env)->ReleasePrimitiveArrayCritical(env, bgrArray, bgr, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, rgbArray, rgb, 0);
}

// most of this code is borrowed from
// https://github.com/alzybaad/RGB2YUV/
JNIEXPORT void JNICALL
Java_jp_shiguredo_webrtc_video_effector_format_LibYuvBridge_bgrToYuvInternal(
        JNIEnv *env,
        jobject obj,
        jbyteArray bgrArray,
        jint width,
        jint height,
        jbyteArray yuvArray)
{
    jbyte *bgr = (jbyte*)(*env)->GetPrimitiveArrayCritical(env, bgrArray, 0);
    jbyte *yuv = (jbyte*)(*env)->GetPrimitiveArrayCritical(env, yuvArray, 0);
    ARGBToNV21((uint8*) bgr, width << 2, (uint8*) yuv, width, (uint8*) &yuv[width * height], width, width, height);
    (*env)->ReleasePrimitiveArrayCritical(env, yuvArray, yuv, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, bgrArray, bgr, 0);
}
