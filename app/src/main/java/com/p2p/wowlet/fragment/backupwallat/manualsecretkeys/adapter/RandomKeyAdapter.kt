package com.p2p.wowlet.fragment.backupwallat.manualsecretkeys.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.p2p.wowlet.databinding.ItemSecretKeyBinding
import com.p2p.wowlet.fragment.backupwallat.manualsecretkeys.viewmodel.ManualSecretKeyViewModel
import com.wowlet.entities.local.SecretKeyItem

class RandomKeyAdapter(
    private val viewModel: ManualSecretKeyViewModel,
    private var list: MutableList<SecretKeyItem>
) : RecyclerView.Adapter<RandomKeyAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {

        val bind = ItemSecretKeyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return MyViewHolder(bind)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.onBind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun updateData(secretKeyList: MutableList<SecretKeyItem>) {
        list.addAll(secretKeyList)
        notifyDataSetChanged()
    }

    fun hideItem(it: SecretKeyItem) {
        val itemPos = list.indexOf(it)
        if (itemPos > -1) {
            list.remove(it)
            notifyDataSetChanged()
        }
    }

    inner class MyViewHolder(
        val binding: ItemSecretKeyBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val phraseTextView = binding.phraseTextView

        @SuppressLint("SetTextI18n")
        fun onBind(item: SecretKeyItem) {
            phraseTextView.text = "${item.id}. ${item.value}"
            itemView.setOnClickListener { viewModel.randomItemClickListener(item) }
        }
    }
}