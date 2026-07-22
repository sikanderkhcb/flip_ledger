package com.circuitflip.flipledger.android

import android.app.Application
import com.circuitflip.flipledger.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

/** Boots dependency injection once for the whole process, injecting the Android context. */
class FlipLedgerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger(Level.ERROR)
            androidContext(this@FlipLedgerApplication)
        }
    }
}
