package jp.shiguredo.sora.sample.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.databinding.ActivityVoiceChatRoomSetupBinding
import jp.shiguredo.sora.sample.option.SoraBitRateOptions

class VoiceChatRoomSetupActivity : AppCompatActivity() {
    companion object {
        val TAG = VoiceChatRoomSetupActivity::class.simpleName

        private const val STATE_PERMISSION_REQUEST_IN_FLIGHT = "PERMISSION_REQUEST_IN_FLIGHT"
    }

    private val audioCodecOptions = listOf("未指定", "OPUS")
    private val audioBitRateOptions = SoraBitRateOptions.audio

    private val roleOptions = listOf("SENDRECV", "SENDONLY", "RECVONLY")
    private val dataChannelSignalingOptions = listOf("未指定", "無効", "有効")
    private val ignoreDisconnectWebSocketOptions = listOf("未指定", "無効", "有効")

    private lateinit var binding: ActivityVoiceChatRoomSetupBinding

    private var permissionRequestInFlight = false

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permissionRequestInFlight = false
            if (!granted) {
                // 通話画面へ遷移した後も見えるように Toast で表示する
                Toast.makeText(this, R.string.background_call_notification_denied, Toast.LENGTH_LONG).show()
            }
            startVoiceChatInternal()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        permissionRequestInFlight =
            savedInstanceState?.getBoolean(STATE_PERMISSION_REQUEST_IN_FLIGHT) ?: false
        binding = ActivityVoiceChatRoomSetupBinding.inflate(layoutInflater)

        setContentView(binding.root)
        binding.start.setOnClickListener { startVoiceChat() }
        binding.audioCodecSelection.name.text = "音声コーデック"
        binding.audioBitRateSelection.name.text = "音声ビットレート"
        binding.roleSelection.name.text = "ロール"
        binding.dataChannelSignalingSelection.name.text = "データチャネル"
        binding.ignoreDisconnectWebSocketSelection.name.text = "WS 切断を無視"
        setupDropdowns(
            listOf(
                DropdownConfig(binding.audioCodecSelection.spinner, audioCodecOptions),
                DropdownConfig(binding.audioBitRateSelection.spinner, audioBitRateOptions),
                DropdownConfig(binding.roleSelection.spinner, roleOptions),
                DropdownConfig(binding.dataChannelSignalingSelection.spinner, dataChannelSignalingOptions),
                DropdownConfig(binding.ignoreDisconnectWebSocketSelection.spinner, ignoreDisconnectWebSocketOptions),
            ),
        )
    }

    override fun onResume() {
        super.onResume()
        // 権限ダイアログの結果待ちの間は二重開始を防ぐため、開始ボタンを有効に戻さない
        binding.start.isEnabled = !permissionRequestInFlight
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_PERMISSION_REQUEST_IN_FLIGHT, permissionRequestInFlight)
    }

    private fun startVoiceChat() {
        if (permissionRequestInFlight) {
            return
        }
        val channelName = binding.channelNameInput.text.toString()
        if (channelName.isEmpty()) {
            showInputError()
            return
        }

        binding.start.isEnabled = false
        if (needsNotificationPermission()) {
            permissionRequestInFlight = true
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        startVoiceChatInternal()
    }

    private fun startVoiceChatInternal() {
        val channelName = binding.channelNameInput.text.toString()
        val role = binding.roleSelection.spinner.selectedItem()
        val audioCodec = binding.audioCodecSelection.spinner.selectedItem()
        val audioBitRate = binding.audioBitRateSelection.spinner.selectedItem()
        val dataChannelSignaling = binding.dataChannelSignalingSelection.spinner.selectedItem()
        val ignoreDisconnectWebSocket = binding.ignoreDisconnectWebSocketSelection.spinner.selectedItem()
        val backgroundCall = binding.backgroundCallCheck.isChecked

        val intent = Intent(this, VoiceChatRoomActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channelName)
        intent.putExtra("ROLE", role)
        intent.putExtra("AUDIO_CODEC", audioCodec)
        intent.putExtra("AUDIO_BIT_RATE", audioBitRate)
        intent.putExtra("DATA_CHANNEL_SIGNALING", dataChannelSignaling)
        intent.putExtra("IGNORE_DISCONNECT_WEBSOCKET", ignoreDisconnectWebSocket)
        intent.putExtra(VoiceChatRoomActivity.EXTRA_BACKGROUND_CALL, backgroundCall)

        startActivity(intent)
    }

    private fun needsNotificationPermission(): Boolean {
        if (!binding.backgroundCallCheck.isChecked) {
            return false
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
    }

    private fun showInputError() {
        Snackbar
            .make(
                binding.rootLayout,
                "チャネル名を適切に入力してください",
                Snackbar.LENGTH_LONG,
            ).setAction("OK") { }
            .show()
    }
}
