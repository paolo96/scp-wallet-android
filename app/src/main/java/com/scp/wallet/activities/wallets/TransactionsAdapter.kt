package com.scp.wallet.activities.wallets

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.scp.wallet.R
import com.scp.wallet.databinding.ListItemTransactionBinding
import com.scp.wallet.scp.Transaction
import com.scp.wallet.utils.Dates
import java.math.BigInteger

import android.net.Uri
import com.google.gson.Gson


class TransactionsAdapter : ListAdapter<Transaction, TransactionsAdapter.ViewHolder>(TransactionsDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder private constructor(private val binding: ListItemTransactionBinding) : RecyclerView.ViewHolder(binding.root){

        private var transactionId: String? = null

        fun bind(item: Transaction) {

            transactionId = item.id

            val transactionTime = item.blockTimestamp
            if(transactionTime == null) {
                binding.itemTransactionTitle.text = "Unconfirmed"
            } else {
                binding.itemTransactionTitle.text = Dates.timestampToReadable(transactionTime)
            }

            item.walletValue?.let { tValue ->

                if(item.confirmationHeight == null) {
                    binding.itemTransactionDescription.text = "Waiting"
                    binding.itemTransactionValue.setTextColor(binding.root.resources.getColor(android.R.color.white, binding.root.context.theme))
                    binding.itemTransactionStatusImage.setImageResource(R.drawable.ic_transaction_pending)
                } else {
                    binding.itemTransactionDescription.text = getConfirms(item.confirmationBlocksPassed)
                    if(tValue.value < BigInteger.ZERO) {
                        binding.itemTransactionValue.setTextColor(binding.root.resources.getColor(R.color.red_negative, binding.root.context.theme))
                        binding.itemTransactionStatusImage.setImageResource(R.drawable.ic_transaction_unconfirmed)
                    } else {
                        binding.itemTransactionValue.setTextColor(binding.root.resources.getColor(R.color.green_negative, binding.root.context.theme))
                        binding.itemTransactionStatusImage.setImageResource(R.drawable.ic_transaction_confirmed)
                    }
                }

                binding.itemTransactionValue.text = tValue.toScpReadable()

            }

        }

        private fun getConfirms(confirms: Int?): String {
            return if(confirms != null) {
                if(confirms > 100) {
                    "100+ confirmations"
                } else if(confirms > 1) {
                    "$confirms confirmations"
                } else {
                    "One confirmation"
                }
            } else {
                "Confirmed"
            }
        }

        fun addClickListeners(context: Context) {

            binding.root.setOnClickListener {
                transactionId?.let{tId ->
                    val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://scprime.info/?search=$tId"))
                    context.startActivity(i)
                }
            }

        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemTransactionBinding.inflate(layoutInflater, parent, false)
                val vh = ViewHolder(binding)
                vh.addClickListeners(parent.context)
                return vh
            }
        }
    }
}

class TransactionsDiffCallback : DiffUtil.ItemCallback<Transaction>() {

    override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {

        if(oldItem.id != null && newItem.id != null) {
            return oldItem.id == newItem.id
        } else if(oldItem.id == null && newItem.id == null) {
            return oldItem == newItem
        }
        return false
    }

    override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return newItem == oldItem
    }

}