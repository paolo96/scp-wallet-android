package com.scp.wallet.activities.createwallet

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.scp.wallet.R
import com.scp.wallet.activities.newwallet.NewWalletActivity
import com.scp.wallet.activities.wallets.WalletsActivity
import com.scp.wallet.crypto.Crypto
import com.scp.wallet.databinding.ActivityCreateWalletBinding
import com.scp.wallet.ui.SeedInterface
import com.scp.wallet.utils.Hex
import com.scp.wallet.wallet.Wallet

class CreateWalletActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateWalletBinding

    private lateinit var newWallet: Wallet
    private lateinit var newSeed: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.customActionBar.actionBarTitle.text = getString(R.string.activity_title_create_wallet)

        val walletPassword = intent.getStringExtra(NewWalletActivity.IE_WALLET_PWD)

        if(walletPassword != null) {

            newWallet = Wallet(Hex.bytesToHexToString(Crypto.crypto.randomByteArray(16)), this)
            newSeed = newWallet.initNew(walletPassword)
            val walletName = intent.getStringExtra(NewWalletActivity.IE_WALLET_NAME)
            if(!walletName.isNullOrEmpty()) {
                newWallet.updateName(walletName)
            }
            newWallet.unlock(walletPassword)

            SeedInterface.drawSeed(newSeed, binding.createWalletSeedContainer, this)

            binding.createWalletConfirm.text = getString(R.string.button_seed_warning, newSeed.split(" ").size.toString())
            initListeners()

        } else {
            onBackPressed()
        }

    }

    private fun initListeners() {

        binding.createWalletConfirm.setOnClickListener {

            newWallet.newAddress()

            val sp = getSharedPreferences(WalletsActivity.SP_WALLETS_IDS, MODE_PRIVATE)

            val result = mutableSetOf(newWallet.id)
            sp.getStringSet(WalletsActivity.SP_WALLETS_IDS, null)?.let { currIds ->
                result.addAll(currIds)
            }
            sp.edit().putStringSet(WalletsActivity.SP_WALLETS_IDS, result).apply()

            val intent = Intent(this, WalletsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)

        }

        binding.customActionBar.actionBarBack.setOnClickListener {
            onBackPressed()
        }

    }


}