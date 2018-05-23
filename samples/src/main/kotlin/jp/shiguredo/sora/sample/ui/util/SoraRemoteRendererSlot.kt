package jp.shiguredo.sora.sample.ui.util

import android.content.Context
import jp.shiguredo.sora.sdk.util.SoraLogger
import org.webrtc.*

// TODO support NUMBER_OF_RENDERERS limitation
class SoraRemoteRendererSlot(
        val context:    Context,
        val eglContext: EglBase.Context,
        var listener:   Listener?
) {
    val TAG = SoraRemoteRendererSlot::class.simpleName

    interface Listener {
        fun onAddRenderer(renderer: SurfaceViewRenderer)
        fun onRemoveRenderer(renderer: SurfaceViewRenderer)
    }

    val workingRenderers = HashMap<String, SurfaceViewRenderer>()
    val workingTracks    = HashMap<String, VideoTrack>()

    fun onAddRemoteStream(ms: MediaStream) {
        SoraLogger.d(TAG, "onAddRemoteStream:${ms.id}")

        if (ms.videoTracks.size != 1) {
            SoraLogger.w(TAG, "unsupported video track size")
            return;
        }
        val renderer = createSurfaceViewRenderer()
        listener?.onAddRenderer(renderer)

        val track = ms.videoTracks[0]
        workingRenderers.put(ms.id, renderer)
        workingTracks.put(ms.id, track)

        track.setEnabled(true)
        track.addSink(renderer)
    }

    fun onRemoveRemoteStream(label: String) {

        SoraLogger.d(TAG, "onRemoveRemoteStream:$label")

        if (workingRenderers.containsKey(label)) {

            SoraLogger.d(TAG, "track for $label found")

            val renderer = workingRenderers.get(label)
            renderer?.let {
                listener?.onRemoveRenderer(it)
                it.release()
            }
            workingRenderers.remove(label)
            workingTracks.remove(label)
        }
    }

    fun createSurfaceViewRenderer(): SurfaceViewRenderer {
        val renderer = SurfaceViewRenderer(context)
        renderer.init(eglContext, null)
        return renderer
    }

    fun dispose() {
        SoraLogger.d(TAG, "dispose")
        workingRenderers.values.forEach {
            listener?.onRemoveRenderer(it)
            it.release()
        }
        workingRenderers.clear()
        workingTracks.clear()
        listener = null
    }
}

