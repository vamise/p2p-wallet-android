package com.p2p.wallet.dashboard.model.local

import com.github.mikephil.charting.data.PieEntry

data class YourWallets(
    val wallets: List<Token>,
    val balance: Double,
    val mainWallets: List<Token>,
    val pieChartList: List<PieEntry>
)