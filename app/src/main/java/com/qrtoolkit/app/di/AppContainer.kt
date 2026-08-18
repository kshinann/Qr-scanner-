package com.qrtoolkit.app.di

import android.content.Context
import com.qrtoolkit.app.data.lookup.ProductLookupService
import com.qrtoolkit.app.data.scan.ScanHistoryStore

/** Small hand-rolled DI container (no Hilt/Dagger dependency for an app this size). */
class AppContainer(context: Context) {
    val scanHistoryStore = ScanHistoryStore(context)
    val productLookupService = ProductLookupService()
}

fun Context.appContainer(): AppContainer =
    (applicationContext as com.qrtoolkit.app.QrToolkitApplication).container
