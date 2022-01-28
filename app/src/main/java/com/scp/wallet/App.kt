package com.scp.wallet

import android.app.Application
import com.scp.wallet.crypto.Crypto

class ScpWalletApp: Application() {

    override fun onCreate() {
        super.onCreate()
        Crypto.initCryptoAndroid()
    }

}
