package com.scp.wallet

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.scp.wallet.crypto.Crypto
import com.scp.wallet.utils.Hex
import com.scp.wallet.wallet.Wallet
import com.scp.wallet.wallet.WalletDataAccess
import com.scp.wallet.wallet.WalletDataAccess.Companion.KEY_PROGRESS
import com.scp.wallet.wallet.WalletDataAccess.Companion.SHARED_PREF_WALLET_PREFIX

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WalletDataAccessTest {

    @Test
    fun updateTest() {

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val walletId = UtilsAndroidTest.getRandomString(10)
        val walletDataAccess = WalletDataAccess(walletId, appContext)

        val randomInt = (1 until Int.MAX_VALUE).random()
        walletDataAccess.updateProgress(randomInt)
        assert(randomInt == walletDataAccess.getProgress())

        val walletDataAccess2 = WalletDataAccess(walletId, appContext)
        assert(walletDataAccess2.getProgress() == walletDataAccess.getProgress())

        walletDataAccess2.updateProgress(randomInt+1)
        assert(walletDataAccess2.getProgress() == walletDataAccess.getProgress())
        assert(randomInt != walletDataAccess.getProgress())

    }

    @Test
    fun encryptionTest() {

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val walletId = UtilsAndroidTest.getRandomString(10)
        val walletDataAccess = WalletDataAccess(walletId, appContext)

        val unencryptedPreferences = appContext.getSharedPreferences("$SHARED_PREF_WALLET_PREFIX$walletId", Context.MODE_PRIVATE)

        val randomInt = (1..Int.MAX_VALUE).random()
        walletDataAccess.updateProgress(randomInt)

        assert(unencryptedPreferences.getInt(KEY_PROGRESS, 0) != randomInt)

    }

    @Test
    fun seedTest() {

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val walletId = UtilsAndroidTest.getRandomString(10)
        val wallet = Wallet(walletId, appContext)

        Crypto.initCryptoAndroid()
        val password = "lorem ipsum dolor sit amen"
        val walletKey = Crypto.crypto.blake2b(password.toByteArray())
        val newSeed = Crypto.crypto.randomByteArray(Crypto.HASH_SIZE)

        wallet.getDataAccess().updateSeed(walletKey, newSeed)
        val retrievedSeed = wallet.getDataAccess().getSeed(walletKey)

        assert(retrievedSeed.contentEquals(newSeed))

        wallet.getHelper().seedToString(newSeed, null).let { seedString ->
            wallet.initSeed(seedString, password)
            wallet.unlock(password)
        }

    }

}