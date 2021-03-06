package com.scp.wallet.activities.wallets

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.scp.wallet.R
import com.scp.wallet.activities.donations.DonationsActivity
import com.scp.wallet.activities.launch.LaunchActivity
import com.scp.wallet.activities.receive.ReceiveActivity
import com.scp.wallet.activities.scan.ScanActivity
import com.scp.wallet.activities.send.SendActivity
import com.scp.wallet.activities.send.SendActivity.Companion.IE_TRANSACTION_FEE
import com.scp.wallet.activities.settings.SettingsActivity
import com.scp.wallet.activities.walletsettings.WalletSettingsActivity
import com.scp.wallet.databinding.ActivityWalletsBinding
import com.scp.wallet.ui.Popup
import com.scp.wallet.utils.Currency
import com.scp.wallet.wallet.Wallet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigInteger

class WalletsActivity : AppCompatActivity(), WalletSettingsOpener {

    private lateinit var binding: ActivityWalletsBinding

    private lateinit var resultRequestScan: ActivityResultLauncher<Intent>
    private lateinit var resultRequestWalletSettings: ActivityResultLauncher<Intent>
    private lateinit var resultRequestSettings: ActivityResultLauncher<Intent>
    private lateinit var resultRequestReceive: ActivityResultLauncher<Intent>
    private lateinit var resultRequestSend: ActivityResultLauncher<Intent>
    private lateinit var resultRequestDonations: ActivityResultLauncher<Intent>

    private val walletsViewModel: WalletsViewModel by viewModels()

    private val walletsAdapter = WalletsAdapter()
    private val transactionsAdapter = TransactionsAdapter()
    private val walletsLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    private val transactionsLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

    private val refreshIntervalSeconds = 30L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWalletsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.customActionBar.actionBarTitle.text = getString(R.string.activity_title_wallets)
        binding.customActionBar.actionBarBack.visibility = View.GONE
        binding.customActionBar.actionBarExtra.setImageResource(R.drawable.ic_settings)
        binding.customActionBar.actionBarExtra.visibility = View.VISIBLE
        binding.customActionBar.actionBarExtra2.setImageResource(R.drawable.ic_code)
        binding.customActionBar.actionBarExtra2.visibility = View.VISIBLE

        walletsViewModel.updateScprimeData()

        initResultRequests()
        initViews()
        initObservers()
        initListeners()

