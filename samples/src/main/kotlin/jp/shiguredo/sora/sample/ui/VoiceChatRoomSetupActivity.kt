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

    private val audioCodecOptions = listOf("OPUS")
    private val audioBitRateOptions = listOf("未指定", "8", "16", "24", "32",
            "64", "96", "128", "256")

    private val roleOptions = listOf("SENDRECV", "SENDONLY", "RECVONLY")
    private val multistreamOptions = listOf("有効", "無効")

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_voice_chat_room_setup)
        start.setOnClickListener { startVoiceChat() }
        audioCodecSelection.name.text = "音声コーデック"
        audioCodecSelection.spinner.setItems(audioCodecOptions)
        audioBitRateSelection.name.text = "音声ビットレート"
        audioBitRateSelection.spinner.setItems(audioBitRateOptions)
        roleSelection.name.text = "ロール"
        roleSelection.spinner.setItems(roleOptions)
        multistreamSelection.name.text = "マルチストリーム"
        multistreamSelection.spinner.setItems(multistreamOptions)
    }

    private fun startVoiceChat() {
        val channelName = channelNameInput.text.toString()
        if (channelName.isEmpty()) {
            showInputError()
            return
        }

        val role = selectedItem(roleSelection.spinner)
        val multistream = selectedItem(multistreamSelection.spinner)
        val audioCodec = selectedItem(audioCodecSelection.spinner)
        val audioBitRate = selectedItem(audioBitRateSelection.spinner)

        val intent = Intent(this, VoiceChatRoomActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channelName)
        intent.putExtra("ROLE", role)
        intent.putExtra("MULTISTREAM", multistream)
        intent.putExtra("AUDIO_CODEC", audioCodec)
        intent.putExtra("AUDIO_BIT_RATE", audioBitRate)

        startActivity(intent)
    }

    private fun selectedItem(spinner: MaterialSpinner): String {
        return spinner.getItems<String>()[spinner.selectedIndex]
    }

    private fun showInputError() {
        Snackbar.make(rootLayout,
                "チャネル名を適切に入力してください",
                Snackbar.LENGTH_LONG)
                .setAction("OK") { }
                .show()
    }

}
