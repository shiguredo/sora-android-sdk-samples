package jp.shiguredo.sora.sample.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.shiguredo.sora.sample.R

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
                onClick = {},
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

@Composable
fun MessagingComposable() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Divider(
            color = Color.Gray,
            thickness = 1.dp,
        )
        MessageInput()
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
            .fillMaxHeight(0.1f)
            .padding(2.dp)
    ) {
        // TODO: フォーカスが当たった際に幅を広げたい
        OutlinedTextField(
            value = selectedLabel.value,
            onValueChange = { selectedLabel.value = it },
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .clickable { expanded.value = true },
                /* NOTE: 効かない
                .onFocusChanged {
                    expanded.value = it.hasFocus
                },
                 */
            label = { Text("ラベル") },
            trailingIcon = {
                Icon(
                    icon, "contentDescription",
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
                contentDescription = "Send"
            )
        }
    }
}

class MessagingSetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "メッセージング"

        val channel = super.getBaseContext().getString(R.string.channelId)

        setContent {
            // MessagingSetupComposable(channel)
            MessagingComposable()
        }
    }
}
