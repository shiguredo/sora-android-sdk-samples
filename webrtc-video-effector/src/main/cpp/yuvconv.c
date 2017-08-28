#include <jni.h>
#include "libyuv.h"

JNIEXPORT void JNICALL
Java_jp_shiguredo_webrtc_video_effector_format_LibYuvBridge_yuvToRgbaInternal(
    JNIEnv *env,
    jobject obj,
    jbyteArray inYuvBytes,
    jint width,
    jint height,
    jintArray outRgbaBytes)
{
  uint8_t *rgbData = (uint8_t *)((*env)->GetPrimitiveArrayCritical(env, outRgbaBytes, 0));
  uint8_t *yuv = (uint8_t*) (*env)->GetPrimitiveArrayCritical(env, inYuvBytes, 0);

  const uint8* src_y = yuv;
  int src_stride_y = width;
  const uint8* src_vu = src_y + width * height;
  int src_stride_vu = (width + 1) / 2 * 2;;
  int dst_stride_argb = width * 4;

    NV12ToARGB(src_y,
         src_stride_y,
         src_vu,
         src_stride_vu,
         rgbData,
         dst_stride_argb,
         width,
         height);

  (*env)->ReleasePrimitiveArrayCritical(env, outRgbaBytes, rgbData, 0);
  (*env)->ReleasePrimitiveArrayCritical(env, inYuvBytes, yuv, 0);
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