        lifecycleScope.launch {
            lifecycleScope.launchWhenResumed {
                while(true) {
                    delay(refreshIntervalSeconds*1000)
                    refresh()
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()

        walletsViewModel.updateWallets()
        updateTransactionsUI()

    }

    private fun initViews() {

        binding.walletsRecycler.apply {
            adapter = walletsAdapter
            layoutManager = walletsLayoutManager
            PagerSnapHelper().attachToRecyclerView(this)
            (this.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }

        binding.walletTransactionsRecycler.apply {
            adapter = transactionsAdapter
            layoutManager = transactionsLayoutManager
        }

    }

    private fun initObservers() {

        walletsViewModel.wallets.observe(this) {
            walletsAdapter.submitList(it)
        }

        walletsViewModel.transactions.observe(this) {

            val currentIndex = walletsLayoutManager.findFirstVisibleItemPosition()
            val currentWallet = walletsAdapter.currentList.getOrNull(currentIndex)
            if(currentWallet != null && it.first?.id == currentWallet.id) {

                transactionsAdapter.submitList(it.second)

                binding.walletTransactionsRecycler.post {
                    val walletIndex = walletsLayoutManager.findFirstVisibleItemPosition()

                    if(it.second.size == 0 && walletsViewModel.wallets.value?.size != walletIndex) {
                        binding.walletsNoTransactions.visibility = View.VISIBLE
                        binding.walletsNoTransactions.animate().alpha(1f)
                        binding.walletTransactionsRecycler.animate().alpha(0f)
                    } else {
                        walletsAdapter.notifyItemChanged(walletIndex)
                        binding.walletsNoTransactions.animate().alpha(0f).withEndAction {
                            binding.walletsNoTransactions.visibility = View.GONE
                            binding.walletTransactionsRecycler.animate().alpha(1f)
                        }
                    }
                }

            }
        }

        walletsViewModel.consensusHeight.observe(this) {
            walletsViewModel.transactions.value?.let { transactions ->
                walletsViewModel.transactionsBlocksPassed(transactions.second)
                walletsViewModel.transactions.value = transactions
            }
        }

        walletsViewModel.scpExchangeRates.observe(this) {
            walletsViewModel.updateWalletsFiatBalance()
            walletsViewModel.wallets.value?.let { wallets ->
                walletsAdapter.notifyItemRangeChanged(0, wallets.size)
            }
        }

        walletsViewModel.currency.observe(this) {
            walletsViewModel.updateWalletsFiatBalance()
            walletsViewModel.wallets.value?.let { wallets ->
                walletsAdapter.notifyItemRangeChanged(0, wallets.size)
            }
        }

    }

    private fun initListeners() {

        binding.walletsReceiveButton.setOnClickListener {

            walletsAdapter.currentList.getOrNull(walletsLayoutManager.findFirstVisibleItemPosition())?.let { selectedWallet ->
                val i = Intent(this, ReceiveActivity::class.java)
                i.putExtra(IE_WALLET_ID, selectedWallet.id)
                i.putExtra(IE_WALLET_PWD, selectedWallet.getPassword())
                resultRequestReceive.launch(i)
            }

        }
        binding.walletsSendButton.setOnClickListener {

            walletsAdapter.currentList.getOrNull(walletsLayoutManager.findFirstVisibleItemPosition())?.let { selectedWallet ->
                if(selectedWallet.getSeed() == null) {
                    Popup.showUnlockWallet(selectedWallet, this) { result ->
                        if(result) {
                            openWalletSend(selectedWallet)
                        }
                    }
                } else {
                    openWalletSend(selectedWallet)
                }
            }

        }
        binding.walletsRecycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val offset = recyclerView.computeHorizontalScrollOffset()
                if(recyclerView.childCount > 0 && offset % recyclerView.getChildAt(0).measuredWidth == 0) {
                    updateTransactionsUI()
                }

            }

        })

        binding.floatingScan.setOnClickListener {

            val i = Intent(this,  ScanActivity::class.java)
            resultRequestScan.launch(i)

        }

        binding.customActionBar.actionBarExtra.setOnClickListener {

            val i = Intent(this, SettingsActivity::class.java)
            resultRequestSettings.launch(i)

        }

        binding.customActionBar.actionBarExtra2.setOnClickListener {

            val i = Intent(this, DonationsActivity::class.java)
            val currentWallet = walletsAdapter.currentList.getOrNull(walletsLayoutManager.findFirstVisibleItemPosition())
            if(currentWallet != null) {
                i.putExtra(IE_WALLET_ID, currentWallet.id)
            }
            resultRequestDonations.launch(i)

        }

        binding.walletTransactionsRefresh.setOnClickListener {
            refresh()
        }

    }

    private fun refresh() {

        val currentIndex = walletsLayoutManager.findFirstVisibleItemPosition()
        val currentWallet = walletsAdapter.currentList.getOrNull(currentIndex)
        if (currentWallet != null) {

            val anim = ObjectAnimator.ofFloat(binding.walletTransactionsRefresh, "rotation", 0f, 360f).apply {
                duration = 500
                repeatMode = ValueAnimator.RESTART
            }
            anim.repeatCount = Animation.INFINITE
            anim.start()
            binding.walletTransactionsRefresh.isClickable = false

            walletsViewModel.updateTransactionsAndScprimeData(currentWallet) {
                anim.repeatCount = 0

                lifecycleScope.launch {
                    delay(1000)
                    runOnUiThread {
                        binding.walletTransactionsRefresh.isClickable = true
                    }
                }

            }

        }
    }

