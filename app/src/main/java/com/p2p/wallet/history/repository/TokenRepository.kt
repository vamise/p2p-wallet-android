package com.p2p.wallet.history.repository

import com.p2p.wallet.history.model.PriceHistory

interface TokenRepository {
    suspend fun getDailyPriceHistory(sourceToken: String, destination: String, days: Int): List<PriceHistory>
    suspend fun getHourlyPriceHistory(sourceToken: String, destination: String, hours: Int): List<PriceHistory>
}