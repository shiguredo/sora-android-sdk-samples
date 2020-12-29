package jp.shiguredo.sora.sample.ui

import android.os.Bundle
import android.util.Log
import jp.shiguredo.sora.sample.R
import kotlinx.android.synthetic.main.activity_simulcast_setup.*
import kotlinx.android.synthetic.main.signaling_selection.view.*

class SimulcastSetupActivity : SampleAppSetupActivity() {

    companion object {
        val TAG = SimulcastSetupActivity::class.simpleName
    }

    init {
        videoCodecOptions = listOf("VP8", "H264")
        videoBitRateOptions = listOf("200", "500", "700", "1200", "2500", "4000", "5000", "10000", "15000", "20000", "30000")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_simulcast_setup, SimulcastActivity::class.java)
        videoBitRateSelection.spinner.selectedIndex = 6
        videoSizeSelection.spinner.selectedIndex = 6
    }

}
