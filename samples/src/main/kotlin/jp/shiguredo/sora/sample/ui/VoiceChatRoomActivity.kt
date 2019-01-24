package jp.shiguredo.sora.sample.ui

import android.annotation.TargetApi
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import jp.shiguredo.sora.sample.BuildConfig
import jp.shiguredo.sora.sample.facade.SoraAudioChannel
import jp.shiguredo.sora.sample.option.SoraStreamType
import jp.shiguredo.sora.sdk.channel.data.ChannelAttendeesCount
import jp.shiguredo.sora.sdk.channel.option.SoraAudioOption
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk21.listeners.onClick
import org.webrtc.PeerConnection

class VoiceChatRoomActivity : AppCompatActivity() {

    val TAG = VoiceChatRoomActivity::class.simpleName

    private var channelName = ""
    private var ui: VoiceChatRoomActivityUI? = null

    private var audioCodec:  SoraAudioOption.Codec = SoraAudioOption.Codec.OPUS
    private var streamType   = SoraStreamType.BIDIRECTIONAL
    private var sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setupWindow()

        ui = VoiceChatRoomActivityUI()
        ui?.setContentView(this)

        channelName = intent.getStringExtra("CHANNEL_NAME")

        audioCodec = SoraAudioOption.Codec.valueOf(
                intent.getStringExtra("AUDIO_CODEC"))

        when (intent.getStringExtra("STREAM_TYPE")) {
            "BIDIRECTIONAL" -> { streamType = SoraStreamType.BIDIRECTIONAL }
            "SINGLE-UP"     -> { streamType = SoraStreamType.SINGLE_UP     }
            "SINGLE-DOWN"   -> { streamType = SoraStreamType.SINGLE_DOWN   }
            "MULTI-DOWN"    -> { streamType = SoraStreamType.MULTI_DOWN    }
            else            -> { streamType = SoraStreamType.BIDIRECTIONAL }
        }

        when (intent.getStringExtra("SDP_SEMANTICS")) {
            "Unified Plan" -> { sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN }
            "Plan B"       -> { sdpSemantics = PeerConnection.SdpSemantics.PLAN_B }
            else           -> { sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN }
        }

        ui?.setChannelName(channelName)

        connectChannel()
    }

    private fun setupWindow() {
        supportActionBar?.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
            or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setWindowVisibility()
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun setWindowVisibility() {
        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    override fun onResume() {
        super.onResume()
        this.volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        close()
    }

    internal fun close() {
        Log.d(TAG, "close")
        disposeChannel()
        finish()
    }

    private var channel: SoraAudioChannel? = null
    private var channelListener: SoraAudioChannel.Listener = object : SoraAudioChannel.Listener {

        override fun onConnect(channel: SoraAudioChannel) {
            ui?.changeStateText("CONNECTED")
        }

        override fun onClose(channel: SoraAudioChannel) {
            ui?.changeStateText("CLOSED")
            close()
        }

        override fun onError(channel: SoraAudioChannel, reason: SoraErrorReason) {
            ui?.changeStateText("ERROR")
            close()
        }

        override fun onAttendeesCountUpdated(channel: SoraAudioChannel, attendees: ChannelAttendeesCount) {
            Log.d(TAG, "onAttendeesCountUpdated")
        }

    }

    private fun connectChannel() {
        Log.d(TAG, "connectChannel")

        channel = SoraAudioChannel(
                context           = this,
                signalingEndpoint = BuildConfig.SIGNALING_ENDPOINT,
                channelId         = channelName,
                signalingMetadata = "",
                codec             = audioCodec,
                sdpSemantics      = sdpSemantics,
                streamType        = streamType,
                listener          = channelListener
        )
        channel!!.connect()
    }

    private fun disconnectChannel() {
        Log.d(TAG, "disconnectChannel")
        channel?.disconnect()
    }

    private fun disposeChannel() {
        channel?.dispose()
    }
}

class VoiceChatRoomActivityUI : AnkoComponent<VoiceChatRoomActivity> {

    private var channelText: TextView? = null
    private var stateText:   TextView? = null

    internal fun changeStateText(msg: String) {
        stateText?.text = msg
    }

    internal fun setChannelName(name: String) {
        channelText?.text = name
    }

    override fun createView(ui: AnkoContext<VoiceChatRoomActivity>): View = with(ui) {

        return verticalLayout {

            lparams {
                width  = matchParent
                height = matchParent
            }

            backgroundColor = Color.BLACK

            padding = dip(20)

            verticalLayout {

                lparams {
                    width  = matchParent
                    weight = 1f
                }

                channelText = textView {
                    backgroundColor = Color.parseColor("#333333")

                    this.gravity = Gravity.CENTER
                    text = "Channel"
                    textColor = Color.WHITE
                    textSize = 20f
                    padding = dip(10)
                }.lparams {
                    width  = matchParent
                    height = wrapContent
                }

                stateText = textView {
                    this.gravity = Gravity.CENTER
                    text = "CONNECTING..."
                    textColor = Color.WHITE
                    textSize = 14f
                    padding = dip(10)
                }.lparams {
                    width = matchParent
                    height = wrapContent
                    setMargins(0, 10, 0, 10)
                }

            }

            button("CLOSE") {
                backgroundColor = Color.RED
                textColor = Color.WHITE

                onClick {
                    ui.owner.close()
                }
            }.lparams {
                width = matchParent
                height = wrapContent
            }

        }
    }

}