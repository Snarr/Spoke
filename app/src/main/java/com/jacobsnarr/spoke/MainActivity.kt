package com.jacobsnarr.spoke

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jacobsnarr.spoke.ui.navigation.SpokeNavHost
import com.mudita.mmd.ThemeMMD

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThemeMMD {
                SpokeNavHost()
            }
        }
    }
}
