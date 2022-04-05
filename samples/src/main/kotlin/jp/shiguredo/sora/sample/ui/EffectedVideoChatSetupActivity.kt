package jp.shiguredo.sora.sample.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.jaredrummler.materialspinner.MaterialSpinner
import jp.shiguredo.sora.sample.databinding.ActivityEffectedVideoChatSetupBinding

class EffectedVideoChatSetupActivity : AppCompatActivity() {

    companion object {
        val TAG = EffectedVideoChatSetupActivity::class.simpleName
    }

    private val videoEffectOptions = listOf(
        "グレースケール", "ピクセル化", "ポスタライズ", "トゥーン調",
        "ハーフトーン", "色調補正", "エンボス", "セピア調", "なし"
    )

    private lateinit var binding: ActivityEffectedVideoChatSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivityEffectedVideoChatSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.start.setOnClickListener { startVideoChat() }

        binding.videoEffectSelection.name.text = "映像エフェクト"
        binding.videoEffectSelection.spinner.setItems(videoEffectOptions)
    }

    private fun startVideoChat() {
        val channelName = binding.channelNameInput!!.text.toString()
        if (channelName.isEmpty()) {
            showInputError()
            return
        }

        val effect = selectedItem(binding.videoEffectSelection.spinner)

        val intent = Intent(this, EffectedVideoChatActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channelName)
        intent.putExtra("EFFECT", effect)

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
