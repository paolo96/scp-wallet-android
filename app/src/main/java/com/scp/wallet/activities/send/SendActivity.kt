package com.scp.wallet.activities.send

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.scp.wallet.R
import com.scp.wallet.activities.scan.ScanActivity
import com.scp.wallet.activities.wallets.WalletsActivity
import com.scp.wallet.databinding.ActivitySendBinding
import com.scp.wallet.exceptions.InvalidUnlockHashException
import com.scp.wallet.exceptions.WrongWalletPasswordException
import com.scp.wallet.scp.CurrencyValue
import com.scp.wallet.scp.UnlockHash
import com.scp.wallet.ui.Popup
import com.scp.wallet.utils.Android
import com.scp.wallet.wallet.Wallet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigInteger


class SendActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendBinding

    private lateinit var resultRequestScan: ActivityResultLauncher<Intent>
    private lateinit var wallet: Wallet

    private var scpFiat: Double? = null
    private var fiatSymbol: String? = null
    private var currencyScp = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.customActionBar.actionBarTitle.text = getString(R.string.activity_title_send)

        val walletId = intent.getStringExtra(WalletsActivity.IE_WALLET_ID)
        val walletPwd = intent.getByteArrayExtra(WalletsActivity.IE_WALLET_PWD)

        if(walletId != null && walletPwd != null) {

            wallet = Wallet(walletId, this)
            try {
                wallet.unlockWithKey(walletPwd)
            } catch (e: WrongWalletPasswordException) {
                setResult(RESULT_CANCELED, Intent())
                finish()
            }

            val value = intent.getDoubleExtra(IE_SCP_FIAT, -1.0)
            scpFiat = if(value <= 0) null else value
            fiatSymbol = intent.getStringExtra(IE_SCP_FIAT_SYMBOL)

            initResultRequests()
            initViews()
            initListeners()

        } else {
            setResult(RESULT_CANCELED, Intent())
            finish()
        }

    }

    private fun initViews() {

        if(scpFiat == null) binding.sendAmountOtherCurrency.visibility = View.GONE else {
            val amountText = "${fiatSymbol}0"
            binding.sendAmountOtherCurrency.text = amountText
        }

        intent.getStringExtra(ScanActivity.IE_ADDRESS)?.let { prefilledAddress ->
            binding.sendAddress.setText(prefilledAddress)
        }

        intent.getStringExtra(IE_TRANSACTION_FEE)?.let { fee ->
            val minersFee = CurrencyValue(BigInteger(fee))
            val feeText = getString(R.string.textview_miner_fees, minersFee.toScpReadable())
            binding.sendTransactionFees.text = feeText
        }

        val balancePrefix = "${getString(R.string.textview_wallet_name, wallet.name)}: "
        val balanceValue = wallet.getBalance().toScpReadable()
        val spannable = SpannableString(balancePrefix+balanceValue)
        spannable.setSpan(ForegroundColorSpan(resources.getColor(R.color.blue_scp, theme)), balancePrefix.length, (balancePrefix + balanceValue).length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        binding.sendWalletBalance.setText(spannable, TextView.BufferType.SPANNABLE)

    }

    private fun initListeners() {

        binding.customActionBar.actionBarBack.setOnClickListener {
            setResult(RESULT_CANCELED, Intent())
            finish()
        }

        binding.sendAmountChangeCurrency.setOnClickListener {
            changeCurrency()
        }

        binding.sendAmountCurrency.setOnClickListener {
            changeCurrency()
        }

        binding.sendAddressScan.setOnClickListener {
            val i = Intent(this, ScanActivity::class.java)
            resultRequestScan.launch(i)
        }

        binding.sendWalletBalance.setOnClickListener {
            val fee = intent.getStringExtra(IE_TRANSACTION_FEE)
            val maxAmount = if(fee != null) {
                wallet.getBalance() - CurrencyValue(BigInteger(fee))
            } else {
                wallet.getBalance()
            }
            if(maxAmount.value <= BigInteger.ZERO) {
                Popup.showSimple(getString(R.string.popup_title_insufficient_funds), getString(R.string.popup_description_insufficient_funds_fees), this)
            } else {
                binding.sendAmount.setText(maxAmount.significantValueString())
            }
        }

        binding.sendAmount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateOtherCurrencyValue()
            }
        })

        binding.sendButton.setOnClickListener {

            if(Android.isEmulator()) {
                Popup.showChoice(getString(R.string.popup_title_emulator_warning), getString(R.string.popup_description_emulator_warning), this) {
                    if(it) {
                        send()
                    }
                }
            } else {
                send()
            }

        }

    }

    private fun send() {

        var insertedValue = CurrencyValue(BigInteger.ZERO)
        val value = (binding.sendAmount.text.toString().toDoubleOrNull() ?: 0.0)
        if(currencyScp) {
            insertedValue = CurrencyValue.initFromDouble(value)
        } else {
            scpFiat?.let { scpFiatValue ->
                insertedValue = CurrencyValue.initFromDouble(value/scpFiatValue)
            }
        }

        val walletBalance = wallet.getBalance()
        if(insertedValue.value <= BigInteger.ZERO) {
            binding.sendAmount.setText("")
            Toast.makeText(this, getString(R.string.toast_invalid_amount), Toast.LENGTH_SHORT).show()
        } else if(insertedValue.value > walletBalance.value) {
            Popup.showSimple(getString(R.string.popup_title_insufficient_funds), getString(R.string.popup_description_insufficient_funds, insertedValue.toScpReadable(), walletBalance.toScpReadable()), this)
        } else {
            val insertedAddress = binding.sendAddress.text.toString().lowercase().replace("[^0-9a-f]".toRegex(), "")
            if(insertedAddress == "") {
                Toast.makeText(this, getString(R.string.toast_empty_address), Toast.LENGTH_SHORT).show()
            } else {

                try {
                    val unlockHash = UnlockHash.fromString(insertedAddress)

                    binding.sendButton.isEnabled = false

                    //TODO add switch to UI for feeIncluded
                    wallet.send(insertedValue, unlockHash, false, {
                        binding.sendAmount.setText("")
                        binding.sendAddress.setText("")
                        Popup.showSimple(getString(R.string.popup_title_transaction_sent), getString(R.string.popup_description_transaction_sent), this) {
                            onBackPressed()
                        }
                    }, {
                        Popup.showSimple(getString(R.string.popup_title_transaction_not_sent), getString(R.string.popup_description_transaction_not_sent, it), this)
                        activateSendButtonWithDelay()
                    })

                } catch (e: InvalidUnlockHashException) {
                    Popup.showSimple(getString(R.string.popup_title_invalid_address), getString(R.string.popup_description_invalid_address, e.message), this)
                    activateSendButtonWithDelay()
                }
            }
        }

    }

    private fun initResultRequests() {

        resultRequestScan = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { intent ->
                    intent.getStringExtra(ScanActivity.IE_ADDRESS)?.let { address ->
                        binding.sendAddress.setText(address)
                    }
                }
            }
        }

    }

    private fun changeCurrency() {

        currencyScp = !currencyScp

        val value = (binding.sendAmount.text.toString().toDoubleOrNull() ?: 0.0)
        scpFiat?.let { scpFiatValue ->
            if(currencyScp) {
                val scpValue = "%.6f".format(value / scpFiatValue)
                binding.sendAmount.setText(scpValue)
            } else {
                val usdValue = "%.2f".format(value*scpFiatValue)
                binding.sendAmount.setText(usdValue)
            }
        }
        binding.sendAmountCurrency.text = if(currencyScp) "SCP" else fiatSymbol
        updateOtherCurrencyValue()

    }

    private fun updateOtherCurrencyValue() {
        scpFiat?.let { scpFiatValue ->
            val value = binding.sendAmount.text.toString().toDoubleOrNull() ?: 0.0
            if(currencyScp) {
                val usdValue = "${fiatSymbol}${"%.2f".format(value*scpFiatValue).replace(".00", "")}"
                binding.sendAmountOtherCurrency.text = usdValue
            } else {
                val scpValue = CurrencyValue.initFromDouble(value/scpFiatValue).toScpReadable()
                binding.sendAmountOtherCurrency.text = scpValue
            }
        }
    }

    private fun activateSendButtonWithDelay() {
        lifecycleScope.launch {
            delay(1000)
            runOnUiThread {
                binding.sendButton.isEnabled = true
            }
        }
    }

    override fun onBackPressed() {
        val returnIntent = Intent()
        returnIntent.putExtra(WalletsActivity.IE_WALLET_ID, wallet.id)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    companion object {
        const val IE_SCP_FIAT = "scp-fiat"
        const val IE_SCP_FIAT_SYMBOL = "scp-fiat-symbol"
        const val IE_TRANSACTION_FEE = "transaction-fee"
    }

}