package jp.shiguredo.sora.sample.ui.util

import android.content.Context
import jp.shiguredo.sora.sdk.util.SoraLogger
import org.webrtc.EglBase
import org.webrtc.MediaStream
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

// TODO support NUMBER_OF_RENDERERS limitation
class SoraRemoteRendererSlot(
    val context: Context,
    val eglContext: EglBase.Context,
    var listener: Listener?,
) {
    companion object {
        private val TAG = SoraRemoteRendererSlot::class.simpleName
    }

    interface Listener {
        fun onAddRenderer(renderer: SurfaceViewRenderer)

        fun onRemoveRenderer(renderer: SurfaceViewRenderer)

        // onAddRenderer の後に呼ばれる
        // 映像の表示サイズ (rotation を反映した幅と高さ) が変わった際の通知
        // libwebrtc のメディアスレッドから呼ばれるため、UI を更新する場合は main thread へポストすること
        fun onFrameResolutionChanged(
            renderer: SurfaceViewRenderer,
            width: Int,
            height: Int,
        )
    }

    val workingRenderers = HashMap<String, SurfaceViewRenderer>()

    fun onAddRemoteStream(ms: MediaStream) {
        SoraLogger.d(TAG, "onAddRemoteStream:${ms.id}")

        if (ms.videoTracks.size != 1) {
            SoraLogger.w(TAG, "unsupported video track size")
            return
        }
        val renderer = createSurfaceViewRenderer()
        workingRenderers.put(ms.id, renderer)
        listener?.onAddRenderer(renderer)

        val track = ms.videoTracks[0]
        track.setEnabled(true)
        track.addSink(renderer)
    }

    fun onRemoveRemoteStream(msid: String) {
        SoraLogger.d(TAG, "onRemoveRemoteStream:$msid")

        if (workingRenderers.containsKey(msid)) {
            SoraLogger.d(TAG, "track for $msid found")

            val renderer = workingRenderers.get(msid)
            renderer?.let {
                listener?.onRemoveRenderer(it)
                it.release()
            }
            workingRenderers.remove(msid)
        }
    }

    fun createSurfaceViewRenderer(): SurfaceViewRenderer {
        val renderer = SurfaceViewRenderer(context)
        renderer.init(
            eglContext,
            object : RendererCommon.RendererEvents {
                override fun onFirstFrameRendered() {}

                override fun onFrameResolutionChanged(
                    videoWidth: Int,
                    videoHeight: Int,
                    rotation: Int,
                ) {
                    val size = VideoSize.fromRotation(videoWidth, videoHeight, rotation)
                    listener?.onFrameResolutionChanged(renderer, size.width, size.height)
                }
            },
        )
        return renderer
    }

    // workingRenderers の反復順で最初のストリームに対応するレンダラーを返す
    fun getFirstWorkingRenderer(): SurfaceViewRenderer? = workingRenderers.values.firstOrNull()

    fun dispose() {
        SoraLogger.d(TAG, "dispose")
        workingRenderers.values.forEach {
            listener?.onRemoveRenderer(it)
            it.release()
        }
        workingRenderers.clear()
        listener = null
    }
}
