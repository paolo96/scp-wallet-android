package com.scp.wallet.activities.walletsettings

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.scp.wallet.R
import com.scp.wallet.activities.exportseed.ExportSeedActivity
import com.scp.wallet.activities.wallets.WalletsActivity
import com.scp.wallet.activities.wallets.WalletsActivity.Companion.IE_WALLET_ID
import com.scp.wallet.activities.wallets.WalletsActivity.Companion.IE_WALLET_PWD
import com.scp.wallet.databinding.ActivityWalletSettingsBinding
import com.scp.wallet.exceptions.WrongWalletPasswordException
import com.scp.wallet.ui.Popup
import com.scp.wallet.wallet.Wallet

class WalletSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWalletSettingsBinding
    private lateinit var wallet: Wallet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWalletSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.customActionBar.actionBarTitle.text = getString(R.string.activity_title_wallet_settings)

        val walletId = intent.getStringExtra(IE_WALLET_ID)
        val walletPwd = intent.getByteArrayExtra(IE_WALLET_PWD)

        if(walletId != null) {

            wallet = Wallet(walletId, this)
            walletPwd?.let {
                try {
                    wallet.unlockWithKey(it)
                } catch (e: WrongWalletPasswordException) {
                    wallet.lock()
                }
            }

            initViews()
            initListeners()

        } else {
            setResult(RESULT_CANCELED, Intent())
            finish()
        }

    }

    private fun initViews() {

        binding.walletSettingsName.setText(wallet.name)
        binding.walletSettingsAddresses.setText(wallet.getProgress().toString())

    }

    private fun initListeners() {

        binding.walletSettingsName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                wallet.updateName(binding.walletSettingsName.text.toString())
            }
        })

        binding.walletSettingsDelete.setOnClickListener {

            Popup.showChoice(getString(R.string.popup_title_remove_wallet), getString(R.string.popup_description_remove_wallet), this) { result ->
                if(result) {

                    wallet.getDataAccess().reset()

                    val sp = getSharedPreferences(WalletsActivity.SP_WALLETS_IDS, MODE_PRIVATE)

                    val resultIds = mutableSetOf<String>()
                    sp.getStringSet(WalletsActivity.SP_WALLETS_IDS, null)?.let { currIds ->
                        resultIds.addAll(currIds.filter { it != wallet.id })
                    }
                    sp.edit().putStringSet(WalletsActivity.SP_WALLETS_IDS, resultIds).apply()

                    setResult(RESULT_CANCELED, Intent())
                    finish()

                }
            }

        }

        binding.walletSettingsExport.setOnClickListener {

            val seed = wallet.getSeed()
            if(seed == null) {
                Popup.showUnlockWallet(wallet, this) { result ->
                    if(result) {
                        wallet.getSeed()?.let {
                            showWalletSeed(it)
                        }
                    }
                }
            } else {
                showWalletSeed(seed)
            }

        }

        binding.walletSettingsSave.setOnClickListener {

            if(wallet.getProgress() != binding.walletSettingsAddresses.text.toString().toInt()) {

                Popup.showChoice(getString(R.string.popup_title_advanced_warning), getString(R.string.popup_description_advanced_warning), this) { result ->
                    if(result) {
                        val newAddressesNum = binding.walletSettingsAddresses.text.toString().toInt() - wallet.getProgress()
                        if(newAddressesNum <= 0) {
                            Toast.makeText(this, getString(R.string.toast_invalid_number_addresses), Toast.LENGTH_SHORT).show()
                        } else if(newAddressesNum > Wallet.MAX_NUM_ADDRESSES_IMPORT) {
                            Toast.makeText(this, getString(R.string.toast_max_allowed_import_addresses, Wallet.MAX_NUM_ADDRESSES_IMPORT.toString()), Toast.LENGTH_SHORT).show()
                        } else {

                            val seed = wallet.getSeed()
                            if(seed == null) {
                                Popup.showUnlockWallet(wallet, this) { unlockResult ->
                                    if(unlockResult) {
                                        wallet.getSeed()?.let {
                                            wallet.newAddresses(newAddressesNum)
                                            onBackPressed()
                                        }
                                    }
                                }
                            } else {
                                wallet.newAddresses(newAddressesNum)
                                onBackPressed()
                            }

                        }
                    } else {
                        binding.walletSettingsAddresses.setText(wallet.getProgress().toString())
                    }
                }

            }

        }

        binding.walletSettingsChangePassword.setOnClickListener {

            Popup.showChangePasswordWallet(wallet, this) { result ->
                if(result) {
                    Toast.makeText(this, getString(R.string.toast_password_change_success), Toast.LENGTH_SHORT).show()
                }
            }

        }

        binding.customActionBar.actionBarBack.setOnClickListener {
            onBackPressed()
        }

    }

    private fun showWalletSeed(seed: ByteArray) {
        val i = Intent(this, ExportSeedActivity::class.java)
        i.putExtra(ExportSeedActivity.IE_SEED, wallet.getHelper().seedToString(seed,null))
        startActivity(i)
    }

    override fun onBackPressed() {
        val returnIntent = Intent()
        returnIntent.putExtra(IE_WALLET_ID, wallet.id)
        returnIntent.putExtra(IE_WALLET_PWD, wallet.getPassword())
        setResult(RESULT_OK, returnIntent)
        finish()
    }


}