package jp.shiguredo.sora.sample.ui

import android.os.Bundle
import android.util.Log
import jp.shiguredo.sora.sample.R

class VoiceChatRoomSetupActivity : SampleAppSetupActivity() {

    companion object {
        val TAG = VoiceChatRoomSetupActivity::class.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        Log.d(TAG, "set content view")
        setContentView(R.layout.activity_voice_chat_room_setup, VoiceChatRoomActivity::class.java)
    }

}
