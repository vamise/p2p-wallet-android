package com.p2p.wallet.dashboard.model.local

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow

@Parcelize
data class Token(
    val tokenSymbol: String,
    val depositAddress: String,
    val decimals: Int,
    val mintAddress: String,
    val tokenName: String,
    val iconUrl: String,
    val price: BigDecimal,
    val total: BigDecimal,
    val walletBinds: Double
) : Parcelable {

    // fixme: remove this after refactoring. We should avoid creating objects with default params
    constructor() : this(
        "",
        "",
        0,
        "",
        "",
        "",
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        0.0
    )

    companion object {
        private const val ADDRESS_SYMBOL_COUNT = 10
        private const val SOL_DECIMALS = 9

        /* fixme: workaround about adding hardcode wallet, looks strange */
        fun getSOL(publicKey: String, amount: Long) = Token(
            tokenSymbol = "SOL",
            tokenName = "SOL",
            mintAddress = "SOLMINT",
            iconUrl = "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/solana/info/logo.png",
            depositAddress = publicKey,
            decimals = 9,
            total = BigDecimal(amount).divide(BigDecimal(10.0.pow(SOL_DECIMALS))),
            price = BigDecimal.ZERO,
            walletBinds = 0.0
        )
    }

    @Suppress("MagicNumber")
    fun getFormattedAddress(): String {
        if (depositAddress.length < ADDRESS_SYMBOL_COUNT) {
            return depositAddress
        }

        val firstSix = depositAddress.take(4)
        val lastFour = depositAddress.takeLast(4)
        return "$firstSix...$lastFour"
    }

    fun getFormattedPrice(): String = "${price.setScale(2, RoundingMode.HALF_EVEN)} $"

    fun getFormattedTotal(): String = "$total $tokenSymbol"
}