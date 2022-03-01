package com.scp.wallet.activities.donations

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.scp.wallet.R
import com.scp.wallet.activities.wallets.WalletsActivity
import com.scp.wallet.databinding.ActivityDonationsBinding

class DonationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDonationsBinding

    companion object {
        const val DONATION_ADDRESS_SCP = "71b797c650193b125cd0042dd8ab0be9e4f549537bc061a17ce3dddca8983938b401c4b92b0c"
        const val DONATION_ADDRESS_BTC = "12EVpv75KnDKPuKuGgxedw3ad8VbR5mc8e"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.customActionBar.actionBarTitle.text = getString(R.string.activity_title_donations)

        initViews()
        initListeners()

    }

    private fun initViews() {

        binding.donationsSCPaddress.text = DONATION_ADDRESS_SCP
        binding.donationsBTCaddress.text = DONATION_ADDRESS_BTC

        if(intent.getStringExtra(WalletsActivity.IE_WALLET_ID) == null) {
            binding.donationsSCPwallet.visibility = View.GONE
        }

    }

    private fun initListeners() {

        binding.donationsSCPAddressCopy.setOnClickListener {
            val clipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData = ClipData.newPlainText(getString(R.string.scp_address), DONATION_ADDRESS_SCP)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.address_copied), Toast.LENGTH_SHORT).show()
        }

        binding.donationsBTCAddressCopy.setOnClickListener {
            val clipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData = ClipData.newPlainText(getString(R.string.scp_address), DONATION_ADDRESS_BTC)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.address_copied), Toast.LENGTH_SHORT).show()
        }

        binding.donationsSCPwallet.setOnClickListener {
            setResult(RESULT_OK, Intent())
            finish()
        }

        binding.customActionBar.actionBarBack.setOnClickListener {
            onBackPressed()
        }

    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED, Intent())
        finish()
    }

}



