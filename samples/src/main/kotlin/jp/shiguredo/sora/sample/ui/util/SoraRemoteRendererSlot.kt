package jp.shiguredo.sora.sample.ui.util

import android.content.Context
import jp.shiguredo.sora.sdk.util.SoraLogger
import org.webrtc.AudioTrack
import org.webrtc.EglBase
import org.webrtc.MediaStream
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

// TODO support NUMBER_OF_RENDERERS limitation
class SoraRemoteRendererSlot(
    val context: Context,
    val eglContext: EglBase.Context,
    var listener: Listener?
) {
    companion object {
        private val TAG = SoraRemoteRendererSlot::class.simpleName
    }

    interface Listener {
        fun onAddRenderer(renderer: SurfaceViewRenderer)
        fun onRemoveRenderer(renderer: SurfaceViewRenderer)
    }

    val workingRenderers = HashMap<String, SurfaceViewRenderer>()
    val workingTracks = HashMap<String, VideoTrack>()
    // ここに Volume 持たせるのはこの class の目的外だと思うので、仮置きとしておく
    val workingVolumeTracks = HashMap<String, AudioTrack>()

    fun onAddRemoteStream(ms: MediaStream) {
        SoraLogger.d(TAG, "onAddRemoteStream:${ms.id}")

        if (ms.videoTracks.size != 1) {
            SoraLogger.w(TAG, "unsupported video track size")
            return
        }
        val renderer = createSurfaceViewRenderer()
        listener?.onAddRenderer(renderer)

        val track = ms.videoTracks[0]
        if (ms.audioTracks.size > 0) {
            val audioTrack = ms.audioTracks[0]
            workingVolumeTracks[ms.id] = audioTrack
        }
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
            workingVolumeTracks.remove(msid)
        }
    }

    fun createSurfaceViewRenderer(): SurfaceViewRenderer {
        val renderer = SurfaceViewRenderer(context)
        renderer.init(eglContext, null)
        return renderer
    }

    fun setVolume(msid: String, volume: Double) {
        if (workingVolumeTracks.containsKey(msid)) {
            workingVolumeTracks[msid]?.setVolume(volume)
        }
    }

    fun listMsids(): List<String> {
        return workingVolumeTracks.keys.toList()
    }

    fun dispose() {
        SoraLogger.d(TAG, "dispose")
        workingRenderers.values.forEach {
            listener?.onRemoveRenderer(it)
            it.release()
        }
        workingRenderers.clear()
        workingTracks.clear()
        workingVolumeTracks.clear()
        listener = null
    }
}
