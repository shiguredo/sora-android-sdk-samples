package jp.shiguredo.sora.sample.ui.util

import android.content.Context
import jp.shiguredo.sora.sdk.util.SoraLogger
import org.webrtc.EglBase
import org.webrtc.MediaStream
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

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
            workingTracks.remove(msid)
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

