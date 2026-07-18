package com.jacobsnarr.spoke.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.progress_indicator.CircularProgressIndicatorMMD
import com.mudita.mmd.components.text.TextMMD

@Composable
fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier =
        modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        TextMMD(text = text)
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier =
        modifier
            .fillMaxSize()
            .heightIn(min = 120.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicatorMMD()
    }
}
