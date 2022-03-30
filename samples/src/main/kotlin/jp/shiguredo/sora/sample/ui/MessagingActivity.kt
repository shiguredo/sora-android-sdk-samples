package jp.shiguredo.sora.sample.ui

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import jp.shiguredo.sora.sample.BuildConfig
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sdk.channel.SoraMediaChannel
import jp.shiguredo.sora.sdk.channel.option.SoraChannelRole
import jp.shiguredo.sora.sdk.channel.option.SoraMediaOption
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import jp.shiguredo.sora.sdk.error.SoraMessagingError
import jp.shiguredo.sora.sdk.util.SoraLogger
import java.lang.Exception
import java.nio.ByteBuffer
import kotlin.random.Random

// ランダムなバイト列を送信する (テスト用)
const val SEND_RANDOM_BINARY = false

val COLOR_PRIMARY_BUTTON = android.graphics.Color.parseColor("#F06292")
val COLOR_SETUP_BACKGROUND = android.graphics.Color.parseColor("#2288dd")
const val DEFAULT_DATA_CHANNELS = """
[
    {
        "label": "#spam",
        "direction": "sendrecv",
        "max_packet_life_time": 600
    },
    {
        "label": "#egg",
        "direction": "sendrecv",
        "compress": true,
        "max_retransmits": 3,
        "ordered": true,
        "protocol": "abc"
    }
]
"""

enum class MessageType {
    SENT,
    RECEIVED,
}

