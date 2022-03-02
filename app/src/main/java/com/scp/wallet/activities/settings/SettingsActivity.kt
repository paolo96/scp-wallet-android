package com.scp.wallet.activities.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
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
        binding.customActionBar.actionBarTitle.text = getString(R.string.activity_title_settings)

        initViews()
        initListeners()

    }

    private fun initViews() {

        binding.settingsServer.setText(API.host)

        ArrayAdapter(this, R.layout.spinner_item_selected, Currency.getCurrencies()).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_item)
            binding.spinnerCurrency.adapter = adapter

            val currCurrency = getSharedPreferences(LaunchActivity.SP_FILE_SETTINGS, MODE_PRIVATE).getString(LaunchActivity.SP_CURRENCY, Currency.DEFAULT_CURRENCY)
            val currCurrencyIndex = Currency.getCurrencies().indexOfFirst { it == currCurrency }
            if(currCurrencyIndex != -1) {
                binding.spinnerCurrency.setSelection(currCurrencyIndex)
            }
        }

    }

    private fun initListeners() {

        binding.settingsSave.setOnClickListener {

            val sp = getSharedPreferences(LaunchActivity.SP_FILE_SETTINGS, MODE_PRIVATE)
            val selectedCurrency = binding.spinnerCurrency.selectedItem.toString()
            val newHost = binding.settingsServer.text.toString()

            if(sp.getString(LaunchActivity.SP_CURRENCY, Currency.DEFAULT_CURRENCY) != selectedCurrency) {
                sp.edit().putString(LaunchActivity.SP_CURRENCY, selectedCurrency).apply()
            }

            if(newHost != API.host) {
                Popup.showChoice(getString(R.string.popup_title_change_server), getString(R.string.popup_description_change_server), this) { result ->

                    if(result) {
                        sp.edit().putString(LaunchActivity.SP_HOST, newHost).apply()

                        val i = Intent(this, LaunchActivity::class.java)
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        this.startActivity(i)
                    }

                }
            } else {
                setResult(RESULT_OK, Intent())
                finish()
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