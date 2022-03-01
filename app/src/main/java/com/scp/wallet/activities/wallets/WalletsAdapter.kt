package com.scp.wallet.activities.wallets

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.scp.wallet.R
import com.scp.wallet.activities.newwallet.NewWalletActivity
import com.scp.wallet.databinding.ListItemNewWalletBinding
import com.scp.wallet.databinding.ListItemWalletBinding
import com.scp.wallet.ui.Popup
import com.scp.wallet.utils.Currency
import com.scp.wallet.utils.Dates
import com.scp.wallet.wallet.Wallet

class WalletsAdapter : ListAdapter<Wallet, RecyclerView.ViewHolder>(WalletsDiffCallback()) {

    companion object {
        const val VIEW_WALLET = 0
        const val VIEW_NEW_WALLET = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if(position == this.itemCount-1) {
            VIEW_NEW_WALLET
        } else {
            VIEW_WALLET
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(position == this.itemCount-1) {
            (holder as ViewHolderNewWallet).bind()
        } else {
            val item = getItem(position)
            (holder as ViewHolderWallet).bind(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if(viewType == VIEW_NEW_WALLET) {
            ViewHolderNewWallet.from(parent)
        } else {
            ViewHolderWallet.from(parent)
        }
    }

    class ViewHolderWallet private constructor(private val binding: ListItemWalletBinding) : RecyclerView.ViewHolder(binding.root){

        private var currWallet: Wallet? = null

        fun bind(item: Wallet) {

            currWallet = item

            binding.itemWalletName.text = item.name
            val balance = item.getBalance()
            val balanceText = balance.toScpReadable()
            binding.itemWalletBalance.text = balanceText

            item.getFiatBalance()?.let { fiatBalance ->
                val fiatBalanceText = "${Currency.getSymbol(fiatBalance.first)}${"%.2f".format(fiatBalance.second).replace(".00", "")}"
                binding.itemWalletBalanceFiat.text = fiatBalanceText
            }

            if(item.getSeed() == null) {
                binding.itemWalletLock.setImageResource(R.drawable.ic_lock)
            } else {
                binding.itemWalletLock.setImageResource(R.drawable.ic_unlock)
            }

            val lastTransactionTime = item.getLastTransactionDate()
            if(lastTransactionTime == null) {
                binding.itemWalletTransaction.text = binding.root.resources.getString(R.string.none)
            } else {
                binding.itemWalletTransaction.text = Dates.timestampToReadable(lastTransactionTime)
            }

        }

        fun addClickListeners(context: Context) {

            binding.itemWalletLock.setOnClickListener {
                currWallet?.let { wallet ->
                    if(wallet.getSeed() == null) {
                        Popup.showUnlockWallet(wallet, context) { result ->
                            if(result) {
                                binding.itemWalletLock.setImageResource(R.drawable.ic_unlock)
                            }
                        }
                    } else {
                        wallet.lock()
                        binding.itemWalletLock.setImageResource(R.drawable.ic_lock)
                    }
                }

            }

            binding.itemWalletSettings.setOnClickListener {
                currWallet?.let { wallet ->
                    (context as WalletSettingsOpener).openWalletSettings(wallet)
                }
            }

        }

        companion object {
            fun from(parent: ViewGroup): ViewHolderWallet {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemWalletBinding.inflate(layoutInflater, parent, false)
                val vh = ViewHolderWallet(binding)
                vh.addClickListeners(parent.context)
                return vh
            }
        }

    }

    class ViewHolderNewWallet private constructor(private val binding: ListItemNewWalletBinding) : RecyclerView.ViewHolder(binding.root){

        fun bind() {
        }

        fun addClickListeners(context: Context) {
            binding.itemNewWalletButton.setOnClickListener {
                val i = Intent(context, NewWalletActivity::class.java)
                context.startActivity(i)
            }
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolderNewWallet {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemNewWalletBinding.inflate(layoutInflater, parent, false)
                val result = ViewHolderNewWallet(binding)
                result.addClickListeners(parent.context)
                return result
            }
        }

    }

}

class WalletsDiffCallback : DiffUtil.ItemCallback<Wallet>() {

    override fun areItemsTheSame(oldItem: Wallet, newItem: Wallet): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Wallet, newItem: Wallet): Boolean {
        return oldItem == newItem
    }

}