@Composable
fun SetupComposable(
    channel: SoraMessagingChannel,
    defaultChannel: String,
    setConnected: (Boolean) -> Unit,
    labels: SnapshotStateList<String>,
    messages: SnapshotStateList<Message>
) {
    val textChannel = remember { mutableStateOf(defaultChannel) }
    val (isLoading, setIsLoading) = remember { mutableStateOf(false) }
    val textDataChannels = remember {
        mutableStateOf(DEFAULT_DATA_CHANNELS.trim())
    }
    val c = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(COLOR_SETUP_BACKGROUND))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = textChannel.value,
                onValueChange = {
                    textChannel.value = it
                },
                label = { Text("チャネル名", color = Color.Gray) },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color.Black,
                    unfocusedIndicatorColor = Color.Black
                ),
                textStyle = TextStyle(fontSize = 20.sp)
            )
            Button(
                onClick = {
                    val channelListener = object : SoraMediaChannel.Listener {

                        override fun onConnect(mediaChannel: SoraMediaChannel) {
                            super.onConnect(mediaChannel)
                            SoraLogger.d(
                                SoraMessagingChannel.TAG,
                                "onConnect: contactSignalingEndpoint=${mediaChannel.contactSignalingEndpoint}," +
                                    "connectedSignalingEndpoint=${mediaChannel.connectedSignalingEndpoint}"
                            )
                        }

                        override fun onClose(mediaChannel: SoraMediaChannel) {
                            super.onClose(mediaChannel)
                            SoraLogger.d(SoraMessagingChannel.TAG, "onClose")
                            setConnected(false)
                            setIsLoading(false)
                        }

                        override fun onDataChannel(mediaChannel: SoraMediaChannel, dataChannels: List<Map<String, Any>>?) {
                            super.onDataChannel(mediaChannel, dataChannels)
                            SoraLogger.d(SoraMessagingChannel.TAG, "onDataChannel: data_channels=$dataChannels")
                            // DataChannel の接続をもって接続したとみなす
                            setConnected(true)
                            dataChannels?.map { it["label"] as String }?.let {
                                labels.addAll(it)
                            }
                        }

                        override fun onDataChannelMessage(mediaChannel: SoraMediaChannel, label: String, data: ByteBuffer) {
                            SoraLogger.d(SoraMessagingChannel.TAG, "onDataChannelMessage: label=$label received_data_in_bytes=${data.remaining()}")
                            // 受信した data を UTF-8 の文字列に変換する
                            var message: String? = channel.dataToString(data)
                            if (message == null) {
                                // バイナリ形式のメッセージを受信した場合など、 UTF-8 への変換が失敗する場合、
                                // 10進数の数値のリストに data を変換する
                                //
                                // ランダムなバイト列を受信する場合、受信した data が偶然 UTF-8 な文字列になる可能性もあるが、
                                // ここでは考慮しない
                                // 実際のアプリケーションでは、ラベル毎に受信するメッセージの種類 (文字列 or バイナリ) を選択することが想定されるため、
                                // このようなフォールバックを実装する必要はないと思われる
                                data.rewind()
                                val bytes = ByteArray(data.remaining())
                                data.get(bytes)

                                // UByte は experimental なので一旦使用を控える
                                // message = bytes.toUByteArray().contentToString()
                                message = bytes.map { it.toInt() and 0xff }.toTypedArray().contentToString()
                            }

                            message.let {
                                messages.add(Message(label, it, MessageType.RECEIVED))
                            }
                        }

                        override fun onError(mediaChannel: SoraMediaChannel, reason: SoraErrorReason) {
                            super.onError(mediaChannel, reason)
                            SoraLogger.e(SoraMessagingChannel.TAG, "onError: reason=${reason.name}")
                            setConnected(false)
                            setIsLoading(false)

                            val handler = Handler(Looper.getMainLooper())
                            handler.post {
                                run {
                                    Toast.makeText(c, reason.toString(), Toast.LENGTH_LONG).show()
                                }
                            }
                        }

                        override fun onError(mediaChannel: SoraMediaChannel, reason: SoraErrorReason, message: String) {
                            super.onError(mediaChannel, reason, message)
                            SoraLogger.e(SoraMessagingChannel.TAG, "onError: reason=${reason.name}, message=$message")
                            setConnected(false)
                            setIsLoading(false)

                            val handler = Handler(Looper.getMainLooper())
                            handler.post {
                                run {
                                    Toast.makeText(c, "$reason: $message", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }

                    /**
                     * サンプル・アプリで、文字列の data_channels を List<Map<String, Any>> に変換して利用する仕様にしたところ、
                     * Gson の仕様に基づいて、 max_retransmits など Int 型の値が Float に変換された
                     * この問題を防ぐために、サンプル・アプリでは DataChannel クラスを独自に定義している
                     *
                     * 上記のような文字列からの変換処理を行わない限り、 DataChannel クラスを独自に定義する必要はない
                     */
                    class DataChannel {
                        lateinit var label: String
                        lateinit var direction: String
                        var compress: Boolean? = null
                        var max_packet_life_time: Int? = null
                        var max_retransmits: Int? = null
                        var protocol: String? = null
                        var ordered: Boolean? = null

                        fun getMap(): Map<String, Any> {
                            val map = mutableMapOf<String, Any>()
                            val fields = this.javaClass.declaredFields
                            for (field in fields) {
                                val k = field.name
                                val v = field.get(this)
                                if (v != null) {
                                    map[k] = v
                                }
                            }

                            return map
                        }
                    }
                    val t = object : TypeToken<Collection<DataChannel>>() {}.type
                    val data = Gson().fromJson<List<DataChannel>>(textDataChannels.value, t)
                    val connectDataChannels = data.map { it.getMap() }

                    SoraLogger.d(MessagingActivity.TAG, "data_channels=$connectDataChannels")
                    channel.connect(c, textChannel.value, connectDataChannels, channelListener)
                    setIsLoading(true)
                },
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(COLOR_PRIMARY_BUTTON)),
                shape = RoundedCornerShape(0)
            ) {
                Text("接続する", color = Color.White)
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = textDataChannels.value,
                onValueChange = {
                    textDataChannels.value = it
                },
                label = { Text("データチャネル", color = Color.Gray) },
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = Color.Black,
                    unfocusedIndicatorColor = Color.Black
                )
            )
            if (isLoading) {
                Dialog(
                    onDismissRequest = {},
                    DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color.White, shape = RoundedCornerShape(8.dp))
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

class BalloonTailTriangleShape(private val size: Int, private val type: MessageType) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val x = if (type == MessageType.RECEIVED) {
            size.width
        } else {
            0f
        }

        val path = Path().apply {
            moveTo(x = x, y = size.height - this@BalloonTailTriangleShape.size)
            lineTo(x = x, y = size.height)
            lineTo(
                x = if (type == MessageType.RECEIVED) {
                    x - this@BalloonTailTriangleShape.size
                } else {
                    x + this@BalloonTailTriangleShape.size
                },
                y = size.height
            )
        }
        return Outline.Generic(path = path)
    }
}

@Composable
fun BalloonTailComposable(color: Color, type: MessageType) {
    Column(
        modifier = Modifier
            .background(
                color = color,
                shape = BalloonTailTriangleShape(20, type)
            )
            .width(16.dp)
            .fillMaxHeight()
    ) {}
}

@Composable
fun MessageComposable(label: String, message: String, type: MessageType = MessageType.SENT) {
    val backgroundColor = if (type == MessageType.SENT) {
        Color.Green
    } else {
        Color.LightGray
    }

    val screenWidth = LocalConfiguration.current.screenWidthDp
    Row(
        Modifier
            .height(IntrinsicSize.Max)
            .fillMaxWidth(),
        horizontalArrangement = if (type == MessageType.SENT) { Arrangement.End } else { Arrangement.Start }
    ) {
        if (type == MessageType.RECEIVED) {
            BalloonTailComposable(color = backgroundColor, type = MessageType.RECEIVED)
        }
        Column(
            modifier = Modifier
                .background(
                    color = backgroundColor,
                    shape = if (type == MessageType.SENT) {
                        RoundedCornerShape(4.dp, 4.dp, 0.dp, 4.dp)
                    } else {
                        RoundedCornerShape(4.dp, 4.dp, 4.dp, 0.dp)
                    }
                )
                .padding(8.dp)
        ) {
            Text(
                "$label: $message",
                Modifier.widthIn(0.dp, (screenWidth * 0.7).dp)
            )
        }
        if (type == MessageType.SENT) {
            BalloonTailComposable(color = backgroundColor, type = MessageType.SENT)
        }
    }
}

@Composable
fun TimelineComposable(
    channel: SoraMessagingChannel,
    setConnected: (Boolean) -> Unit,
    labels: SnapshotStateList<String>,
    messages: SnapshotStateList<Message>,
    listState: LazyListState
) {
    BackHandler {
        // 設定画面に戻る
        labels.clear()
        messages.clear()
        setConnected(false)
        channel.disconnect()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(0.9f),
            verticalArrangement = Arrangement.Bottom,
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                items(messages) { message ->
                    MessageComposable(message.label, message.message, message.type)
                }
            }
        }
        Divider(
            color = Color.Gray,
            thickness = 1.dp,
        )
        MessageInput(channel, labels, messages)
    }

    // 新しく追加されたメッセージが表示されるように自動でスクロールする
    LaunchedEffect(messages.size) {
        listState.scrollToItem(messages.size)
    }
}

