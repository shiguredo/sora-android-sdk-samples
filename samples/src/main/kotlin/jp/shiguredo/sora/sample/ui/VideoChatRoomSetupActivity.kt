package jp.shiguredo.sora.sample.ui

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import android.util.Log
import jp.shiguredo.sora.sample.R
import kotlinx.android.synthetic.main.activity_video_chat_room_setup.*

class VideoChatRoomSetupActivity : SampleAppSetupActivity() {

    companion object {
        val TAG = VideoChatRoomSetupActivity::class.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_chat_room_setup)

        start.setOnClickListener { startVideoChat() }
    }

    private fun startVideoChat() {
        val intent = createIntent(VideoChatRoomActivity::class.java)
        if (intent != null) {
            startActivity(intent)
        }
    }

}
