package jp.shiguredo.sora.sample.stats

import jp.shiguredo.sora.sdk.util.SoraLogger
import org.webrtc.RTCStats
import org.webrtc.RTCStatsReport

class OutboundRtpVideoStatsLogger {
    companion object {
        val TAG = OutboundRtpVideoStatsLogger::class.simpleName

        private fun logBasicFields(prefix: String, stats: RTCStats) {
            val w = stats.members["frameWidth"]
            val h = stats.members["frameHeight"]
            val fps = stats.members["framesPerSecond"]

            val rid = stats.members["rid"]
            if (rid != null) {
                SoraLogger.i(TAG, "$prefix $rid")
            }
            SoraLogger.i(TAG, "$prefix ${w}x$h ${fps}fps")
            SoraLogger.i(TAG, "$prefix targetBitrate: ${stats.members["targetBitrate"]}")
        }

        private fun logQualityLimitationFields(prefix: String, stats: RTCStats) {
            val durations = stats.members["qualityLimitationDurations"]
            val resolutionChanges = stats.members["qualityLimitationResolutionChanges"]
            SoraLogger.i(TAG, "$prefix qualityLimitationDurations: $durations")
            SoraLogger.i(TAG, "$prefix qualityLimitationResolutionChanges: $resolutionChanges")
        }

        fun log(stats: RTCStatsReport) {
            stats.statsMap.filter { (_, stats) ->
                stats.type == "outbound-rtp" && stats.members["kind"] == "video"
            }.map { it.value }
                .forEach { stats ->
                    // SoraLogger.i(TAG, stats.toString())
                    val prefix = "[${stats.id}]"
                    logBasicFields(prefix, stats)
                    logQualityLimitationFields(prefix, stats)
                }
        }
    }
}
