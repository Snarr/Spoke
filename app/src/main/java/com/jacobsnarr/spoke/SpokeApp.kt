package com.jacobsnarr.spoke

import android.app.Application
import com.jacobsnarr.spoke.di.AppContainer

class SpokeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
