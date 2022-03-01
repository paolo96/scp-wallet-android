package com.scp.wallet.activities.newwallet

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.scp.wallet.R
import com.scp.wallet.activities.createwallet.CreateWalletActivity
import com.scp.wallet.activities.importwallet.ImportWalletActivity
import com.scp.wallet.databinding.ActivityNewWalletBinding

class NewWalletActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewWalletBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.customActionBar.actionBarTitle.text = getString(R.string.activity_title_new_wallet)

        initListeners()

    }

    private fun initListeners() {

        binding.newWalletCreate.setOnClickListener {
            val i = Intent(this, CreateWalletActivity::class.java)
            i.putExtra(IE_WALLET_NAME, binding.newWalletName.text.toString())
            i.putExtra(IE_WALLET_PWD, binding.newWalletPassword.text.toString())
            startActivity(i)
        }

        binding.newWalletImport.setOnClickListener {
            val i = Intent(this, ImportWalletActivity::class.java)
            i.putExtra(IE_WALLET_NAME, binding.newWalletName.text.toString())
            i.putExtra(IE_WALLET_PWD, binding.newWalletPassword.text.toString())
            startActivity(i)
        }

        binding.customActionBar.actionBarBack.setOnClickListener {
            onBackPressed()
        }

    }

    companion object {
        const val IE_WALLET_NAME = "wallet-name"
        const val IE_WALLET_PWD = "wallet-pwd"
    }

}