@Composable
fun MessageInput(
    channel: SoraMessagingChannel,
    labels: SnapshotStateList<String>,
    messages: SnapshotStateList<Message>
) {
    val (textMessage, setTextMessage) = remember { mutableStateOf("") }
    val (expanded, setExpanded) = remember { mutableStateOf(false) }
    val (selectedLabel, setSelectedLabel) = remember {
        mutableStateOf(if (labels.isNotEmpty()) { labels.first() } else { "" })
    }
    val c = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxHeight()
            .padding(2.dp)
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = { setSelectedLabel(it) },
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .clickable { setExpanded(true) },
            label = { Text("ラベル") },
            trailingIcon = {
                Icon(
                    if (expanded) { Icons.Filled.ArrowDropUp } else { Icons.Filled.ArrowDropDown },
                    "拡大",
                    Modifier.clickable { setExpanded(!expanded) }
                )
            },
            singleLine = true,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { setExpanded(false) }
        ) {
            labels.forEach { label ->
                DropdownMenuItem(
                    onClick = {
                        setSelectedLabel(label)
                        setExpanded(false)
                    }
                ) {
                    Text(label)
                }
            }
        }

        OutlinedTextField(
            value = textMessage,
            onValueChange = { setTextMessage(it) },
            label = { Text("メッセージ") },
            modifier = Modifier.fillMaxWidth(0.7f),
        )

        OutlinedButton(
            onClick = {
                try {
                    var message = textMessage
                    var error: SoraMessagingError? = null
                    if (!SEND_RANDOM_BINARY) {
                        error = channel.sendMessage(selectedLabel, message)
                    } else {
                        val bytes = ByteArray(20)
                        Random.nextBytes(bytes)
                        error = channel.sendMessage(selectedLabel, ByteBuffer.wrap(bytes))

                        // UByte は experimental なので一旦使用を控える
                        // message = bytes.toUByteArray().contentToString()
                        message = bytes.map { it.toInt() and 0xff }.toTypedArray().contentToString()
                    }

                    if (error == SoraMessagingError.OK) {
                        messages.add(Message(selectedLabel, message, MessageType.SENT))
                    } else {
                        val handler = Handler(Looper.getMainLooper())
                        handler.post {
                            run {
                                Toast.makeText(c, error.toString(), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    SoraLogger.e(MessagingActivity.TAG, "failed to send message", e)
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        run {
                            Toast.makeText(c, e.toString(), Toast.LENGTH_LONG).show()
                        }
                    }
                }

                if (labels.isNotEmpty()) {
                    setSelectedLabel(labels.first())
                }
            },
            modifier = Modifier.size(50.dp),
            shape = CircleShape,
            border = BorderStroke(2.dp, Color(COLOR_PRIMARY_BUTTON)),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(COLOR_PRIMARY_BUTTON))
        ) {
            Icon(
                Icons.Filled.Send,
                contentDescription = "送信"
            )
        }
    }
}

