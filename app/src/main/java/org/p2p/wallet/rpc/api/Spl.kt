package org.p2p.wallet.rpc.api

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class Spl(
    @SerializedName("program_id")
    val programId: String,
    @SerializedName("account_pubkey")
    val accountPubkey: String,
    @SerializedName("authority_pubkey")
    val authorityPubkey: String,
    @SerializedName("transfer_authority_pubkey")
    val transferAuthorityPubkey: String,
    @SerializedName("source_pubkey")
    val sourcePubkey: String,
    @SerializedName("destination_pubkey")
    val destinationPubkey: String,
    @SerializedName("pool_token_mint_pubkey")
    val poolTokenMintPubkey: String,
    @SerializedName("pool_fee_account_pubkey")
    val poolFeeAccountPubkey: String,
    @SerializedName("amount_in")
    val amountIn: BigDecimal,
    @SerializedName("minimum_amount_out")
    val minimumAmountOut: BigDecimal
)