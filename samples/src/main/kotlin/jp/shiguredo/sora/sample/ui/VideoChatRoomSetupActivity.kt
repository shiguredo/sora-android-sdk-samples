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
import kotlinx.android.synthetic.main.activity_video_chat_room_setup.*
import kotlinx.android.synthetic.main.signaling_selection.view.*

class VideoChatRoomSetupActivity : AppCompatActivity() {

    companion object {
        val TAG = VideoChatRoomSetupActivity::class.simpleName
    }

    private val videoCodecOptions = listOf("VP9", "VP8", "H264")
    private val audioCodecOptions = listOf("OPUS", "PCMU")
    private val audioEnabledOptions = listOf("YES", "NO")
    private val streamTypeOptions = listOf("BIDIRECTIONAL", "SINGLE-UP", "SINGLE-DOWN", "MULTI-DOWN")
    private val bitRateOptions = listOf("UNDEFINED", "100", "300", "500", "800", "1000", "1500",
            "2000", "2500", "3000", "5000")
    private val sizeOptions = listOf(
            // Portrait
            "VGA", "QQVGA", "QCIF", "HQVGA", "QVGA", "HD", "FHD",
            "Res1920x3840", "UHD2160x3840", "UHD2160x4096",
            // Landscape
            "Res3840x1920")
    private val fpsOptions = listOf("30", "10", "15", "20", "24", "60")
    private val sdpSemanticsOptions = listOf("Unified Plan", "Plan B")

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_chat_room_setup)

        videoCodecSelection.name.text = "VIDEO CODEC"
        videoCodecSelection.spinner.setItems(videoCodecOptions)
    }

    private var channelNameInput:    EditText? = null
    private var videoCodecSpinner:   MaterialSpinner? = null
    private var audioCodecSpinner:   MaterialSpinner? = null
    private var streamTypeSpinner:   MaterialSpinner? = null
    private var bitRateSpinner:      MaterialSpinner? = null
    private var audioEnabledSpinner: MaterialSpinner? = null
    private var sizeSpinner:         MaterialSpinner? = null
    private var fpsSpinner:          MaterialSpinner? = null
    private var sdpSemanticsSpinner: MaterialSpinner? = null


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
                            startVideoChat()
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
                            padding = dip(10)
                            backgroundColor = Color.parseColor(spinnerBackgroundColor)
                            maxLines = 10
                            text = "VIDEO CODEC"
                        }.lparams {
                            width = wrapContent
                            height = wrapContent
                            margin = dip(10)
                            alignParentLeft()
                            centerVertically()
                        }

                        videoCodecSpinner = materialSpinner {

                            padding = dip(10)

                            lparams{
                                width = dip(spinnerWidth)
                                height= wrapContent
                                margin = dip(10)
                                alignParentRight()
                                centerVertically()
                            }
                        }

                        videoCodecSpinner?.setItems(videoCodecOptions)
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
                            text = "AUDIO ENABLED"
                            padding = dip(10)
                            backgroundColor = Color.parseColor(spinnerBackgroundColor)
                        }.lparams {
                            width = wrapContent
                            height = wrapContent
                            margin = dip(10)
                            alignParentLeft()
                            centerVertically()
                        }

                        audioEnabledSpinner = materialSpinner {
                            padding = dip(10)

                            lparams{
                                width = dip(spinnerWidth)
                                height= wrapContent
                                margin = dip(10)
                                alignParentRight()
                                centerVertically()
                            }
                        }

                        audioEnabledSpinner?.setItems(audioEnabledOptions)
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
                            text = "BITRATE"
                            padding = dip(10)
                            backgroundColor = Color.parseColor(spinnerBackgroundColor)
                        }.lparams {
                            width = wrapContent
                            height = wrapContent
                            margin = dip(10)
                            alignParentLeft()
                            centerVertically()
                        }

                        bitRateSpinner = materialSpinner {
                            padding = dip(10)

                            lparams{
                                width = dip(spinnerWidth)
                                height= wrapContent
                                margin = dip(10)
                                alignParentRight()
                                centerVertically()
                            }
                        }

                        bitRateSpinner?.setItems(bitRateOptions)
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
                            text = "VIDEO SIZE"
                            padding = dip(10)
                            backgroundColor = Color.parseColor(spinnerBackgroundColor)
                        }.lparams {
                            width = wrapContent
                            height = wrapContent
                            margin = dip(10)
                            alignParentLeft()
                            centerVertically()
                        }

                        sizeSpinner = materialSpinner {
                            padding = dip(10)

                            lparams{
                                width = dip(spinnerWidth)
                                height= wrapContent
                                margin = dip(10)
                                alignParentRight()
                                centerVertically()
                            }
                        }

                        sizeSpinner?.setItems(sizeOptions)
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
                            text = "FPS"
                            padding = dip(10)
                            backgroundColor = Color.parseColor(spinnerBackgroundColor)
                        }.lparams {
                            width = wrapContent
                            height = wrapContent
                            margin = dip(10)
                            alignParentLeft()
                            centerVertically()
                        }

                        fpsSpinner = materialSpinner {
                            padding = dip(10)

                            lparams{
                                width = dip(spinnerWidth)
                                height= wrapContent
                                margin = dip(10)
                                alignParentRight()
                                centerVertically()
                            }
                        }

                        fpsSpinner?.setItems(fpsOptions)
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

    private fun startVideoChat() {
        val channelName = channelNameInput!!.text.toString()
        if (channelName.isEmpty()) {
            showInputError()
            return
        }

        val streamType = streamTypeOptions[streamTypeSpinner!!.selectedIndex]
        val videoCodec = videoCodecOptions[videoCodecSpinner!!.selectedIndex]
        val audioCodec = audioCodecOptions[audioCodecSpinner!!.selectedIndex]
        val audioEnabled = audioEnabledOptions[audioEnabledSpinner!!.selectedIndex]
        val bitRate = bitRateOptions[bitRateSpinner!!.selectedIndex]
        val size = sizeOptions[sizeSpinner!!.selectedIndex]
        val fps = fpsOptions[fpsSpinner!!.selectedIndex]
        val sdpSemantics = sdpSemanticsOptions[sdpSemanticsSpinner!!.selectedIndex]

        val intent = Intent(this, VideoChatRoomActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channelName)
        intent.putExtra("STREAM_TYPE", streamType)
        intent.putExtra("VIDEO_CODEC", videoCodec)
        intent.putExtra("AUDIO_CODEC", audioCodec)
        intent.putExtra("AUDIO_ENABLED", audioEnabled)
        intent.putExtra("BITRATE", bitRate)
        intent.putExtra("VIDEO_SIZE", size)
        intent.putExtra("FPS", fps)
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

