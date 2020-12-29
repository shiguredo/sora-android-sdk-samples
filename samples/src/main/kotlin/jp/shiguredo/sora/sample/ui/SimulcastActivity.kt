package jp.shiguredo.sora.sample.ui

import android.os.Bundle
import android.util.Log
import jp.shiguredo.sora.sample.BuildConfig
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sdk.channel.signaling.message.SimulcastRid
import jp.shiguredo.sora.sdk2.Configuration
import kotlinx.android.synthetic.main.activity_simulcast.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class SimulcastActivity: SampleAppActivity() {

    companion object {
        private val TAG = SimulcastActivity::class.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        r0Button.setOnClickListener { changeRid(SimulcastRid.R0) }
        r1Button.setOnClickListener { changeRid(SimulcastRid.R1) }
        r2Button.setOnClickListener { changeRid(SimulcastRid.R2) }
    }

    override fun onConnectionConfiguration(configuration: Configuration) {
        configuration.simulcastEnabled = true
    }

    override fun createUI(): VideoChatActivityUI {
        return VideoChatActivityUI(
                activity = this,
                layout = R.layout.activity_simulcast,
                channelName = channelName,
                resources = resources,
                videoViewWidth = 100,
                videoViewHeight = 100,
                videoViewMargin = 10,
                density = this.resources.displayMetrics.density)
    }

    private fun changeRid(quality: SimulcastRid) {
        if (mediaChannel?.connectionId == null) {
            Log.d(TAG, "cannot change quality: connection ID is not found")
            return
        }

       GlobalScope.launch {
            withContext(Dispatchers.IO) {
                val host = URI(BuildConfig.SIGNALING_ENDPOINT).host
                val url = URL("http://$host:3000")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("x-sora-target", "Sora_20180820.ChangeSimulcastQuality")
                conn.doOutput = true
                conn.connect()
                val buffer = BufferedOutputStream(conn.outputStream)
                buffer.write("{\n".toByteArray())
                buffer.write("    \"channel_id\": \"$channelName\",\n".toByteArray())
                buffer.write("    \"connection_id\": \"${mediaChannel!!.connectionId!!}\",\n".toByteArray())
                buffer.write("    \"quality\": \"$quality\"\n".toByteArray())
                buffer.write("}".toByteArray())
                buffer.flush()
                buffer.close()
                conn.outputStream.close()

                val status = conn.responseCode
                Log.d(TAG, "change quality: response $status")
            }
        }
    }
}