package com.scp.wallet.activities.send

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.scp.wallet.activities.scan.ScanActivity
import com.scp.wallet.activities.wallets.WalletsActivity
import com.scp.wallet.databinding.ActivitySendBinding
import com.scp.wallet.exceptions.InvalidUnlockHashException
import com.scp.wallet.scp.CurrencyValue
import com.scp.wallet.scp.UnlockHash
import com.scp.wallet.ui.Popup
import com.scp.wallet.wallet.Wallet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigInteger

class SendActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendBinding

    private lateinit var resultRequestScan: ActivityResultLauncher<Intent>
    private lateinit var wallet: Wallet

    private var scpFiat: Double? = null
    private var currencyScp = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.customActionBar.actionBarTitle.text = "Send SCP"

        val walletId = intent.getStringExtra(WalletsActivity.IE_WALLET_ID)
        val walletPwd = intent.getByteArrayExtra(WalletsActivity.IE_WALLET_PWD)

        if(walletId != null && walletPwd != null) {

            wallet = Wallet(walletId, this)
            wallet.unlockWithKey(walletPwd)

            val value = intent.getDoubleExtra(IE_SCP_FIAT, -1.0)
            scpFiat = if(value <= 0) null else value

            initResultRequests()
            initViews()
            initListeners()

        } else {
            setResult(RESULT_CANCELED, Intent())
            finish()
        }

    }

    private fun initViews() {

        if(scpFiat == null) binding.sendAmountOtherCurrency.visibility = View.GONE

        intent.getStringExtra(ScanActivity.IE_ADDRESS)?.let { prefilledAddress ->
            binding.sendAddress.setText(prefilledAddress)
        }

        intent.getStringExtra(IE_TRANSACTION_FEE)?.let { fee ->
            val minersFee = CurrencyValue(BigInteger(fee))
            val feeText = "Miners fee ${minersFee.toScpReadable()}"
            binding.sendTransactionFees.text = feeText
        }

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

            var insertedValue = CurrencyValue(BigInteger.ZERO)
            val value = (binding.sendAmount.text.toString().toDoubleOrNull() ?: 0.0)
            if(currencyScp) {
                insertedValue = CurrencyValue.initFromDouble(value)
            } else {
                scpFiat?.let { scpFiatValue ->
                    insertedValue = CurrencyValue.initFromDouble(value*scpFiatValue)
                }
            }

            val walletBalance = wallet.getBalance()
            if(insertedValue.value <= BigInteger.ZERO) {
                binding.sendAmount.setText("")
                Toast.makeText(this, "Invalid amount, no transaction sent", Toast.LENGTH_SHORT).show()
            } else if(insertedValue.value > walletBalance.value) {
                Popup.showSimple("Not enough funds", "Cannot send ${insertedValue.toScpReadable()}. The balance for this wallet is ${walletBalance.toScpReadable()}", this)
            } else {
                val insertedAddress = binding.sendAddress.text.toString().lowercase().replace("[^0-9a-f]".toRegex(), "")
                if(insertedAddress == "") {
                    Toast.makeText(this, "No address inserted", Toast.LENGTH_SHORT).show()
                } else {

                    try {
                        val unlockHash = UnlockHash.fromString(insertedAddress)

                        binding.sendButton.isEnabled = false

                        //TODO add switch to UI for feeIncluded
                        wallet.send(insertedValue, unlockHash, false, {
                            binding.sendAmount.setText("")
                            binding.sendAddress.setText("")
                            Popup.showSimple("Transaction sent", "The transaction has been successfully sent.", this) {
                                onBackPressed()
                            }
                        }, {
                            Popup.showSimple("Transaction not sent", it, this)
                            activateSendButtonWithDelay()
                        })

                    } catch (e: InvalidUnlockHashException) {
                        Popup.showSimple("Invalid address", "${e.message}", this)
                        activateSendButtonWithDelay()
                    }
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
        binding.sendAmountCurrency.text = if(currencyScp) "SCP" else "$"
        updateOtherCurrencyValue()

    }

    private fun updateOtherCurrencyValue() {
        scpFiat?.let { scpFiatValue ->
            val value = binding.sendAmount.text.toString().toDoubleOrNull() ?: 0.0
            if(currencyScp) {
                val usdValue = "\$${"%.2f".format(value*scpFiatValue).replace(".00", "")}"
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
        const val IE_TRANSACTION_FEE = "transaction-fee"
    }

}