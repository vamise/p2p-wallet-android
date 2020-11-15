package com.p2p.wowlet.fragment.swap.adapter

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.p2p.wowlet.fragment.swap.viewmodel.SwapViewModel
import com.wowlet.entities.local.CoinItem

@BindingAdapter("adapter_list", "view_model")
fun RecyclerView.setAdapterList(
    data: List<CoinItem>?,
    viewModel: SwapViewModel
) {
    data?.let {
        adapter = YourWalletsAdapter(viewModel, it)
    }
    this.layoutManager = LinearLayoutManager(this.context)
}