package jp.shiguredo.sora.sample.ui

import android.content.Intent
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.jaredrummler.materialspinner.MaterialSpinner
import jp.shiguredo.sora.sample.R
import kotlinx.android.synthetic.main.activity_voice_chat_room_setup.*
import kotlinx.android.synthetic.main.signaling_selection.view.*

class VoiceChatRoomSetupActivity : AppCompatActivity() {

    companion object {
        val TAG = VoiceChatRoomSetupActivity::class.simpleName
    }

    private val audioCodecOptions = listOf("OPUS", "PCMU")
    private val streamTypeOptions = listOf("BIDIRECTIONAL", "SINGLE-UP", "SINGLE-DOWN", "MULTI-DOWN")
    private val sdpSemanticsOptions = listOf("Unified Plan", "Plan B")

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_voice_chat_room_setup)
        start.setOnClickListener { startVoiceChat() }
        audioCodecSelection.name.text = "AUDIO CODEC"
        audioCodecSelection.spinner.setItems(audioCodecOptions)
        streamTypeSelection.name.text = "STREAM TYPE"
        streamTypeSelection.spinner.setItems(streamTypeOptions)
        sdpSemanticsSelection.name.text = "SDP SEMANTICS"
        sdpSemanticsSelection.spinner.setItems(sdpSemanticsOptions)
    }

    private fun startVoiceChat() {
        val channelName = channelNameInput.text.toString()
        if (channelName.isEmpty()) {
            showInputError()
            return
        }

        val streamType = selectedItem(streamTypeSelection.spinner)
        val audioCodec = selectedItem(audioCodecSelection.spinner)
        val sdpSemantics = selectedItem(sdpSemanticsSelection.spinner)

        val intent = Intent(this, VoiceChatRoomActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channelName)
        intent.putExtra("STREAM_TYPE", streamType)
        intent.putExtra("AUDIO_CODEC", audioCodec)
        intent.putExtra("SDP_SEMANTICS", sdpSemantics)

        startActivity(intent)
    }

    private fun selectedItem(spinner: MaterialSpinner): String {
        return spinner.getItems<String>()[spinner.selectedIndex]
    }

    private fun showInputError() {
        Snackbar.make(rootLayout,
                "Channel Nameを適切に入力してください",
                Snackbar.LENGTH_LONG)
                .setAction("OK") { }
                .show()
    }

}
