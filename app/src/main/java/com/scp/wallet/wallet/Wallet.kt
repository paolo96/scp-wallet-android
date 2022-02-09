package com.scp.wallet.wallet

import android.content.Context
import com.google.gson.Gson
import com.scp.wallet.api.API
import com.scp.wallet.crypto.Crypto
import com.scp.wallet.crypto.Crypto.Companion.crypto
import com.scp.wallet.exceptions.InvalidSeedStringException
import com.scp.wallet.exceptions.ApiException
import com.scp.wallet.exceptions.WalletLockedException
import com.scp.wallet.exceptions.WrongWalletPasswordException
import com.scp.wallet.scp.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.TestOnly
import java.lang.Exception
import java.math.BigInteger
import kotlin.concurrent.thread

class Wallet(val id: String, context: Context) {

    companion object {

        const val SEED_CHECKSUM_SIZE = 6
        const val MAX_NUM_ADDRESSES_IMPORT = 1000
        val SUPPORTED_DICTIONARIES = arrayOf("english")

    }

    private var dataAccess: WalletDataAccess = WalletDataAccess(id, context)
    private var helper: WalletHelper = WalletHelper(dataAccess)

    private var seed: ByteArray? = null
    private var walletKey: ByteArray? = null

    var name: String = dataAccess.getName()
    private var transactions: MutableList<Transaction> = dataAccess.getTransactions().toMutableList()
    private var keys: MutableList<SpendableKey> = dataAccess.getAddresses().toMutableList()

    private var fiatBalance: Pair<String, Double>? = null

    fun updateDataFromStorage() {
        name = dataAccess.getName()
        transactions = dataAccess.getTransactions().toMutableList()
        walletKey?.let { validEncryptionKey ->
            seed = dataAccess.getSeed(validEncryptionKey)
            keys = dataAccess.getKeys(validEncryptionKey).toMutableList()
        }
    }

    //Downloads transactions relevant for this wallet from server
    fun downloadWalletData(callback: (Boolean) -> Unit) {

        val addresses = keys.map { it.unlockConditions }.toTypedArray()

        API.getTransactions(addresses, { newTransactions ->
            thread {
                transactions = newTransactions.toMutableList()
                transactions.sortByDescending { it.blockTimestamp }
                transactions.sortByDescending { it.blockTimestamp == null }
                transactions.forEach {
                    it.walletValue = helper.transactionValue(it, keys, transactions)
                }
                dataAccess.updateTransactions(transactions.toTypedArray())
                callback(true)
            }
        }, {
            callback(false)
        })

    }

    //Creates a random seed for this wallet and stores it
    //Should only be called only once per new wallet
    @Throws(Exception::class)
    fun initNew(encryptionPassword: String?, dictionaryId: String? = null) : String {

        val seed = crypto.randomByteArray(Crypto.ENTROPY_SIZE)

        val encryptionKey = if (encryptionPassword != null) {
            crypto.blake2b(encryptionPassword.toByteArray())
        } else {
            null
        }

        //If wallet key is null, use the seed as the wallet key
        val walletKeyNotNull = encryptionKey ?: crypto.blake2b(seed)
        dataAccess.updateSeed(walletKeyNotNull, seed)

        return helper.seedToString(seed, dictionaryId)

    }

    //Initializes the wallet with the given seed and password
    //If a seed already exists it overrides it
    @Throws(InvalidSeedStringException::class)
    fun initSeed(seedString: String, encryptionPassword: String?, dictionaryId: String? = null) {

        val encryptionKey = if(encryptionPassword != null) {
            crypto.blake2b(encryptionPassword.toByteArray())
        } else {
            null
        }

        val seed = helper.stringToSeed(seedString, dictionaryId)
        helper.initSeed(encryptionKey, seed)

    }

    //Changes the wallet password to newPassword
    @Throws(WrongWalletPasswordException::class)
    fun changePassword(encryptionPassword: String, newPassword: String) {

        val newKey = crypto.blake2b(newPassword.toByteArray())
        val key = crypto.blake2b(encryptionPassword.toByteArray())

        dataAccess.changeWalletKey(key, newKey)

    }

    //Locks the wallet, removing the seed and the spendable private keys from memory
    fun lock() {

        seed = null
        walletKey = null
        keys = dataAccess.getAddresses().toMutableList()

    }

    //Unlocks the wallet with the password
    @Throws(WrongWalletPasswordException::class)
    fun unlock(encryptionPassword: String) {

        val encryptionKey = crypto.blake2b(encryptionPassword.toByteArray())
        unlockWithKey(encryptionKey)

    }

    //Unlocks the wallet loading the seed and the spendable private keys into memory
    @Throws(WrongWalletPasswordException::class)
    fun unlockWithKey(encryptionKey: ByteArray) {

        walletKey = encryptionKey
        seed = dataAccess.getSeed(encryptionKey)
        keys = dataAccess.getKeys(encryptionKey).toMutableList()

    }

