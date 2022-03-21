package com.scp.wallet.activities.receive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.scp.wallet.R
import com.scp.wallet.activities.wallets.WalletsActivity
import com.scp.wallet.databinding.ActivityReceiveBinding
import com.scp.wallet.exceptions.WalletLockedException
import com.scp.wallet.exceptions.WrongWalletPasswordException
import com.scp.wallet.scp.UnlockHash
import com.scp.wallet.ui.Popup
import com.scp.wallet.ui.QRcode
import com.scp.wallet.wallet.Wallet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReceiveActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReceiveBinding
    private lateinit var wallet: Wallet

    private val receiveViewModel: ReceiveViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiveBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.customActionBar.actionBarTitle.text = getString(R.string.activity_title_receive)

        val walletId = intent.getStringExtra(WalletsActivity.IE_WALLET_ID)
        val walletPwd = intent.getByteArrayExtra(WalletsActivity.IE_WALLET_PWD)

        if(walletId != null) {

            loadReceiveWallet(walletId, walletPwd)

            initViews()
            initObservers()
            initListeners()

        } else {
            setResult(RESULT_CANCELED, Intent())
            finish()
        }

    }

    private fun loadReceiveWallet(walletId: String, walletPwd: ByteArray?) {

        wallet = Wallet(walletId, this)
        walletPwd?.let {
            try {
                wallet.unlockWithKey(it)
            } catch (e: WrongWalletPasswordException) {
                wallet.lock()
            }
        }

        val walletKeys = wallet.getKeys()
        if(walletKeys.isEmpty()) {
            newAddress {
                setResult(RESULT_CANCELED, Intent())
                finish()
            }
        } else {
            val lastWalletAddress = UnlockHash.fromUnlockConditionsToAddress(walletKeys[walletKeys.size-1].unlockConditions)
            receiveViewModel.address.value = lastWalletAddress
        }


    }

    private fun initViews() {

        val walletTitle = getString(R.string.textview_wallet_name, wallet.name)
        binding.receiveWalletName.text = walletTitle

    }

    private fun initObservers() {

        receiveViewModel.address.observe(this) { address ->
            showAddress(address)
        }

    }

    private fun initListeners() {

        binding.receiveCopyButton.setOnClickListener {
            val clipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData = ClipData.newPlainText(getString(R.string.scp_address), receiveViewModel.address.value)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.address_copied), Toast.LENGTH_SHORT).show()
        }

        binding.receiveShareButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, receiveViewModel.address.value)
            startActivity(Intent.createChooser(intent, getString(R.string.share_using)))
        }

        binding.receiveNewAddressButton.setOnClickListener {
            newAddress()
        }

        binding.customActionBar.actionBarBack.setOnClickListener {
            onBackPressed()
        }

    }

    override fun onBackPressed() {
        val returnIntent = Intent()
        returnIntent.putExtra(WalletsActivity.IE_WALLET_ID, wallet.id)
        returnIntent.putExtra(WalletsActivity.IE_WALLET_PWD, wallet.getPassword())
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    private fun activateNewAddressButtonWithDelay() {
        lifecycleScope.launch {
            delay(1000)
            runOnUiThread {
                binding.receiveNewAddressButton.isEnabled = true
            }
        }
    }

    private fun showAddress(address: String) {

        val qrCode = QRcode.create(address)
        binding.receiveImageQR.setImageBitmap(qrCode)
        binding.receiveAddress.text = address

    }

    private fun newAddress(callbackFail: (() -> Unit)? = null) {
        binding.receiveNewAddressButton.isEnabled = false
        try {
            receiveViewModel.address.value = wallet.newAddress()
            activateNewAddressButtonWithDelay()
        } catch (e: WalletLockedException) {
            Popup.showUnlockWallet(wallet, this) { result ->
                if(result) {
                    receiveViewModel.address.value = wallet.newAddress()
                    activateNewAddressButtonWithDelay()
                } else {
                    activateNewAddressButtonWithDelay()
                    if(callbackFail != null) callbackFail()
                }
            }
        }
    }

}