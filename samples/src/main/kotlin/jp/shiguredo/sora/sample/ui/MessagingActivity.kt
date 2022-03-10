package jp.shiguredo.sora.sample.ui

import android.os.Bundle
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import jp.shiguredo.sora.sample.BuildConfig
import jp.shiguredo.sora.sample.R
import jp.shiguredo.sora.sdk.channel.SoraMediaChannel
import jp.shiguredo.sora.sdk.channel.option.SoraMediaOption
import jp.shiguredo.sora.sdk.util.SoraLogger
import java.nio.ByteBuffer

val connectButtonColor = android.graphics.Color.parseColor("#F06292")

@Composable
fun MessagingSetupComposable(defaultChannel: String) {
    val backgroundColor = android.graphics.Color.parseColor("#2288dd")

    var channel = remember { mutableStateOf(defaultChannel) }
    var dataChannels = remember {
        mutableStateOf(
            """
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
""".trim()
        )
    }

    val c = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(backgroundColor)) // TODO: 要グラデーション?
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
                    // Sora に接続する
                    val signalingEndpointCandidates = BuildConfig.SIGNALING_ENDPOINT.split(",").map { it.trim() }
                    val signalingMetadata = Gson().fromJson(BuildConfig.SIGNALING_METADATA, Map::class.java)

                    val channelListener = object : SoraMediaChannel.Listener {
                        override fun onConnect(mediaChannel: SoraMediaChannel) {
                            super.onConnect(mediaChannel)
                            SoraLogger.d(MessagingActivity.TAG, "connected")
                        }

                        override fun onDataChannelMessage(label: String, data: ByteBuffer) {
                            SoraLogger.d(MessagingActivity.TAG, "received")
                        }
                    }

                    val mediaOption = SoraMediaOption()
                    mediaOption.enableMultistream()
                    mediaOption.enableVideoDownstream(null)


                    val t = object : TypeToken<Collection<Map<String, Any>>>() {}.type
                    var connectDataChannels = Gson().fromJson<List<Map<String, Any>>>(dataChannels.value, t)

                    MessagingActivity.mediaChannel = SoraMediaChannel(
                        context = c,
                        signalingEndpointCandidates = signalingEndpointCandidates,
                        channelId = channel.value,
                        signalingMetadata = signalingMetadata,
                        mediaOption = mediaOption,
                        listener = channelListener,
                        dataChannelSignaling = true,
                        dataChannels = connectDataChannels
                    )
                    MessagingActivity.mediaChannel!!.connect()
                },
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(connectButtonColor)),
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
fun MessagingComposable() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        TimelineComposable()
        Divider(
            color = Color.Gray,
            thickness = 1.dp,
        )
        MessageInput()
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

data class Message(val label: String, val message: String, val self: Boolean)

@Composable
fun TimelineComposable() {
    val messages = listOf(
        Message("#spam", "ham", true),
        Message("#spam", "祇園精舎の鐘の声、諸行無常の響きあり。沙羅双樹の花の色、盛者必衰の理をあらはす。奢れる人も久からず、ただ春の夜の夢のごとし。猛き者も遂にはほろびぬ、偏ひとへに風の前の塵におなじ。", true),
        Message("#spam", "egg", false),
        Message("#spam", "祇園精舎の鐘の声、諸行無常の響きあり。沙羅双樹の花の色、盛者必衰の理をあらはす。奢れる人も久からず、ただ春の夜の夢のごとし。猛き者も遂にはほろびぬ、偏ひとへに風の前の塵におなじ。", false),
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(8.dp)
    ) {

        items(messages) { message ->
            MessageComposable(message.label, message.message, message.self)
        }
    }
}

@Composable
fun MessageInput() {
    val message = remember { mutableStateOf("") }
    val expanded = remember { mutableStateOf(false) }
    val icon = if (expanded.value)
        Icons.Filled.ArrowDropUp
    else
        Icons.Filled.ArrowDropDown
    val labels = listOf("#spam", "#egg", "#少し長めのラベル")
    val selectedLabel = remember { mutableStateOf(labels.first()) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxHeight(0.16f)
            .padding(2.dp)
    ) {
        // TODO: フォーカスが当たった際に幅を広げたい
        OutlinedTextField(
            value = selectedLabel.value,
            onValueChange = { selectedLabel.value = it },
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .clickable { expanded.value = true },
            label = { Text("ラベル") },
            trailingIcon = {
                Icon(
                    icon, "拡大",
                    Modifier.clickable { expanded.value = !expanded.value }
                )
            },
            singleLine = true,
        )
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }
        ) {
            labels.forEachIndexed { index, label ->
                DropdownMenuItem(
                    onClick = {
                        selectedLabel.value = label
                        expanded.value = false
                    }
                ) {
                    Text(label)
                }
            }
        }

        OutlinedTextField(
            value = message.value,
            onValueChange = { message.value = it },
            label = { Text("メッセージ") },
            modifier = Modifier.fillMaxWidth(0.7f),
        )

        OutlinedButton(
            onClick = { },
            modifier = Modifier.size(50.dp),
            shape = CircleShape,
            border = BorderStroke(2.dp, Color(connectButtonColor)),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(connectButtonColor))
        ) {
            Icon(
                Icons.Filled.Send,
                contentDescription = "送信"
            )
        }
    }
}

/*
class MessagingChannel {
    private var mediaChannel: SoraMediaChannel? = null

    fun connect() {

    }
}
 */

class MessagingActivity : AppCompatActivity() {
    companion object {
        var mediaChannel: SoraMediaChannel? = null
        val TAG = MessagingActivity::class.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "メッセージング"

        val context = super.getBaseContext()
        val channel = context.getString(R.string.channelId)
        val connected = false

        setContent {
            if (connected) {
                MessagingComposable()
            } else {
                MessagingSetupComposable(channel)
            }
        }
    }
}
