package jp.shiguredo.sora.sample.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.jaredrummler.materialspinner.MaterialSpinner
import jp.shiguredo.sora.sample.databinding.ActivityVoiceChatRoomSetupBinding

class VoiceChatRoomSetupActivity : AppCompatActivity() {

    companion object {
        val TAG = VoiceChatRoomSetupActivity::class.simpleName
    }

    private val audioCodecOptions = listOf("未指定", "OPUS")
    private val audioBitRateOptions = listOf(
        "未指定", "8", "16", "24", "32",
        "64", "96", "128", "256"
    )

    private val roleOptions = listOf("SENDRECV", "SENDONLY", "RECVONLY")
    private val multistreamOptions = listOf("有効", "無効")
    private val dataChannelSignalingOptions = listOf("未指定", "無効", "有効")
    private val ignoreDisconnectWebSocketOptions = listOf("未指定", "無効", "有効")

    private lateinit var binding: ActivityVoiceChatRoomSetupBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceChatRoomSetupBinding.inflate(layoutInflater)

        setContentView(binding.root)
        binding.start.setOnClickListener { startVoiceChat() }
        binding.audioCodecSelection.name.text = "音声コーデック"
        binding.audioCodecSelection.spinner.setItems(audioCodecOptions)
        binding.audioBitRateSelection.name.text = "音声ビットレート"
        binding.audioBitRateSelection.spinner.setItems(audioBitRateOptions)
        binding.roleSelection.name.text = "ロール"
        binding.roleSelection.spinner.setItems(roleOptions)
        binding.multistreamSelection.name.text = "マルチストリーム"
        binding.multistreamSelection.spinner.setItems(multistreamOptions)
        binding.dataChannelSignalingSelection.name.text = "データチャネル"
        binding.dataChannelSignalingSelection.spinner.setItems(dataChannelSignalingOptions)
        binding.ignoreDisconnectWebSocketSelection.name.text = "WS 切断を無視"
        binding.ignoreDisconnectWebSocketSelection.spinner.setItems(ignoreDisconnectWebSocketOptions)
    }

    private fun startVoiceChat() {
        val channelName = binding.channelNameInput.text.toString()
        if (channelName.isEmpty()) {
            showInputError()
            return
        }

        val role = selectedItem(binding.roleSelection.spinner)
        val multistream = selectedItem(binding.multistreamSelection.spinner)
        val audioCodec = selectedItem(binding.audioCodecSelection.spinner)
        val audioBitRate = selectedItem(binding.audioBitRateSelection.spinner)
        val dataChannelSignaling = selectedItem(binding.dataChannelSignalingSelection.spinner)
        val ignoreDisconnectWebSocket = selectedItem(binding.ignoreDisconnectWebSocketSelection.spinner)

        val intent = Intent(this, VoiceChatRoomActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channelName)
        intent.putExtra("ROLE", role)
        intent.putExtra("MULTISTREAM", multistream)
        intent.putExtra("AUDIO_CODEC", audioCodec)
        intent.putExtra("AUDIO_BIT_RATE", audioBitRate)
        intent.putExtra("DATA_CHANNEL_SIGNALING", dataChannelSignaling)
        intent.putExtra("IGNORE_DISCONNECT_WEBSOCKET", ignoreDisconnectWebSocket)

        startActivity(intent)
    }

    private fun selectedItem(spinner: MaterialSpinner): String {
        return spinner.getItems<String>()[spinner.selectedIndex]
    }

    private fun showInputError() {
        Snackbar.make(
            binding.rootLayout,
            "チャネル名を適切に入力してください",
            Snackbar.LENGTH_LONG
        )
            .setAction("OK") { }
            .show()
    }
}
