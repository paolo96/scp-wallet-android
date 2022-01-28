package com.scp.wallet

import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava
import com.scp.wallet.crypto.Crypto

object UtilsTest {

    fun useLazySodiumJava() {

        //Lazy sodium android is not available when running unit tests on development machine.
        //Using Lazy sodium java instead
        Crypto.crypto = Crypto(LazySodiumJava(SodiumJava()))

    }

    fun getRandomString(length: Int) : String {
        val charset = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz0123456789"
        return (1..length).map { charset.random() }.joinToString("")
    }


}