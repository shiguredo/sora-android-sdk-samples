package jp.shiguredo.sora.sample.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.util.Log
import android.widget.EditText
import com.jaredrummler.materialspinner.MaterialSpinner
import jp.shiguredo.sora.sample.BuildConfig
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.ui.util.materialSpinner
import org.jetbrains.anko.*
import org.jetbrains.anko.design.textInputLayout
import org.jetbrains.anko.sdk21.listeners.onClick
import org.webrtc.PeerConnection

class VoiceChatRoomSetupActivity : AppCompatActivity() {

    val TAG = VoiceChatRoomSetupActivity::class.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setupUI()
    }

    private var channelNameInput:  EditText? = null
    private var audioCodecSpinner: MaterialSpinner? = null
    private var streamTypeSpinner: MaterialSpinner? = null
    private var sdpSemanticsSpinner: MaterialSpinner? = null

    val audioCodecOptions = listOf("OPUS", "PCMU")
    val streamTypeOptions = listOf("BIDIRECTIONAL", "SINGLE-UP", "SINGLE-DOWN", "MULTI-DOWN")
    val sdpSemanticsOptions = listOf("Plan B", "Unified Plan")

    private fun setupUI() {

        val spinnerBackgroundColor = "#f6f6f6"
        val spinnerWidth = 160

        verticalLayout {

            scrollView {

                lparams {
                    width = matchParent
                    height = matchParent
                }
                padding = dip(6)

                lparams {
                    width = matchParent
                    height = matchParent
                }

                backgroundResource = R.drawable.app_background

                textInputLayout {

                    padding = dip(10)

                    lparams {
                        width = matchParent
                        height = wrapContent
                        margin = dip(10)
                    }

                    backgroundColor = Color.WHITE

                    channelNameInput = editText {
                        hint = "Channel Name"
                        keyListener = DigitsKeyListener.getInstance(
                                "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
                        inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        setText(BuildConfig.CHANNEL_ID)
                    }

                    button("START") {
                        backgroundColor = Color.parseColor("#F06292")
                        textColor = Color.WHITE

                        onClick {
                            startVoiceChat()
                        }
                    }.lparams{

                        width = matchParent
                        height= wrapContent
                        margin = dip(10)
                    }

                    relativeLayout {

                        lparams{
                            width = matchParent
                            height= wrapContent
                            margin = dip(10)
                        }

                        backgroundColor = Color.parseColor(spinnerBackgroundColor)

                        textView {
                            maxLines = 10
                            text = "AUDIO CODEC"
                            padding = dip(10)
                            backgroundColor = Color.parseColor(spinnerBackgroundColor)
                        }.lparams {
                            width = wrapContent
                            height = wrapContent
                            margin = dip(10)
                            alignParentLeft()
                            centerVertically()
                        }

                        audioCodecSpinner = materialSpinner {
                            padding = dip(10)

                            lparams{
                                width = dip(spinnerWidth)
                                height= wrapContent
                                margin = dip(10)
                                alignParentRight()
                                centerVertically()
                            }
                        }

                        audioCodecSpinner?.setItems(audioCodecOptions)
                    }

                    relativeLayout {

                        lparams{
                            width = matchParent
                            height= wrapContent
                            margin = dip(10)
                        }

                        backgroundColor = Color.parseColor(spinnerBackgroundColor)

                        textView {
                            maxLines = 10
                            text = "STREAM TYPE"
                            padding = dip(10)
                            backgroundColor = Color.parseColor(spinnerBackgroundColor)
                        }.lparams {
                            width = wrapContent
                            height = wrapContent
                            margin = dip(10)
                            alignParentLeft()
                            centerVertically()
                        }

                        streamTypeSpinner = materialSpinner {
                            padding = dip(10)

                            lparams{
                                width = dip(spinnerWidth)
                                height= wrapContent
                                margin = dip(10)
                                alignParentRight()
                                centerVertically()
                            }
                        }

                        streamTypeSpinner?.setItems(streamTypeOptions)
                    }

                    relativeLayout {

                        lparams{
                            width = matchParent
                            height= wrapContent
                            margin = dip(10)
                        }

                        backgroundColor = Color.parseColor(spinnerBackgroundColor)

                        textView {
                            maxLines = 10
                            text = "sdpSemantics"
                            padding = dip(10)
                            backgroundColor = Color.parseColor(spinnerBackgroundColor)
                        }.lparams {
                            width = wrapContent
                            height = wrapContent
                            margin = dip(10)
                            alignParentLeft()
                            centerVertically()
                        }

                        sdpSemanticsSpinner = materialSpinner {
                            padding = dip(10)

                            lparams{
                                width = dip(spinnerWidth)
                                height= wrapContent
                                margin = dip(10)
                                alignParentRight()
                                centerVertically()
                            }
                        }

                        sdpSemanticsSpinner?.setItems(sdpSemanticsOptions)
                    }

                }
            }
        }
    }

    private fun startVoiceChat() {
        val channelName = channelNameInput!!.text.toString()
        if (channelName.isEmpty()) {
            showInputError()
            return
        }

        val streamType = streamTypeOptions[streamTypeSpinner!!.selectedIndex]
        val audioCodec = audioCodecOptions[audioCodecSpinner!!.selectedIndex]
        val sdpSemantics = sdpSemanticsOptions[sdpSemanticsSpinner!!.selectedIndex]

        val intent = Intent(this, VoiceChatRoomActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channelName)
        intent.putExtra("STREAM_TYPE", streamType)
        intent.putExtra("AUDIO_CODEC", audioCodec)
        intent.putExtra("SDP_SEMANTICS", sdpSemantics)

        startActivity(intent)
    }

    private fun showInputError() {
        Snackbar.make(this.contentView!!,
                "Channel Nameを適切に入力してください",
                Snackbar.LENGTH_LONG)
                .setAction("OK") { }
                .show()
    }

}


