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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
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
import jp.shiguredo.sora.sdk.channel.option.SoraMediaOption
import jp.shiguredo.sora.sdk.error.SoraErrorReason
import jp.shiguredo.sora.sdk.error.SoraMessagingError
import jp.shiguredo.sora.sdk.util.SoraLogger
import kotlinx.coroutines.launch
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Base64

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

@Composable
fun SetupComposable(
    defaultChannel: String,
    setConnected: (Boolean) -> Unit,
    labels: SnapshotStateList<String>,
    messages: SnapshotStateList<Message>,
    listState: LazyListState
) {
    val channel = remember { mutableStateOf(defaultChannel) }
    val (isLoading, setIsLoading) = remember { mutableStateOf(false) }
    val textDataChannels = remember {
        mutableStateOf(DEFAULT_DATA_CHANNELS.trim())
    }
    val coroutineScope = rememberCoroutineScope()

    val c = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(COLOR_SETUP_BACKGROUND)) // TODO: 要グラデーション?
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
                value = channel.value,
                onValueChange = {
                    channel.value = it
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

                        override fun onClose(mediaChannel: SoraMediaChannel) {
                            super.onClose(mediaChannel)
                            setConnected(false)
                            setIsLoading(false)
                        }

                        override fun onDataChannel(mediaChannel: SoraMediaChannel, dataChannels: List<Map<String, Any>>?) {
                            super.onDataChannel(mediaChannel, dataChannels)
                            // DataChannel の接続をもって接続したとみなす
                            setConnected(true)
                            SoraLogger.d(SoraMessagingChannel.TAG, "data_channels=$dataChannels")
                            dataChannels?.map { it["label"] as String }?.let {
                                labels.addAll(it)
                            }
                        }

                        override fun onDataChannelMessage(mediaChannel: SoraMediaChannel, label: String, data: ByteBuffer) {
                            val newIndex = messages.size + 1
                            var message: String? = SoraMessagingChannel.dataToString(data.duplicate())

                            if (message == null) {
                                try {
                                    val buffer = Base64.getEncoder().encode(data)
                                    message = SoraMessagingChannel.dataToString(buffer)
                                } catch (e: Exception) {
                                    SoraLogger.d(SoraMessagingChannel.TAG, e.stackTraceToString())
                                }
                            }
                            messages.add(Message(label, message ?: "", false))
                            coroutineScope.launch {
                                // TODO: 効いてない気がする
                                listState.scrollToItem(newIndex)
                            }
                        }

                        override fun onError(mediaChannel: SoraMediaChannel, reason: SoraErrorReason) {
                            super.onError(mediaChannel, reason)
                            SoraLogger.e(SoraMessagingChannel.TAG, "SoraErrorReason: reason=${reason.name}")
                            setConnected(false)
                            setIsLoading(false)

                            val handler = Handler(Looper.getMainLooper())
                            handler.post {
                                run() {
                                    Toast.makeText(c, reason.toString(), Toast.LENGTH_LONG).show()
                                }
                            }
                        }

                        override fun onError(mediaChannel: SoraMediaChannel, reason: SoraErrorReason, message: String) {
                            super.onError(mediaChannel, reason, message)
                            SoraLogger.e(SoraMessagingChannel.TAG, "SoraErrorReason: reason=${reason.name}, message=$message")
                            setConnected(false)
                            setIsLoading(false)

                            val handler = Handler(Looper.getMainLooper())
                            handler.post {
                                run() {
                                    Toast.makeText(c, "$reason: $message", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }

                    class DataChannel {
                        lateinit var label: String
                        lateinit var direction: String
                        var compress: Boolean? = null
                        var max_packet_life_time: Int? = null
                        var max_retransmits: Int? = null
                        var protocol: String? = null
                        var ordered: Boolean? = null

                        fun getMap(): Map<String, Any> {
                            var map = mutableMapOf<String, Any>()
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
                    MessagingActivity.channel.connect(c, channel.value, connectDataChannels, channelListener)
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

class TriangleShape(private val size: Int, private val reversed: Boolean = true) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val x = if (reversed) {
            size.width
        } else {
            0f
        }

        val path = Path().apply {
            moveTo(x = x, y = size.height - this@TriangleShape.size)
            lineTo(x = x, y = size.height)
            lineTo(
                x = if (reversed) {
                    x - this@TriangleShape.size
                } else {
                    x + this@TriangleShape.size
                },
                y = size.height
            )
        }
        return Outline.Generic(path = path)
    }
}

@Composable
fun BalloonTailComposable(color: Color, reversed: Boolean) {
    Column(
        modifier = Modifier
            .background(
                color = color,
                shape = TriangleShape(20, reversed = reversed)
            )
            .width(16.dp)
            .fillMaxHeight()
    ) {}
}

@Composable
fun MessageComposable(label: String, message: String, self: Boolean = true) {
    val backgroundColor = if (self) {
        Color.Green
    } else {
        Color.LightGray
    }

    Row(
        Modifier
            .height(IntrinsicSize.Max)
            .fillMaxWidth(),
        horizontalArrangement = if (self) { Arrangement.End } else { Arrangement.Start }
    ) {
        if (!self) {
            BalloonTailComposable(color = backgroundColor, reversed = !self)
        }
        Column(
            modifier = Modifier
                .background(
                    color = backgroundColor,
                    shape = if (self) {
                        RoundedCornerShape(4.dp, 4.dp, 0.dp, 4.dp)
                    } else {
                        RoundedCornerShape(4.dp, 4.dp, 4.dp, 0.dp)
                    }
                )
                .padding(8.dp)
        ) {
            Text(
                "$label: $message",
                Modifier.widthIn(0.dp, 300.dp) // TODO: View のサイズを取得して調整する
            )
        }
        if (self) {
            BalloonTailComposable(color = backgroundColor, reversed = !self)
        }
    }
}

@Composable
fun TimelineComposable(
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
        MessagingActivity.channel.disconnect()
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
                    MessageComposable(message.label, message.message, message.self)
                }
            }
        }
        Divider(
            color = Color.Gray,
            thickness = 1.dp,
        )
        MessageInput(labels, messages, listState)
    }
}

