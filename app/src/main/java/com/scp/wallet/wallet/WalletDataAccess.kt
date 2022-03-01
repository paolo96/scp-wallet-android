package com.scp.wallet.wallet

import android.content.Context
import androidx.security.crypto.MasterKey
import com.scp.wallet.scp.SpendableKey
import com.scp.wallet.scp.Transaction
import androidx.security.crypto.EncryptedSharedPreferences
import android.content.SharedPreferences
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.scp.wallet.R
import com.scp.wallet.crypto.Crypto.Companion.XSALSA20_POLY1305_NONCEBYTES
import com.scp.wallet.crypto.Crypto.Companion.crypto
import com.scp.wallet.exceptions.WrongWalletPasswordException
import com.scp.wallet.utils.Strings
import java.lang.Exception

//Stores and retrieves wallet data from the device
//Everything is encrypted using AES256. The encryption is managed by androidx.security.crypto
//The wallet seed is further encrypted using the walletKey which is chose by the user and not stored
class WalletDataAccess(walletId: String, context: Context) {

    companion object {

        const val SHARED_PREF_WALLET_PREFIX = "wallet-data-"

        const val KEY_PROGRESS = "progress"
        const val KEY_NAME = "name"
        const val KEY_TRANSACTIONS = "transactions"
        const val KEY_SEED = "seed"
        const val KEY_SEED_NONCE = "seed-nonce"
        const val KEY_ADDRESSES = "addresses"
        const val KEY_ADDRESSES_KEYS = "addresses-keys"
        const val KEY_ADDRESSES_KEYS_NONCE = "addresses-keys-nonce"

    }

    private val sharedPreferences: SharedPreferences

    init {

        val mainKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "$SHARED_PREF_WALLET_PREFIX$walletId",
            mainKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

    }

    //Stores the seed encrypted with the wallet key, overriding what's currently written
    //Should be used only the first time or to force a seed update
    fun updateSeed(walletKey: ByteArray, seed: ByteArray, force: Boolean = true) {

        val nonce = crypto.randomByteArray(XSALSA20_POLY1305_NONCEBYTES)
        val walletSigningKey = crypto.blake2b(walletKey + nonce)
        val encryptedSeed = crypto.encryptMessage(seed, nonce, walletSigningKey)

        if(force) {
            reset()
        }
        val editor = sharedPreferences.edit()
        editor.putString(KEY_SEED, Base64.encodeToString(encryptedSeed, Base64.NO_WRAP))
        editor.putString(KEY_SEED_NONCE, Base64.encodeToString(nonce, Base64.NO_WRAP))
        editor.apply()

    }

    //Tries to retrieve the seed with the provided password
    @Throws(WrongWalletPasswordException::class)
    fun getSeed(walletKey: ByteArray) : ByteArray {

        val encryptedSeed = Base64.decode(sharedPreferences.getString(KEY_SEED, ""), Base64.NO_WRAP)
        val nonce = Base64.decode(sharedPreferences.getString(KEY_SEED_NONCE, ""), Base64.NO_WRAP)
        val walletSigningKey = crypto.blake2b(walletKey + nonce)

        try {
            return crypto.decryptMessage(encryptedSeed, nonce, walletSigningKey)
        } catch (e: Exception) {
            throw WrongWalletPasswordException()
        }

    }

    //Updates the wallet password by retrieving the encrypted seed and encrypting it again
    //with the new password
    @Throws(WrongWalletPasswordException::class)
    fun changeWalletKey(walletKey: ByteArray, newKey: ByteArray) {
        updateSeed(newKey, getSeed(walletKey), false)
        updateKeys(newKey, getKeys(walletKey))
    }

    //Deletes everything stored
    fun reset() {
        sharedPreferences.edit().clear().apply()
    }

    //Retrieves the wallet progress (number of addresses keeping track of)
    fun getProgress() : Int {
        return sharedPreferences.getInt(KEY_PROGRESS, 0)
    }

    //Updates the wallet progress
    fun updateProgress(p: Int) {
        sharedPreferences.edit().putInt(KEY_PROGRESS, p).apply()
    }

    //Retrieves the wallet name
    fun getName() : String {
        return sharedPreferences.getString(KEY_NAME, null) ?: Strings.get(R.string.default_wallet_name)
    }

    //Updates the wallet name
    fun updateName(p: String) {
        sharedPreferences.edit().putString(KEY_NAME, p).apply()
    }

    //Retrieves the wallet addresses with keys
    @Throws(WrongWalletPasswordException::class)
    fun getKeys(walletKey: ByteArray) : Array<SpendableKey> {

        val encryptedResult = sharedPreferences.getString(KEY_ADDRESSES_KEYS, null) ?: return arrayOf()
        val nonce = Base64.decode(sharedPreferences.getString(KEY_ADDRESSES_KEYS_NONCE, ""), Base64.NO_WRAP)

        val walletSigningKey = crypto.blake2b(walletKey + nonce)

        try {
            val savedData = crypto.decryptMessageString(encryptedResult, nonce, walletSigningKey)

            return try {
                Gson().fromJson(savedData, Array<SpendableKey>::class.java)
            } catch (e: JsonSyntaxException) {
                arrayOf()
            }
        } catch (e: Exception) {
            throw WrongWalletPasswordException()
        }

    }

    //Updates the stored wallet keys
    fun updateKeys(walletKey: ByteArray, keys: Array<SpendableKey>) {
        val result = Gson().toJson(keys)

        val nonce = crypto.randomByteArray(XSALSA20_POLY1305_NONCEBYTES)
        val walletSigningKey = crypto.blake2b(walletKey + nonce)
        val encryptedResult = crypto.encryptMessageString(result, nonce, walletSigningKey)

        val editor = sharedPreferences.edit()
        editor.putString(KEY_ADDRESSES_KEYS_NONCE, Base64.encodeToString(nonce, Base64.NO_WRAP))
        editor.putString(KEY_ADDRESSES_KEYS, encryptedResult)
        editor.apply()

    }

    //Retrieves the wallet addresses
    fun getAddresses() : Array<SpendableKey> {

        val savedData = sharedPreferences.getString(KEY_ADDRESSES, null)
        return if(savedData == null) {
            arrayOf()
        } else {
            try {
                Gson().fromJson(savedData, Array<SpendableKey>::class.java)
            } catch (e: JsonSyntaxException) {
                arrayOf()
            }
        }

    }

    //Updates the stored wallet addresses, stripping away the secret keys before storing
    fun updateAddresses(addresses: Array<SpendableKey>) {
        val addressesWithoutSecretsKeys = arrayListOf<SpendableKey>()
        for(a in addresses) {
            addressesWithoutSecretsKeys.add(SpendableKey(null, a.unlockConditions))
        }

        val result = Gson().toJson(addressesWithoutSecretsKeys)
        sharedPreferences.edit().putString(KEY_ADDRESSES, result).apply()
    }

    //Retrieves the wallet transactions
    fun getTransactions() : Array<Transaction> {

        val savedData = sharedPreferences.getString(KEY_TRANSACTIONS, null)
        return if(savedData == null) {
            arrayOf()
        } else {
            try {
                Gson().fromJson(savedData, Array<Transaction>::class.java)
            } catch (e: JsonSyntaxException) {
                arrayOf()
            }
        }

    }

    //Updates the transactions stored for the wallet
    fun updateTransactions(transactions: Array<Transaction>) {
        val result = Gson().toJson(transactions)
        sharedPreferences.edit().putString(KEY_TRANSACTIONS, result).apply()
    }

}