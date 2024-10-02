package arne.jellyfindocumentsprovider.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun ServerSetting(id: Long = 0) {
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
    }

    if (loading) {
        Text("Loading...")
    }

    Column {
        Button(
            onClick = { /*TODO*/ },
        ) {
            Text("Connect")
        }
    }
}