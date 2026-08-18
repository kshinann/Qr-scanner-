package com.qrtoolkit.app

import android.app.Application
import com.qrtoolkit.app.di.AppContainer

class QrToolkitApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