class SoraMessagingChannel {
    private var mediaChannel: SoraMediaChannel? = null

    companion object {
        val TAG = SoraMessagingChannel::class.simpleName
    }

    fun connect(context: Context, channelId: String, dataChannels: List<Map<String, Any>>, listener: SoraMediaChannel.Listener) {

        // Sora に接続する
        val signalingEndpointCandidates = BuildConfig.SIGNALING_ENDPOINT.split(",").map { it.trim() }
        val signalingMetadata = Gson().fromJson(BuildConfig.SIGNALING_METADATA, Map::class.java)

        val mediaOption = SoraMediaOption()
        mediaOption.role = SoraChannelRole.SENDRECV
        mediaOption.enableMultistream()
        // Sora 側で data_channel_messaging_only = true の場合、 enableVideoDownstream は不要
        mediaOption.enableVideoDownstream(null)

        mediaChannel = SoraMediaChannel(
            context = context,
            signalingEndpointCandidates = signalingEndpointCandidates,
            channelId = channelId,
            signalingMetadata = signalingMetadata,
            mediaOption = mediaOption,
            listener = listener,
            dataChannelSignaling = true,
            dataChannels = dataChannels,
        )
        mediaChannel!!.connect()
    }

    fun sendMessage(label: String, message: String): SoraMessagingError {
        val error = mediaChannel!!.sendDataChannelMessage(label, message)
        SoraLogger.d(TAG, "sendMessage: error=$error")
        return error
    }

    fun sendMessage(label: String, message: ByteBuffer): SoraMessagingError {
        val error = mediaChannel!!.sendDataChannelMessage(label, message)
        SoraLogger.d(TAG, "sendMessage: error=$error")
        return error
    }

    fun disconnect() {
        mediaChannel?.disconnect()
    }

    fun dataToString(data: ByteBuffer): String? {
        return try {
            mediaChannel!!.dataToString(data)
        } catch (e: CharacterCodingException) {
            null
        }
    }
}

data class Message(val label: String, val message: String, val type: MessageType)

@Composable
fun TopComposable(channel: SoraMessagingChannel) {
    val defaultChannelId = LocalContext.current.getString(R.string.channelId)
    val (connected, setConnected) = remember { mutableStateOf(false) }

    // メッセージ追加時に、リストをスクロールするために使用
    val listState = rememberLazyListState()

    val labels = remember { mutableStateListOf<String>() }
    val messages = remember { mutableStateListOf<Message>() }

    if (connected) {
        TimelineComposable(channel, setConnected, labels, messages, listState)
    } else {
        SetupComposable(channel, defaultChannelId, setConnected, labels, messages)
    }
}

class MessagingActivity : AppCompatActivity() {
    companion object {
        val TAG = MessagingActivity::class.simpleName
    }

    var channel = SoraMessagingChannel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        title = "メッセージング"

        setContent {
            TopComposable(channel)
        }
    }
}
