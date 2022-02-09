package com.scp.wallet.activities.wallets

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.scp.wallet.activities.launch.LaunchActivity
import com.scp.wallet.api.API
import com.scp.wallet.exceptions.ApiException
import com.scp.wallet.scp.CurrencyValue
import com.scp.wallet.scp.Transaction
import com.scp.wallet.utils.Currency
import com.scp.wallet.wallet.Wallet
import java.math.BigInteger

class WalletsViewModel(application: Application) : AndroidViewModel(application) {

    val wallets = MutableLiveData<ArrayList<Wallet>>().apply {
        value = restoreWallets()
    }

    val currency = MutableLiveData<String>().apply {
        value = restoreCurrency()
    }

    val transactions = MutableLiveData<Pair<Wallet?, ArrayList<Transaction>>>().apply {
        value = Pair(null, arrayListOf())
    }


    val consensusHeight = MutableLiveData<Int>()
    val scpExchangeRates = MutableLiveData<Map<String, Double>>()
    val transactionFee = MutableLiveData<CurrencyValue>()

    //Updates consensusHeight and scp fiat price
    fun updateScprimeData(callback: (() -> Unit)? = null) {

        API.getScprimeData({ cHeight, minFee, maxFee, exchangeRates  ->
            if(consensusHeight.value != cHeight) consensusHeight.value = cHeight
            if(scpExchangeRates.value != exchangeRates) scpExchangeRates.value = exchangeRates
            val fee = Transaction.fee(minFee, maxFee)
            if(transactionFee.value?.value != fee.value) transactionFee.value = fee
            callback?.let { it() }
        }, {
            callback?.let { it() }
        })

    }

    fun updateWallets() {
        val app = getApplication<Application>()
        val sp = app.getSharedPreferences(WalletsActivity.SP_WALLETS_IDS, AppCompatActivity.MODE_PRIVATE)
        val newIds = sp.getStringSet(WalletsActivity.SP_WALLETS_IDS, mutableSetOf())
        val oldIds = wallets.value?.map { it.id }?.toSet()
        if(newIds != oldIds) {
            wallets.value = restoreWallets()
        }
    }

    fun updateWallet(id: String, password: ByteArray?) {
        wallets.value?.find { it.id == id }?.let {  wallet ->
            wallet.updateDataFromStorage()
            currency.value?.let { currency ->
                scpExchangeRates.value?.get(currency)?.let { scpPrice ->
                    wallet.updateFiatBalance(Pair(currency, scpPrice))
                }
            }
            password?.let { walletPassword ->
                wallet.unlockWithKey(walletPassword)
            }
        }
    }

    fun updateWalletsFiatBalance() {
        currency.value?.let { currency ->
            scpExchangeRates.value?.get(currency)?.let { scpPrice ->
                wallets.value?.forEach {
                    it.updateFiatBalance(Pair(currency, scpPrice))
                }
            }
        }
    }

    private fun restoreWallets() : ArrayList<Wallet> {
        val result = arrayListOf<Wallet>()
        val app = getApplication<Application>()
        val sp = app.getSharedPreferences(WalletsActivity.SP_WALLETS_IDS, AppCompatActivity.MODE_PRIVATE)
        sp.getStringSet(WalletsActivity.SP_WALLETS_IDS, mutableSetOf())?.forEach {
            result.add(Wallet(it, app))
        }
        return result
    }

    private fun restoreCurrency() : String {
        var result = Currency.DEFAULT_CURRENCY
        val app = getApplication<Application>()
        val sp = app.getSharedPreferences(LaunchActivity.SP_FILE_SETTINGS, AppCompatActivity.MODE_PRIVATE)
        sp.getString(LaunchActivity.SP_CURRENCY, result)?.let {
            result = it
        }
        return result
    }

    //Updates current displayed wallet transactions and scprime data and calls the callback
    //when both have finished
    fun updateTransactionsAndScprimeData(currentWallet: Wallet, callback: (() -> Unit)? = null) {
        var firstCallDone = false
        updateTransactions(currentWallet) {
            if(firstCallDone) {
                callback?.let { it() }
            } else {
                firstCallDone = true
            }
        }
        updateScprimeData {
            if(firstCallDone) {
                callback?.let { it() }
            } else {
                firstCallDone = true
            }
        }
    }

    //Updates transactions with the ones the wallet currently owns, then it sends an API request
    //to get the most recent transactions and it updates only if the activity is still there
    fun updateTransactions(currentWallet: Wallet, callback: (() -> Unit)? = null) {
        val currTransactions = currentWallet.getTransactions().filter { it.walletValue != null && it.walletValue?.value != BigInteger.ZERO }
        transactionsBlocksPassed(currTransactions)
        transactions.value = Pair(currentWallet, ArrayList(currTransactions))
        try {
            currentWallet.downloadWalletData { result ->
                if(result) {
                    val newTransactions = currentWallet.getTransactions().filter { it.walletValue != null && it.walletValue?.value != BigInteger.ZERO }
                    transactionsBlocksPassed(newTransactions)
                    currency.value?.let { currency ->
                        scpExchangeRates.value?.get(currency)?.let { scpPrice ->
                            currentWallet.updateFiatBalance(Pair(currency, scpPrice))
                        }
                    }
                    transactions.postValue(Pair(currentWallet, ArrayList(newTransactions)))
                    callback?.let { it() }
                } else {
                    callback?.let { it() }
                }
            }
        } catch (e: ApiException) {
            callback?.let { it() }
        }
    }

    fun transactionsBlocksPassed(transactions: List<Transaction>) {
        consensusHeight.value?.let { currHeight ->
            transactions.forEach {
                it.confirmationHeight?.let { tHeight ->
                    it.confirmationBlocksPassed = currHeight - tHeight
                }
            }
        }
    }

}