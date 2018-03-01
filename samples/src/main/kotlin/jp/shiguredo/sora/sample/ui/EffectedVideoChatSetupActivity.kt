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
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.ui.util.materialSpinner
import org.jetbrains.anko.*
import org.jetbrains.anko.design.textInputLayout

class EffectedVideoChatSetupActivity : AppCompatActivity() {

    val TAG = EffectedVideoChatSetupActivity::class.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setupUI()
    }

    private var channelNameInput: EditText? = null
    private var effectSpinner:    MaterialSpinner? = null

    val effectOptions = listOf("GRAYSCALE", "PIXELATION", "POSTERIZE", "TOON", "HALFTONE", "HUE", "EMBOSS")

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
                    }

                    relativeLayout {

                        lparams{
                            width = matchParent
                            height= wrapContent
                            margin = dip(10)
                        }

                        backgroundColor = Color.parseColor(spinnerBackgroundColor)

                        textView {

                            lparams {
                                width = wrapContent
                                height = wrapContent
                                margin = dip(10)
                                alignParentLeft()
                                centerVertically()
                            }

                            padding = dip(10)
                            backgroundColor = Color.parseColor(spinnerBackgroundColor)
                            maxLines = 10
                            text = "VIDEO EFFECT"
                        }

                        effectSpinner = materialSpinner {

                            padding = dip(10)

                            lparams{
                                width = dip(spinnerWidth)
                                height= wrapContent
                                margin = dip(10)
                                alignParentRight()
                                centerVertically()
                            }
                        }

                        effectSpinner?.setItems(effectOptions)
                    }

                    button("START") {

                        lparams{

                            width = matchParent
                            height= wrapContent
                            margin = dip(10)
                        }

                        backgroundColor = Color.parseColor("#F06292")
                        textColor = Color.WHITE

                        onClick {
                            startVideoChat()
                        }
                    }

                }

            }

        }
    }

    private fun startVideoChat() {
        val channelName = channelNameInput!!.text.toString()
        if (channelName.isEmpty()) {
            showInputError()
            return
        }

        val effect = effectOptions[effectSpinner!!.selectedIndex]

        val intent = Intent(this, EffectedVideoChatActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channelName)
        intent.putExtra("EFFECT", effect)

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

