package com.jacobsnarr.spoke.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.jacobsnarr.spoke.SpokeApp
import com.jacobsnarr.spoke.di.AppContainer

@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current
    return (context.applicationContext as SpokeApp).container
}
