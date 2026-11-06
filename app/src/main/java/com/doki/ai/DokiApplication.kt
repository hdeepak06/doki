package com.doki.ai

import android.app.Application
import com.doki.ai.di.AppContainer
import com.doki.ai.di.DefaultAppContainer

class DokiApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}

