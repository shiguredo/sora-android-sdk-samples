package jp.shiguredo.sora.sample.ui

import android.content.Context
import android.os.Bundle
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
import androidx.compose.foundation.lazy.items
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
import jp.shiguredo.sora.sdk.util.ByteBufferBackedInputStream
import jp.shiguredo.sora.sdk.util.SoraLogger
import java.nio.ByteBuffer

val COLOR_PRIMARY_BUTTON = android.graphics.Color.parseColor("#F06292")
val COLOR_SETUP_BACKGROUND = android.graphics.Color.parseColor("#2288dd")
const val DEFAULT_DATA_CHANNELS = """
[
    {
        "label": "#spam",
        "direction": "sendrecv"
    },
    {
        "label": "#egg",
        "direction": "sendrecv",
        "compress": true
    }
]
"""

@Composable
fun SetupComposable(defaultChannel: String, setConnected: (Boolean) -> Unit, labels: SnapshotStateList<String>, messages: SnapshotStateList<Message>) {
    var channel = remember { mutableStateOf(defaultChannel) }
    var (isLoading, setIsLoading) = remember { mutableStateOf(false) }

    var dataChannels = remember {
        mutableStateOf(DEFAULT_DATA_CHANNELS.trim())
    }

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
                        override fun onConnect(mediaChannel: SoraMediaChannel) {
                            super.onConnect(mediaChannel)

                            SoraLogger.d(MessagingActivity.TAG, "connected")
                            setConnected(true)
                        }

                        override fun onDataChannelMessage(label: String, data: ByteBuffer) {
                            if (!label.startsWith("#")) {
                                return
                            }

                            // TODO: バイナリのメッセージに対応する
                            val message = ByteBufferBackedInputStream(data).reader().readText()
                            messages.add(Message(label, message, false))
                        }
                    }

                    val t = object : TypeToken<Collection<Map<String, Any>>>() {}.type
                    val connectDataChannels = Gson().fromJson<List<Map<String, Any>>>(dataChannels.value, t)

                    labels.addAll(connectDataChannels.map { it["label"] as String })
                    MessagingActivity.channel!!.connect(c, channel.value, connectDataChannels, channelListener)
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
                value = dataChannels.value,
                onValueChange = {
                    dataChannels.value = it
                },
                label = { Text("データチャンネル", color = Color.Gray) },
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
fun TimelineComposable(setConnected: (Boolean) -> Unit, labels: SnapshotStateList<String>, messages: SnapshotStateList<Message>) {

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
        MessageInput(labels, messages)
    }
}

@Composable
fun MessageInput(labels: SnapshotStateList<String>, messages: SnapshotStateList<Message>) {
    val (message, setMessage) = remember { mutableStateOf("") }
    val (expanded, setExpanded) = remember { mutableStateOf(false) }

    val (selectedLabel, setSelectedLabel) = remember {
        mutableStateOf(if (labels.isNotEmpty()) { labels.first() } else { "" })
    }

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
                MessagingActivity.channel.sendMessage(selectedLabel, message)
                messages.add(Message(selectedLabel, message, true))
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
    private val TAG = MessagingActivity::class.simpleName

    fun connect(context: Context, channelId: String, dataChannels: List<Map<String, Any>>, listener: SoraMediaChannel.Listener) {

        // Sora に接続する
        val signalingEndpointCandidates = BuildConfig.SIGNALING_ENDPOINT.split(",").map { it.trim() }
        val signalingMetadata = Gson().fromJson(BuildConfig.SIGNALING_METADATA, Map::class.java)

        val mediaOption = SoraMediaOption()
        mediaOption.enableMultistream()
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

    fun sendMessage(label: String, message: String) {
        if (mediaChannel == null) {
            SoraLogger.e(TAG, "mediaChannel is not available")
        }
        mediaChannel?.sendDataChannelMessage(label, message)
    }

    fun disconnect() {
        mediaChannel?.disconnect()
    }
}

data class Message(val label: String, val message: String, val self: Boolean)

@Composable
private fun TopComposable() {
    val channelId = LocalContext.current.getString(R.string.channelId)
    val (connected, setConnected) = remember { mutableStateOf(false) }

    var labels = remember { mutableStateListOf<String>() }
    var messages = remember { mutableStateListOf<Message>() }

    if (connected) {
        TimelineComposable(setConnected, labels, messages)
    } else {
        SetupComposable(channelId, setConnected, labels, messages)
    }
}

class MessagingActivity : AppCompatActivity() {
    companion object {
        var channel = SoraMessagingChannel()
        val TAG = MessagingActivity::class.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "メッセージング"

        setContent {
            TopComposable()
        }
    }
}
