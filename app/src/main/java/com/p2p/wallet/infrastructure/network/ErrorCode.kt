package com.p2p.wallet.infrastructure.network

import com.google.gson.annotations.SerializedName
import com.p2p.wallet.R

private const val DEFAULT_MESSAGE_RES = R.string.error_general_message

enum class ErrorCode(val messageRes: Int = DEFAULT_MESSAGE_RES) {
    @SerializedName("-32602")
    INVALID_TRANSACTION(R.string.error_invalid_transaction),

    @SerializedName("500")
    SERVER_ERROR;

    val hasSpecifiedMessage = messageRes != DEFAULT_MESSAGE_RES
}