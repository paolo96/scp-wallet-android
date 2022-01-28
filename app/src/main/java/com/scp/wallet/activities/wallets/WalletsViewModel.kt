package com.scp.wallet.activities.wallets

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.scp.wallet.api.API
import com.scp.wallet.exceptions.ApiException
import com.scp.wallet.scp.CurrencyValue
import com.scp.wallet.scp.Transaction
import com.scp.wallet.scp.UnlockHash
import com.scp.wallet.wallet.Wallet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.System.exit
import java.lang.ref.WeakReference
import java.math.BigInteger
import kotlin.concurrent.thread

class WalletsViewModel(application: Application) : AndroidViewModel(application) {

    val wallets = MutableLiveData<ArrayList<Wallet>>().apply {
        value = restoreWallets()
    }

    val transactions = MutableLiveData<ArrayList<Transaction>>().apply {
        value = arrayListOf()
    }

    val consensusHeight = MutableLiveData<Int>()
    val scpPrice = MutableLiveData<Double>()
    val transactionFee = MutableLiveData<CurrencyValue>()

    //Updates consensusHeight and scp fiat price
    fun updateScprimeData(callback: (() -> Unit)? = null) {

        API.getScprimeData({ cHeight, minFee, maxFee, scpUsd  ->
            if(consensusHeight.value != cHeight) consensusHeight.value = cHeight
            if(scpPrice.value != scpUsd) scpPrice.value = scpUsd
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
            scpPrice.value?.let { scpPrice ->
                wallet.updateFiatBalance(scpPrice)
            }
            password?.let { walletPassword ->
                wallet.unlockWithKey(walletPassword)
            }
        }
    }

    fun updateWalletsFiatBalance() {
        scpPrice.value?.let { scpPrice ->
            wallets.value?.forEach {
                it.updateFiatBalance(scpPrice)
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

    //Updates current displayed wallet transactions and scprime data and calls the callback
    //when both have finished
    fun updateTransactionsAndScprimeData(walletsAdapter: WeakReference<WalletsAdapter>, walletsLayoutManager: WeakReference<LinearLayoutManager>, callback: (() -> Unit)? = null) {
        var firstCallDone = false
        updateTransactions(walletsAdapter, walletsLayoutManager) {
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
    fun updateTransactions(walletsAdapter: WeakReference<WalletsAdapter>, walletsLayoutManager: WeakReference<LinearLayoutManager>, callback: (() -> Unit)? = null) {
        val currentIndex = walletsLayoutManager.get()?.findFirstVisibleItemPosition()
        if(currentIndex != null) {
            val currentWallet = walletsAdapter.get()?.currentList?.getOrNull(currentIndex)
            if(currentWallet == null) {
                transactions.value = arrayListOf()
                callback?.let { it() }
            } else {
                val currTransactions = currentWallet.getTransactions().filter { it.walletValue != null && it.walletValue?.value != BigInteger.ZERO }
                transactionsBlocksPassed(currTransactions)
                transactions.value = ArrayList(currTransactions)
                try {
                    currentWallet.downloadWalletData { result ->
                        if(result) {
                            val newPosition = walletsLayoutManager.get()?.findFirstVisibleItemPosition()
                            if(newPosition == currentIndex && walletsAdapter.get()?.currentList?.getOrNull(newPosition)?.id == currentWallet.id) {
                                val newTransactions = currentWallet.getTransactions().filter { it.walletValue != null && it.walletValue?.value != BigInteger.ZERO }
                                transactionsBlocksPassed(newTransactions)
                                scpPrice.value?.let { scpPrice ->
                                    currentWallet.updateFiatBalance(scpPrice)
                                }
                                transactions.postValue(ArrayList(newTransactions))
                                callback?.let { it() }
                            } else {
                                callback?.let { it() }
                            }
                        } else {
                            callback?.let { it() }
                        }
                    }
                } catch (e: ApiException) {
                    callback?.let { it() }
                }
            }
        } else {
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