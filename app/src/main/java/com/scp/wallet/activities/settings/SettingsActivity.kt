package com.scp.wallet.activities.settings

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import com.scp.wallet.R
import com.scp.wallet.activities.launch.LaunchActivity
import com.scp.wallet.api.API
import com.scp.wallet.databinding.ActivitySettingsBinding
import com.scp.wallet.ui.Popup
import com.scp.wallet.utils.Currency

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.customActionBar.actionBarTitle.text = "Settings"

        initViews()
        initListeners()

    }

    private fun initViews() {

        binding.settingsServer.setText(API.host)

        ArrayAdapter(this, R.layout.spinner_item_selected, Currency.getCurrencies()).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_item)
            binding.spinnerCurrency.adapter = adapter
        }

    }

    private fun initListeners() {

        binding.settingsSave.setOnClickListener {
            val newHost = binding.settingsServer.text.toString()
            if(newHost != API.host) {
                Popup.showChoice("Do you want to change server?", "Make sure that you're using a trusted server or your own server. Although the server doesn't have access to the wallets seed, a malicious attacker could display false transactions data and cause severe consequences as a result. Furthermore your current wallets could be missing some transactions after this change, you should import them again after the change.", this) { result ->

                    if(result) {
                        val sp = getSharedPreferences(LaunchActivity.SP_FILE_SETTINGS, MODE_PRIVATE)
                        sp.edit().putString(LaunchActivity.SP_HOST, newHost).apply()

                        val i = Intent(this, LaunchActivity::class.java)
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        this.startActivity(i)
                    }

                }
            }
        }

        binding.customActionBar.actionBarBack.setOnClickListener {
            onBackPressed()
        }

        binding.settingsCode.setOnClickListener {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/paolo96/scp-wallet-android"))
            startActivity(i)
        }

    }

}