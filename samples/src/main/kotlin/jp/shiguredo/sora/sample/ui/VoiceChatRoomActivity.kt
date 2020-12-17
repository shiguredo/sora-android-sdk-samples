package jp.shiguredo.sora.sample.ui

import jp.shiguredo.sora.sdk2.Configuration

class VoiceChatRoomActivity : SampleAppActivity() {

    companion object {
        private val TAG = VoiceChatRoomActivity::class.simpleName
    }

    override fun onConnectionConfiguration(configuration: Configuration) {
        configuration.videoEnabled = false
    }

}