    private fun initResultRequests() {

        resultRequestWalletSettings = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { intent ->
                    intent.getStringExtra(IE_WALLET_ID)?.let { id ->
                        walletsViewModel.updateWallet(id, intent.getByteArrayExtra(IE_WALLET_PWD))
                        walletsAdapter.notifyItemChanged(walletsLayoutManager.findFirstVisibleItemPosition())
                    }
                }
            }
        }

        resultRequestSettings = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val newCurrency = getSharedPreferences(LaunchActivity.SP_FILE_SETTINGS, MODE_PRIVATE).getString(LaunchActivity.SP_CURRENCY, Currency.DEFAULT_CURRENCY)
                if(newCurrency != walletsViewModel.currency.value) {
                    walletsViewModel.currency.value = newCurrency
                }
            }
        }

        resultRequestReceive = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { intent ->
                    intent.getStringExtra(IE_WALLET_ID)?.let { id ->
                        walletsViewModel.updateWallet(id, intent.getByteArrayExtra(IE_WALLET_PWD))
                        walletsAdapter.notifyItemChanged(walletsLayoutManager.findFirstVisibleItemPosition())
                        transactionsLayoutManager.smoothScrollToPosition(binding.walletTransactionsRecycler, null, 0)
                    }
                }
            }
        }

        resultRequestSend = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { intent ->
                    intent.getStringExtra(IE_WALLET_ID)?.let { id ->
                        walletsViewModel.updateWallet(id, null)
                        walletsAdapter.notifyItemChanged(walletsLayoutManager.findFirstVisibleItemPosition())
                        transactionsLayoutManager.smoothScrollToPosition(binding.walletTransactionsRecycler, null, 0)
                    }
                }
            }
        }

        resultRequestScan = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { intent ->
                    intent.getStringExtra(ScanActivity.IE_ADDRESS)?.let { address ->

                        walletsAdapter.currentList.getOrNull(walletsLayoutManager.findFirstVisibleItemPosition())?.let { selectedWallet ->
                            if(selectedWallet.getSeed() == null) {
                                Popup.showUnlockWallet(selectedWallet, this) { resultUnlock ->
                                    if(resultUnlock) {
                                        walletsAdapter.notifyItemChanged(walletsLayoutManager.findFirstVisibleItemPosition())
                                        openWalletSend(selectedWallet, address)
                                    }
                                }
                            } else {
                                openWalletSend(selectedWallet, address)
                            }
                        }

                    }
                }
            }
        }

        resultRequestDonations = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val currentWallet = walletsAdapter.currentList.getOrNull(walletsLayoutManager.findFirstVisibleItemPosition())
                if(currentWallet != null) {

                    if(currentWallet.getBalance().value == BigInteger.ZERO) {
                        Popup.showSimple(getString(R.string.popup_title_empty_wallet), getString(R.string.popup_description_empty_wallet), this)
                    } else {
                        if(currentWallet.getSeed() == null) {
                            Popup.showUnlockWallet(currentWallet, this) { resultUnlock ->
                                if(resultUnlock) {
                                    openWalletSend(currentWallet, DonationsActivity.DONATION_ADDRESS_SCP)
                                }
                            }
                        } else {
                            openWalletSend(currentWallet, DonationsActivity.DONATION_ADDRESS_SCP)
                        }
                    }

                } else {
                    Popup.showSimple(getString(R.string.popup_title_donation_no_wallet), getString(R.string.popup_description_donation_no_wallet), this)
                }
            }
        }

    }

    private fun updateTransactionsUI() {
        walletsViewModel.wallets.value?.let { wallets ->
            if(walletsLayoutManager.findFirstVisibleItemPosition() == wallets.size) {
                toggleAddWalletUI(true)
            } else {

                val currentIndex = walletsLayoutManager.findFirstVisibleItemPosition()
                val currentWallet = walletsAdapter.currentList.getOrNull(currentIndex)
                if (currentWallet != null) {
                    walletsViewModel.updateTransactions(currentWallet)
                }
                toggleAddWalletUI(false)

            }
        }
    }

    private fun toggleAddWalletUI(addingWallet: Boolean) {
        binding.walletsReceiveButton.isEnabled = !addingWallet
        binding.walletsSendButton.isEnabled = !addingWallet

        val animTranslationDistance = 50f
        if(addingWallet) {
            binding.walletsReceiveButtonText.animate().alpha(0.5f)
            binding.walletsReceiveButtonIcon.animate().alpha(0.5f)
            binding.walletsSendButtonText.animate().alpha(0.5f)
            binding.walletsSendButtonIcon.animate().alpha(0.5f)
            binding.walletsTransactionTitle.animate().translationX(-animTranslationDistance).alpha(0f).withEndAction {
                binding.walletsTransactionTitle.visibility = View.GONE
            }
            binding.walletTransactionsRecycler.animate().translationX(-animTranslationDistance).alpha(0f).withEndAction {
                binding.walletTransactionsRecycler.visibility = View.GONE
            }
            binding.floatingScan.animate().translationX(-animTranslationDistance).alpha(0f).withEndAction {
                binding.floatingScan.visibility = View.GONE
            }
            binding.floatingScanText.animate().translationX(-animTranslationDistance).alpha(0f).withEndAction {
                binding.floatingScanText.visibility = View.GONE
            }
            binding.floatingScanIcon.animate().translationX(-animTranslationDistance).alpha(0f).withEndAction {
                binding.floatingScanIcon.visibility = View.GONE
            }
            binding.walletsNoTransactions.animate().translationX(-animTranslationDistance).alpha(0f).withEndAction {
                binding.floatingScan.visibility = View.GONE
            }
            binding.walletTransactionsRefresh.animate().translationX(-animTranslationDistance).alpha(0f).withEndAction {
                binding.floatingScan.visibility = View.GONE
            }
        } else {
            binding.walletsReceiveButtonText.animate().alpha(1f)
            binding.walletsReceiveButtonIcon.animate().alpha(1f)
            binding.walletsSendButtonText.animate().alpha(1f)
            binding.walletsSendButtonIcon.animate().alpha(1f)
            binding.floatingScan.visibility = View.VISIBLE
            binding.floatingScanText.visibility = View.VISIBLE
            binding.floatingScanIcon.visibility = View.VISIBLE
            binding.walletsTransactionTitle.visibility = View.VISIBLE
            binding.walletTransactionsRecycler.visibility = View.VISIBLE
            binding.floatingScan.animate().translationX(0f).alpha(1f)
            binding.floatingScanText.animate().translationX(0f).alpha(1f)
            binding.floatingScanIcon.animate().translationX(0f).alpha(1f)
            binding.walletsTransactionTitle.animate().translationX(0f).alpha(1f)
            binding.walletTransactionsRecycler.animate().translationX(0f).alpha(1f)
            binding.walletsNoTransactions.animate().translationX(0f).alpha(1f)
            binding.walletTransactionsRefresh.animate().translationX(0f).alpha(1f)
        }
    }

    override fun openWalletSettings(w: Wallet) {

        val i = Intent(binding.root.context, WalletSettingsActivity::class.java)
        i.putExtra(IE_WALLET_ID, w.id)
        i.putExtra(IE_WALLET_PWD, w.getPassword())
        resultRequestWalletSettings.launch(i)

    }

    private fun openWalletSend(w: Wallet, prefillAddress: String? = null) {

        val i = Intent(this, SendActivity::class.java)
        i.putExtra(IE_WALLET_ID, w.id)
        i.putExtra(IE_WALLET_PWD, w.getPassword())
        walletsViewModel.currency.value?.let { currency ->
            walletsViewModel.scpExchangeRates.value?.get(currency)?.let { scpPrice ->
                if(scpPrice > 0) {
                    i.putExtra(SendActivity.IE_SCP_FIAT, scpPrice)
                    i.putExtra(SendActivity.IE_SCP_FIAT_SYMBOL, Currency.getSymbol(currency))
                }
            }
        }
        walletsViewModel.transactionFee.value?.let { transactionFee ->
            i.putExtra(IE_TRANSACTION_FEE, transactionFee.value.toString())
        }
        if(prefillAddress != null) {
            i.putExtra(ScanActivity.IE_ADDRESS, prefillAddress)
        }
        resultRequestSend.launch(i)

    }

    companion object {
        const val SP_WALLETS_IDS = "wallets-ids"
        const val IE_WALLET_ID = "wallet-id"
        const val IE_WALLET_PWD = "wallet-pwd"
    }

}