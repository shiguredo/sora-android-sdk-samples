package jp.shiguredo.sora.sample.ui

import jp.shiguredo.sora.sdk2.Configuration

class SpotlightRoomActivity: SampleAppActivity() {

    override fun onConnectionConfiguration(configuration: Configuration) {
        configuration.spotlightEnabled = true
    }

}