@Composable
fun MessageInput(
    labels: SnapshotStateList<String>,
    messages: SnapshotStateList<Message>,
    listState: LazyListState
) {
    val (message, setMessage) = remember { mutableStateOf("") }
    val (expanded, setExpanded) = remember { mutableStateOf(false) }
    val (selectedLabel, setSelectedLabel) = remember {
        mutableStateOf(if (labels.isNotEmpty()) { labels.first() } else { "" })
    }
    val coroutineScope = rememberCoroutineScope()
    val c = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxHeight()
            .padding(2.dp)
    ) {
        // TODO: フォーカスが当たった際に幅を広げる?
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
            labels.forEachIndexed { index, label ->
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
            value = message,
            onValueChange = { setMessage(it) },
            label = { Text("メッセージ") },
            modifier = Modifier.fillMaxWidth(0.7f),
        )

        OutlinedButton(
            onClick = {
                val newIndex = messages.size + 1

                val error = MessagingActivity.channel.sendMessage(selectedLabel, message)
                // NOTE: メッセージを送信する代わりに、ランダムなバイト列を送信する例
                // val bytes = ByteArray(20)
                // Random.nextBytes(bytes)
                // val error = MessagingActivity.channel.sendMessage(selectedLabel, ByteBuffer.wrap(bytes)
                SoraLogger.d(MessagingActivity.TAG, "sendMessage: error=$error")

                if (error == SoraMessagingError.OK) {
                    messages.add(Message(selectedLabel, message, true))
                    // NOTE: バイト列を文字列に変換して送信済みメッセージに追加する例
                    // messages.add(Message(selectedLabel, Base64.encodeToString(bytes, Base64.DEFAULT), true))
                } else {
                    val msg = error?.toString() ?: "SoraMediaChannel is unavailable"
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        run() {
                            Toast.makeText(c, error.toString(), Toast.LENGTH_LONG).show()
                        }
                    }
                }

                if (labels.isNotEmpty()) {
                    setSelectedLabel(labels.first())
                }

                coroutineScope.launch {
                    listState.scrollToItem(newIndex)
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
        val TAG = MessagingActivity::class.simpleName
        private val utf8Decoder = StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)

        @Synchronized
        fun dataToString(data: ByteBuffer): String? {
            val s: String?
            try {
                val buffer = utf8Decoder.decode(data)
                s = buffer.toString()
            } catch (e: CharacterCodingException) {
                return null
            }
            return s
        }
    }

    fun connect(context: Context, channelId: String, dataChannels: List<Map<String, Any>>, listener: SoraMediaChannel.Listener) {

        // Sora に接続する
        val signalingEndpointCandidates = BuildConfig.SIGNALING_ENDPOINT.split(",").map { it.trim() }
        val signalingMetadata = Gson().fromJson(BuildConfig.SIGNALING_METADATA, Map::class.java)

        val mediaOption = SoraMediaOption()
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
            dataChannels = dataChannels
        )
        mediaChannel!!.connect()
    }

    fun sendMessage(label: String, message: String): SoraMessagingError? {
        return mediaChannel?.sendDataChannelMessage(label, message)
    }

    fun sendMessage(label: String, message: ByteBuffer): SoraMessagingError? {
        return mediaChannel?.sendDataChannelMessage(label, message)
    }

    fun disconnect() {
        mediaChannel?.disconnect()
    }
}

data class Message(val label: String, val message: String, val self: Boolean)

@Composable
fun TopComposable() {
    val channelId = LocalContext.current.getString(R.string.channelId)
    val (connected, setConnected) = remember { mutableStateOf(false) }

    // メッセージ追加時に、リストをスクロールするために使用
    val listState = rememberLazyListState()

    val labels = remember { mutableStateListOf<String>() }
    val messages = remember { mutableStateListOf<Message>() }

    if (connected) {
        TimelineComposable(setConnected, labels, messages, listState)
    } else {
        SetupComposable(channelId, setConnected, labels, messages, listState)
    }
}

class MessagingActivity : AppCompatActivity() {
    companion object {
        var channel = SoraMessagingChannel()
        val TAG = MessagingActivity::class.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        title = "メッセージング"

        setContent {
            TopComposable()
        }
    }
}
