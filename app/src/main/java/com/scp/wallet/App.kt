package com.scp.wallet

import android.app.Application
import com.scp.wallet.crypto.Crypto

class ScpWalletApp: Application() {

    companion object {
        lateinit var instance: ScpWalletApp private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Crypto.initCryptoAndroid()
    }

}
