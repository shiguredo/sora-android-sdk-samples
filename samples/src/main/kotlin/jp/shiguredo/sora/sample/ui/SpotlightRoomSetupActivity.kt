package jp.shiguredo.sora.sample.ui

import android.os.Bundle
import android.util.Log
import jp.shiguredo.sora.sample.R

class SpotlightRoomSetupActivity : SampleAppSetupActivity() {

    companion object {
        private val TAG = SpotlightRoomSetupActivity::class.simpleName
    }

    init {
        videoCodecOptions = listOf("VP8", "H264")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_spotlight_room_setup, SpotlightRoomActivity::class.java)
    }

}
