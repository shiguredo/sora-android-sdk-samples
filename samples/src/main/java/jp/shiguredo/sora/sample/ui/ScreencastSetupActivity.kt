package jp.shiguredo.sora.sample.ui

import android.annotation.TargetApi
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.util.Log
import android.widget.EditText
import com.jaredrummler.materialspinner.MaterialSpinner
import jp.shiguredo.sora.sample.BuildConfig
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sample.screencast.SoraScreencastService
import jp.shiguredo.sora.sample.screencast.SoraScreencastServiceStarter
import jp.shiguredo.sora.sample.ui.util.materialSpinner
import jp.shiguredo.sora.sdk.util.SoraServiceUtil
import org.jetbrains.anko.*
import org.jetbrains.anko.design.textInputLayout


@TargetApi(21)
class ScreencastSetupActivity : AppCompatActivity() {

    val TAG = ScreencastSetupActivity::class.simpleName

    private var screencastStarter: SoraScreencastServiceStarter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setupUI()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        screencastStarter?.onActivityResult(requestCode, resultCode, data)
    }

    internal fun startScreensast(channelId:   String,
                                 videoCodec:  String,
                                 audioCodec:  String,
                                 multistream: Boolean) {


        if (SoraServiceUtil.isRunning(this, "jp.shiguredo.sora.screencast.SoraScreencastService")) {
            Snackbar.make(this.contentView!!,
                    "既に起動中です",
                    Snackbar.LENGTH_LONG)
                    .setAction("OK") { }
                    .show()
            return
        }

        screencastStarter = SoraScreencastServiceStarter(
                activity          = this,
                signalingEndpoint = BuildConfig.SIGNALING_ENDPOINT,
                signalingMetadata = "",
                channelId         = channelId,
                videoCodec        = videoCodec,
                audioCodec        = audioCodec,
                videoScale        = 0.5f,
                videoFPS          = 30,
                multistream       = multistream,
                stateTitle        = "Sora Screencast",
                stateText         = "live on " + channelId,
                stateIcon         = R.drawable.icon,
                notificationIcon  = R.drawable.icon,
                boundActivityName = MainActivity::class.java.canonicalName,
                serviceClass      = SoraScreencastService::class
        )
        screencastStarter?.start()
        showNavigationMessage()
    }

    private fun showNavigationMessage() {
        AlertDialog.Builder(this)
                .setPositiveButton("OK") { _, _ ->  goToHome() }
                .setCancelable(false)
                .setMessage("スクリーンキャストを終了するときは上のナビゲーションバーから終了ボタンを押してください。")
                .show()
    }

    private fun goToHome() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private var channelNameInput:   EditText? = null
    private var videoCodecSpinner:  MaterialSpinner? = null
    private var audioCodecSpinner:  MaterialSpinner? = null
    private var multistreamSpinner: MaterialSpinner? = null

    val videoCodecOptions  = listOf("VP9", "VP8", "H264")
    val audioCodecOptions  = listOf("OPUS", "PCMU")
    val multistreamOptions = listOf("NO", "YES")

    private fun setupUI() {

        val spinnerBackgroundColor = "#f6f6f6"
        val spinnerWidth = 160

        verticalLayout {

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

                backgroundColor = Color.parseColor("#ffffff")

                channelNameInput = editText {
                    hint = "Channel Name"
                    keyListener = DigitsKeyListener.getInstance(
                            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
                    inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                }

                relativeLayout {

                    lparams {
                        width = matchParent
                        height = wrapContent
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
                        text = "VIDEO CODEC"
                    }

                    videoCodecSpinner = materialSpinner {

                        padding = dip(10)

                        lparams {
                            width = dip(spinnerWidth)
                            height = wrapContent
                            margin = dip(10)
                            alignParentRight()
                            centerVertically()
                        }
                    }

                    videoCodecSpinner?.setItems(videoCodecOptions)
                }

                relativeLayout {

                    lparams {
                        width = matchParent
                        height = wrapContent
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

                        maxLines = 10
                        text = "AUDIO CODEC"
                        padding = dip(10)
                        backgroundColor = Color.parseColor(spinnerBackgroundColor)
                    }

                    audioCodecSpinner = materialSpinner {
                        padding = dip(10)

                        lparams {
                            width = dip(spinnerWidth)
                            height = wrapContent
                            margin = dip(10)
                            alignParentRight()
                            centerVertically()
                        }
                    }

                    audioCodecSpinner?.setItems(audioCodecOptions)


                }

                relativeLayout {

                    lparams {
                        width = matchParent
                        height = wrapContent
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

                        maxLines = 10
                        text = "MULTISTREAM"
                        padding = dip(10)
                        backgroundColor = Color.parseColor(spinnerBackgroundColor)
                    }

                    multistreamSpinner = materialSpinner {
                        padding = dip(10)

                        lparams {
                            width = dip(spinnerWidth)
                            height = wrapContent
                            margin = dip(10)
                            alignParentRight()
                            centerVertically()
                        }
                    }

                    multistreamSpinner?.setItems(multistreamOptions)
                }

                button("START") {

                    lparams {

                        width = matchParent
                        height = wrapContent
                        margin = dip(10)
                    }

                    backgroundColor = Color.parseColor("#F06292")
                    textColor = Color.WHITE

                    onClick {
                        val channelName = channelNameInput!!.text.toString()
                        if (channelName.isEmpty()) {
                            showInputError()
                            return@onClick
                        }
                        val videoCodec = videoCodecOptions[videoCodecSpinner!!.selectedIndex]
                        val audioCodec = audioCodecOptions[audioCodecSpinner!!.selectedIndex]
                        val multistream = when (multistreamOptions[multistreamSpinner!!.selectedIndex]) {
                            "YES" -> true
                            "NO"  -> false
                            else  -> true
                        }
                        startScreensast(channelName, videoCodec, audioCodec, multistream)
                    }
                }

            }

        }
    }

    private fun showInputError() {
        Snackbar.make(this.contentView!!,
                "Channel Nameを適切に入力してください",
                Snackbar.LENGTH_LONG)
                .setAction("OK") { }
                .show()
    }

}