package com.p2p.wallet.history.ui.main.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.p2p.wallet.databinding.ItemProgressBinding

class ProgressViewHolder(
    binding: ItemProgressBinding
) : RecyclerView.ViewHolder(binding.root) {

    constructor(parent: ViewGroup) : this(
        ItemProgressBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
}