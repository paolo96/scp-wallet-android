package com.scp.wallet.activities.exportseed

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.scp.wallet.R
import com.scp.wallet.databinding.ActivityExportSeedBinding
import com.scp.wallet.ui.SeedInterface

class ExportSeedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExportSeedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportSeedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.customActionBar.actionBarTitle.text = getString(R.string.activity_title_export_seed)

        val seed = intent.getStringExtra(IE_SEED)
        if(seed != null) {

            SeedInterface.drawSeed(seed, binding.exportSeedContainer, this)
            initListeners()

        } else {
            onBackPressed()
        }

    }

    private fun initListeners() {

        binding.customActionBar.actionBarBack.setOnClickListener {
            onBackPressed()
        }

    }

    companion object {
        const val IE_SEED = "seed"
    }
}