package org.p2p.wallet.restore.ui.derivable

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.p2p.wallet.R
import org.p2p.wallet.databinding.ItemTokenSimpleBinding
import org.p2p.wallet.restore.model.DerivableAccount
import org.p2p.wallet.utils.Constants.SOL_SYMBOL
import org.p2p.wallet.utils.scaleShort

class DerivableAccountsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val data = mutableListOf<DerivableAccount>()

    fun setItems(new: List<DerivableAccount>) {
        data.clear()
        data.addAll(new)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        ViewHolder(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).onBind(data[position])
    }

    private class ViewHolder(
        binding: ItemTokenSimpleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        constructor(parent: ViewGroup) : this(
            binding = ItemTokenSimpleBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

        private val tokenImageView = binding.tokenImageView
        private val symbolTextView = binding.symbolTextView
        private val nameTextView = binding.nameTextView
        private val valueTextView = binding.valueTextView
        private val totalTextView = binding.totalTextView
        private val colorView = binding.colorView

        @SuppressLint("SetTextI18n")
        fun onBind(account: DerivableAccount) {
            tokenImageView.setImageResource(R.drawable.ic_sol)
            symbolTextView.text = SOL_SYMBOL
            nameTextView.text = cutAddress(account.account.publicKey.toBase58())
            valueTextView.text = "${account.totalInUsd.scaleShort()} $"
            totalTextView.text = "${account.total.toPlainString()} $SOL_SYMBOL"

            colorView.isVisible = false
        }

        @Suppress("MagicNumber")
        fun cutAddress(publicKey: String): String {
            val firstSix = publicKey.take(4)
            val lastFour = publicKey.takeLast(4)
            return "$firstSix...$lastFour"
        }
    }
}