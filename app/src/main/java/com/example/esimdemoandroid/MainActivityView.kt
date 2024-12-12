package com.example.esimdemoandroid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.esimdemoandroid.ui.theme.ESIMDemoAndroidTheme

@Composable
fun MainView(onClick: () -> Unit) {
    Box(Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center) {
        Button(onClick) {
            Text("Install eSIM")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainViewPreview() {
    ESIMDemoAndroidTheme {
        MainView { }
    }
}