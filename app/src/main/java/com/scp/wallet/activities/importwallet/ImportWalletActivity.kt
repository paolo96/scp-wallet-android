package com.scp.wallet.activities.importwallet

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.scp.wallet.activities.newwallet.NewWalletActivity
import com.scp.wallet.activities.wallets.WalletsActivity
import com.scp.wallet.crypto.Crypto
import com.scp.wallet.databinding.ActivityImportWalletBinding
import com.scp.wallet.exceptions.InvalidSeedStringException
import com.scp.wallet.utils.Hex
import com.scp.wallet.ui.Popup
import com.scp.wallet.wallet.Wallet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class ImportWalletActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImportWalletBinding

    private lateinit var walletPassword: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImportWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.customActionBar.actionBarTitle.text = "Import wallet"

        val walletPwd = intent.getStringExtra(NewWalletActivity.IE_WALLET_PWD)

        if(walletPwd != null) {

            this.walletPassword = walletPwd

            initListeners()

        } else {
            onBackPressed()
        }

    }

    private fun initListeners() {

        binding.importWalletButton.setOnClickListener {

            val potentialSeed = binding.importWalletSeed.text.toString().lowercase(Locale.US)

            if(!potentialSeed.matches(Regex("[a-z\\n\\s;,]+"))) {
                invalidSeed()
                return@setOnClickListener
            }

            var seed = potentialSeed.replace("\\s+", " ").split(" ").filter { it != "\n" }
            if(seed.size == 29 || seed.size == 28) {
                importSeed(seed.joinToString(" "))
                return@setOnClickListener
            }

            seed = potentialSeed.replace(" ", "").split("\n").filter { it != "\n" }
            if(seed.size == 29 || seed.size == 28) {
                importSeed(seed.joinToString(" "))
                return@setOnClickListener
            }

            seed = potentialSeed.replace(" ", "").split(";").filter { it != "\n" }
            if(seed.size == 29 || seed.size == 28) {
                importSeed(seed.joinToString(" "))
                return@setOnClickListener
            }

            seed = potentialSeed.replace(" ", "").split(",").filter { it != "\n" }
            if(seed.size == 29 || seed.size == 28) {
                importSeed(seed.joinToString(" "))
                return@setOnClickListener
            }

            invalidSeed()

        }

        binding.customActionBar.actionBarBack.setOnClickListener {
            onBackPressed()
        }

    }

    private fun invalidSeed() {
        Popup.showSimple("Invalid seed", "Supported formats are 29 or 28 words seed (english) separated by whitespaces or new lines or commas or semicolons.", this)
    }

    private fun importSeed(seed: String) {

        val newWallet = Wallet(Hex.bytesToHexToString(Crypto.crypto.randomByteArray(16)), this)
        try {
            newWallet.initSeed(seed, walletPassword)

            val walletName = intent.getStringExtra(NewWalletActivity.IE_WALLET_NAME)
            if(!walletName.isNullOrEmpty()) {
                newWallet.updateName(walletName)
            }
            newWallet.unlock(walletPassword)

            binding.importWalletButton.isEnabled = false
            findUnusedAddresses(newWallet, 0, { found ->
                if(found) {
                    importWallet(newWallet.id)
                } else {
                    Popup.showChoice("Wallet is too big", "This wallet has used many addresses, Scp Wallet is not currently built to handle large wallets. If you import this seed you could experience missing transactions and you may not be able to send SCP coins. Do you want to proceed anyway?", this) { proceed ->
                        if(proceed) {
                            importWallet(newWallet.id)
                        }
                    }
                }
                activateImportButtonWithDelay()
            }, {
                Popup.showSimple("Something went wrong", "An error happened while trying to sync the wallet transactions. Retry later or change server.", this)
                activateImportButtonWithDelay()
            })

        } catch (e: InvalidSeedStringException) {
            invalidSeed()
        }

    }

    private fun activateImportButtonWithDelay() {
        lifecycleScope.launch {
            delay(1000)
            runOnUiThread {
                binding.importWalletButton.isEnabled = true
            }
        }
    }

    private fun importWallet(walletId: String) {

        val sp = getSharedPreferences(WalletsActivity.SP_WALLETS_IDS, MODE_PRIVATE)

        val result = mutableSetOf(walletId)
        sp.getStringSet(WalletsActivity.SP_WALLETS_IDS, null)?.let { currIds ->
            result.addAll(currIds)
        }
        sp.edit().putStringSet(WalletsActivity.SP_WALLETS_IDS, result).apply()

        val intent = Intent(this, WalletsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)

    }

    private fun findUnusedAddresses(w: Wallet, i: Int, callback: (Boolean) -> Unit, callbackError: () -> Unit) {

        val testAddressIndexes = arrayOf(10, 50, 100, 250, 500)

        if(i == testAddressIndexes.size) {
            callback(false)
            return
        }
        val newAddressesToGenerate = if(i == 0) testAddressIndexes[0] else testAddressIndexes[i]-testAddressIndexes[i-1]
        w.newAddresses(newAddressesToGenerate)

        w.downloadWalletData { success ->
            if(success) {
                val walletAddresses = w.getKeys()
                for(addressIndex in walletAddresses.size-1 downTo walletAddresses.size-1-IMPORT_ADDRESSES_TO_CHECK) {
                    if(w.getHelper().hasAddressBeenUsed(walletAddresses[addressIndex], w.getTransactions())) {
                        this.findUnusedAddresses(w, i+1, callback, callbackError)
                        return@downloadWalletData
                    }
                }
                callback(true)
            } else {
                callbackError()
            }
        }

    }

    companion object {
        const val IMPORT_ADDRESSES_TO_CHECK = 5
    }

}