    //Signs a simple transaction with all the available keys
    fun sign(transaction: Transaction, toSign: Array<ByteArray>) {

        seed?.let {

            API.getScprimeData ({ consensusHeight, _, _, _ ->

                val signable = arrayListOf<ByteArray>()
                signable.addAll(toSign)

                if(toSign.isEmpty()) {
                    for(sci in transaction.inputs) {
                        if(keys.find { it.unlockConditions.publicKeys.contentEquals(sci.unlockConditions.publicKeys) } != null) {
                            signable.add(sci.parentId)
                        }
                    }
                }

                helper.signTransaction(transaction, keys.toTypedArray(), signable.toTypedArray(), consensusHeight)

            }, {
                throw ApiException()
            })

        }
        throw WalletLockedException()


    }

    //Creates a simple transaction of amount to dest, signs it and sends it to the API to
    //broadcasts it
    fun send(currencyValue: CurrencyValue, destAddress: ByteArray, feeIncluded: Boolean, callback: () -> Unit, callbackError: (String) -> Unit) {

        val txnBuilder = TransactionBuilder(this)
        txnBuilder.newFoundedByParents(currencyValue, destAddress, feeIncluded, { txnSet ->
            API.postTransactions(txnSet.first, txnSet.second) { result ->
                if(result) {
                    callback()
                } else {
                    callbackError("The transaction could not be broadcasted to the network")
                }
            }
        }, callbackError)

    }

    //Generates a new address for this wallet
    @Throws(WalletLockedException::class, ApiException::class)
    fun newAddress() : String {

        seed?.let {
            val newAddress = helper.nextAddress(it)
            addKeys(arrayOf(newAddress))
            return UnlockHash.fromUnlockConditionsToAddress(newAddress.unlockConditions)
        }
        throw WalletLockedException()

    }

    //Generates a n new addresses for this wallet
    @Throws(WalletLockedException::class)
    fun newAddresses(n: Int) : Array<String> {

        seed?.let {
            val newAddresses = helper.nextAddresses(n, it)
            addKeys(newAddresses)

            val unlockHashesResult = arrayListOf<String>()
            for (newAddress in newAddresses) {
                val unlockHashSummed = UnlockHash.fromUnlockConditionsToAddress(newAddress.unlockConditions)
                unlockHashesResult.add(unlockHashSummed)
            }
            return unlockHashesResult.toTypedArray()
        }
        throw WalletLockedException()

    }

    fun getBalance() : CurrencyValue {
        return CurrencyValue(transactions.sumOf { it.walletValue?.value ?: BigInteger.ZERO })
    }

    fun getLastTransactionDate() : Long? {
        return transactions.maxByOrNull { it.blockTimestamp ?: 0 }?.blockTimestamp
    }

    override fun equals(other: Any?): Boolean {

        if(other !is Wallet) {
            return false
        }

        val sameName = other.name == this.name
        val sameBalance = other.getBalance().value == this.getBalance().value
        val sameFiatBalance = other.getFiatBalance() == this.getFiatBalance()

        val oldSeed = other.getSeed()
        val newSeed = this.getSeed()
        val sameSeed = oldSeed.contentEquals(newSeed)
        if(!sameName || !sameBalance || !sameSeed || !sameFiatBalance) return false

        var addressesSame = true
        val oldKeys = other.getKeys()
        val newKeys = this.getKeys()
        keysLoop@for(i in oldKeys) {
            for(iN in newKeys) {
                if(!UnlockHash.fromUnlockConditions(i.unlockConditions).contentEquals(UnlockHash.fromUnlockConditions(iN.unlockConditions))) {
                    addressesSame = false
                    break@keysLoop
                }
            }
        }
        if(!addressesSame) return false

        val oldTransactions = other.getTransactions()
        val newTransactions = this.getTransactions()
        transactionsLoop@for(t in oldTransactions) {
            for(tN in newTransactions) {
                if(t != tN) {
                    continue@transactionsLoop
                }
            }
            return false
        }

        return true
    }
    

    //Getters functions

    @TestOnly
    fun getDataAccess() : WalletDataAccess {
        return dataAccess
    }

    fun getHelper() : WalletHelper {
        return helper
    }

    fun getPassword() : ByteArray? {
        return walletKey
    }

    fun getKeys() : List<SpendableKey> {
        return keys
    }

    fun getTransactions() : List<Transaction> {
        return transactions
    }

    fun getSeed() : ByteArray? {
        return seed
    }

    fun getProgress() : Int {
        return dataAccess.getProgress()
    }

    fun getFiatBalance() : Pair<String, Double>? {
        return fiatBalance
    }

    fun addKeys(newKeys: Array<SpendableKey>) {
        walletKey?.let {
            keys.addAll(newKeys)
            dataAccess.updateKeys(it, keys.toTypedArray())
            dataAccess.updateAddresses(keys.toTypedArray())
        }
    }

    fun updateName(newName: String) {
        name = newName
        dataAccess.updateName(newName)
    }

    fun updateFiatBalance(value: Pair<String, Double>) {
        if(value.second > 0) {
            fiatBalance = Pair(value.first, ((getBalance().value / CurrencyValue.COIN_PRECISION_CENTS).toDouble()/100.0)*value.second)
        }
    }

}