package jp.shiguredo.sora.sample.stats

import jp.shiguredo.sora.sdk.util.SoraLogger
import org.webrtc.RTCStats
import org.webrtc.RTCStatsReport

class VideoUpstreamLatencyStatsCollector {
    companion object {
        val TAG = VideoUpstreamLatencyStatsCollector::class.simpleName
    }

    private var prevReport: RTCStatsReport? = null

    fun newStatsReport(statsReport: RTCStatsReport) {
        // upstream video は一本しかないことを前提とする

        val outboundVideoStats = outboundRtpVideoStreamStats(statsReport)
        val prevOutboundVideoStats = outboundRtpVideoStreamStats(prevReport)

        val encodeMSec =
            averageMSec(
                "totalEncodeTime",
                "framesEncoded",
                prevOutboundVideoStats,
                outboundVideoStats,
            )
        encodeMSec?.let {
            SoraLogger.d(TAG, "RTCOutboundRtpVideoStream [totalEncodeTime/framesEncoded_in_ms]: %.3f".format(encodeMSec))
        }
        val sendDelayMSec =
            averageMSec(
                "totalPacketSendDelay",
                "packetsSent",
                prevOutboundVideoStats,
                outboundVideoStats,
            )
        sendDelayMSec?.let {
            SoraLogger.d(TAG, "RTCOutboundRtpVideoStream [totalPacketSendDelay/packetsSent_in_ms]: %.3f".format(sendDelayMSec))
        }

        val iceCandidatePairStats = nominatedIceCandidatePairStats(statsReport)
        val currentRoundTripTime = valueAsDouble("currentRoundTripTime", iceCandidatePairStats)
        currentRoundTripTime?.let {
            SoraLogger.d(TAG, "RTCIceCandidatePair currentRoundTripTime_in_ms: %.3f".format(currentRoundTripTime * 1000))
        }

        prevReport = statsReport
    }

    private fun outboundRtpVideoStreamStats(statsReport: RTCStatsReport?): RTCStats? =
        statsReport
            ?.statsMap
            ?.entries
            ?.firstOrNull {
                val stats = it.value
                val type = stats.type
                val kind = stats.members["kind"]
                type == "outbound-rtp" && kind == "video"
            }?.value

    private fun nominatedIceCandidatePairStats(statsReport: RTCStatsReport?): RTCStats? =
        statsReport
            ?.statsMap
            ?.entries
            ?.firstOrNull {
                val stats = it.value
                val type = stats.type
                val nominated = stats.members["nominated"]
                type == "candidate-pair" && nominated == true
            }?.value

    private fun valueAsDouble(
        key: String,
        stats: RTCStats?,
    ): Double? =
        when (val value = stats?.members?.get(key)) {
            is Number ->
                value.toDouble()
            else ->
                null
        }

    private fun difference(
        key: String,
        prevStats: RTCStats?,
        nowStats: RTCStats?,
    ): Double? {
        val prevValue = valueAsDouble(key, prevStats)
        val nowValue = valueAsDouble(key, nowStats)

        return if (prevValue == null || nowValue == null) {
            null
        } else {
            nowValue - prevValue
        }
    }

    private fun averageMSec(
        numeratorKey: String,
        denominatorKey: String,
        prevStats: RTCStats?,
        nowStats: RTCStats?,
    ): Double? {
        val numeratorDiff = difference(numeratorKey, prevStats, nowStats)
        val denominatorDiff = difference(denominatorKey, prevStats, nowStats)
        if (numeratorDiff == null || denominatorDiff == null) {
            return null
        }

        return if (denominatorDiff < 0.01) {
            0.0
        } else {
            // 分子は秒単位の統計項目である前提で msec に変換
            numeratorDiff / denominatorDiff * 1000
        }
    }
}
