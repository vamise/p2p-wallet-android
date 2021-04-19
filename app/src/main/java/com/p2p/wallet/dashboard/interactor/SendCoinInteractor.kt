package com.p2p.wallet.dashboard.interactor

import com.p2p.wallet.infrastructure.persistence.PreferenceService
import com.p2p.wallet.dashboard.repository.WowletApiCallRepository
import com.p2p.wallet.dashboard.repository.transferInfoToActivityItem
import com.p2p.wallet.common.network.CallException
import com.p2p.wallet.common.network.Constants.Companion.ERROR_WALLET_ITEM_IS_NULL
import com.p2p.wallet.common.network.Constants.Companion.REQUEST_EXACTION
import com.p2p.wallet.common.network.Result
import com.p2p.wallet.dashboard.model.local.ActivityItem
import com.p2p.wallet.dashboard.model.local.SendTransactionModel
import com.p2p.wallet.dashboard.model.local.Token
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow

class SendCoinInteractor(
    private val wowletApiCallRepository: WowletApiCallRepository,
    private val preferenceService: PreferenceService
) {

    suspend fun sendCoin(coinData: SendTransactionModel): Result<ActivityItem> {
        val activeWallet = preferenceService.getActiveWallet()
        coinData.secretKey = activeWallet?.secretKey!!
        // coinData.secretKey = "4g8ivUf5LiczqSUs7v2XjhVetZKhSkEFFSRtD94sgtkG6jhNeKXZN6KHkbs2U6AJJWmBXQo8zqmNGFnbgA3F6VCu"
        coinData.fromPublicKey = activeWallet?.publicKey!!
        // coinData.fromPublicKey = "22CbwPktYBVbTctjfsr35ozanxwfVjNbBofsnWY4C2YR"
        try {
            val signature = wowletApiCallRepository.sendTransaction(coinData)
            repeat(1000) {
                delay(120_000)
                val transaction = wowletApiCallRepository.getConfirmedTransaction(
                    signature,
                    0
                )?.transferInfoToActivityItem(coinData.fromPublicKey, "", "", "")
                transaction?.tokenSymbol = coinData.tokenSymbol
                return Result.Success(transaction)
            }
            return Result.Error(CallException(REQUEST_EXACTION, ""))
        } catch (e: Exception) {
            return Result.Error(CallException(REQUEST_EXACTION, e.message))
        }
    }

    fun saveWalletItem(walletItem: Token?) {
        preferenceService.setWalletItemData(walletItem)
    }

    suspend fun getWalletItem(): Result<Token> {
        val walletItemData = preferenceService.getWalletItemData()
        return if (walletItemData != null)
            Result.Success(walletItemData)
        else
            Result.Error(CallException(ERROR_WALLET_ITEM_IS_NULL))
    }

    suspend fun getFee(): Result<BigDecimal> {
        return try {
            val fee = wowletApiCallRepository.getFee()
            val feeValue = (fee / 10.0.pow(9.0))
            val scale = BigDecimal(feeValue).setScale(6, RoundingMode.HALF_EVEN)
            Result.Success(scale)
        } catch (e: Exception) {
            Result.Error(CallException(REQUEST_EXACTION, e.message))
        }
    }
}