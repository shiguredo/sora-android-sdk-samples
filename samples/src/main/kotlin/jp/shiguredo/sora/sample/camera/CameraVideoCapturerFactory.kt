// 現在は一種類のカメラキャプチャしか使用していないが、将来的に複数のカメラキャプチャーを実装する可能性があるため、
// インターフェースを定義している。
// 将来的に、複数のカメラキャプチャーをサポートする場合は、このインターフェースを実装した複数のファクトリを用意することができる
package jp.shiguredo.sora.sample.camera

import android.content.Context
import jp.shiguredo.sora.sdk.camera.CameraCapturerFactory
import org.webrtc.CameraVideoCapturer

interface CameraVideoCapturerFactory {
    fun createCapturer(): CameraVideoCapturer?
}

class DefaultCameraVideoCapturerFactory(
    private val context: Context,
    private val frontFacingFirst: Boolean = true
) : CameraVideoCapturerFactory {

    override fun createCapturer(): CameraVideoCapturer? {
        return CameraCapturerFactory.create(context, frontFacingFirst = frontFacingFirst)
    }
}
