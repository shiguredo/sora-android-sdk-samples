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
    private val audioBitRateOptions = listOf("UNDEFINED", "8", "16", "24", "32",
            "64", "96", "128", "256")

    private val streamTypeOptions = listOf("BIDIRECTIONAL", "SINGLE-UP", "SINGLE-DOWN", "MULTI-DOWN")

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_voice_chat_room_setup)
        start.setOnClickListener { startVoiceChat() }
        audioCodecSelection.name.text = "AUDIO CODEC"
        audioCodecSelection.spinner.setItems(audioCodecOptions)
        audioBitRateSelection.name.text = "AUDIO BIT RATE"
        audioBitRateSelection.spinner.setItems(audioBitRateOptions)
        streamTypeSelection.name.text = "STREAM TYPE"
        streamTypeSelection.spinner.setItems(streamTypeOptions)
    }

    private fun startVoiceChat() {
        val channelName = channelNameInput.text.toString()
        if (channelName.isEmpty()) {
            showInputError()
            return
        }

        val streamType = selectedItem(streamTypeSelection.spinner)
        val audioCodec = selectedItem(audioCodecSelection.spinner)
        val audioBitRate = selectedItem(audioBitRateSelection.spinner)

        val intent = Intent(this, VoiceChatRoomActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channelName)
        intent.putExtra("STREAM_TYPE", streamType)
        intent.putExtra("AUDIO_CODEC", audioCodec)
        intent.putExtra("AUDIO_BIT_RATE", audioBitRate)

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
