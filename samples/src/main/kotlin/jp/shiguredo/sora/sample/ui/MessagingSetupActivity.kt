package jp.shiguredo.sora.sample.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.shiguredo.sora.sample.R

@Composable
fun MessagingSetupComposable(defaultChannel: String) {
    val backgroundColor = android.graphics.Color.parseColor("#2288dd")
    val connectButtonColor = android.graphics.Color.parseColor("#F06292")

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
                modifier = Modifier.fillMaxWidth(0.95f).height(40.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(connectButtonColor)),
                shape = RoundedCornerShape(0)
            ) {
                Text("接続する", color = Color.White)
            }
            TextField(
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

class MessagingSetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "メッセージング"

        val channel = super.getBaseContext().getString(R.string.channelId)

        setContent {
            MessagingSetupComposable(channel)
        }
    }
}
