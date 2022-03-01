package com.scp.wallet

import com.scp.wallet.crypto.Crypto.Companion.XSALSA20_POLY1305_NONCEBYTES
import com.scp.wallet.crypto.Crypto.Companion.crypto
import org.junit.Test

class CryptoTest {

    @Test
    fun secretCryptoTest() {

        UtilsTest.useLazySodiumJava()

        val message = "Hello world"
        val messageBytes = crypto.randomByteArray(10)
        val password = "lorem ipsum dolor sit amen"
        val nonce = crypto.randomByteArray(XSALSA20_POLY1305_NONCEBYTES)
        val walletKey = crypto.blake2b(password.toByteArray())

        assert(crypto.decryptMessageString(crypto.encryptMessageString(message, nonce, walletKey),nonce, walletKey) == message)
        assert(crypto.decryptMessage(crypto.encryptMessage(messageBytes, nonce, walletKey),nonce, walletKey).contentEquals(messageBytes))

    }

}