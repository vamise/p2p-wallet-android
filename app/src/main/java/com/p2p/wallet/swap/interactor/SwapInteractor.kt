package com.p2p.wallet.swap.interactor

import com.p2p.wallet.amount.scalePrice
import com.p2p.wallet.amount.valueOrZero
import com.p2p.wallet.common.network.Constants
import com.p2p.wallet.infrastructure.network.provider.TokenKeyProvider
import com.p2p.wallet.main.model.TransactionResult
import com.p2p.wallet.main.model.roundToDefaultScale
import com.p2p.wallet.swap.model.SwapRequest
import com.p2p.wallet.swap.model.SwapResult
import com.p2p.wallet.swap.repository.SwapLocalRepository
import com.p2p.wallet.swap.repository.SwapRepository
import com.p2p.wallet.user.interactor.UserInteractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.kits.Pool
import org.p2p.solanaj.kits.TokenSwap
import org.p2p.solanaj.rpc.types.TokenAccountBalance
import java.math.BigDecimal
import java.math.BigInteger

class SwapInteractor(
    private val swapRepository: SwapRepository,
    private val swapLocalRepository: SwapLocalRepository,
    private val tokenKeyProvider: TokenKeyProvider,
    private val userInteractor: UserInteractor
) {

    suspend fun loadAllPools() {
        val pools = swapRepository.loadPoolInfoList(Constants.SWAP_PROGRAM_ID)
        swapLocalRepository.setPools(pools)
    }

    fun getAllPools() =
        swapLocalRepository.getPools()

    suspend fun findPool(sourceMint: String, destinationMint: String): Pool.PoolInfo? = withContext(Dispatchers.IO) {
        val allPools = swapLocalRepository.getPools()
        val pool = allPools.find {
            val mintA = it.swapData.mintA.toBase58()
            val mintB = it.swapData.mintB.toBase58()
            (sourceMint == mintA && destinationMint == mintB) || (sourceMint == mintB && destinationMint == mintA)
        } ?: return@withContext null

        if (pool.swapData.mintB.toBase58() == sourceMint && pool.swapData.mintA.toBase58() == destinationMint) {
            pool.swapData.swapMintData()
            pool.swapData.swapTokenAccount()
        }

        return@withContext pool
    }

    suspend fun loadTokenBalance(publicKey: PublicKey): TokenAccountBalance =
        swapRepository.loadTokenBalance(publicKey)

    suspend fun swap(
        request: SwapRequest,
        receivedAmount: BigDecimal,
        usdReceivedAmount: BigDecimal,
        tokenSymbol: String
    ): SwapResult {
        val accountAddressA = userInteractor.findAccountAddress(request.pool.mintA)
        val accountAddressB = userInteractor.findAccountAddress(request.pool.mintB)
        val keys = userInteractor.getSecretKeys()
        val signature = swapRepository.swap(keys, request, accountAddressA, accountAddressB)
        return SwapResult.Success(signature, receivedAmount, usdReceivedAmount, tokenSymbol)
    }

    fun calculateFee(
        pool: Pool.PoolInfo,
        sourceAmount: BigInteger,
        tokenABalance: TokenAccountBalance,
        tokenBBalance: TokenAccountBalance
    ): BigInteger {
        val swappedAmountWithFee =
            calculateAmountInOtherToken(pool, sourceAmount, true, tokenABalance, tokenBBalance)
        val swappedAmountWithoutFee =
            calculateAmountInOtherToken(pool, sourceAmount, false, tokenABalance, tokenBBalance)

        return swappedAmountWithoutFee.subtract(swappedAmountWithFee)
    }

    fun calculateMinReceive(
        balanceA: TokenAccountBalance,
        balanceB: TokenAccountBalance,
        amount: BigInteger,
        slippage: Double
    ): BigInteger {
        val estimated = TokenSwap.calculateSwapEstimatedAmount(balanceA, balanceB, amount)
        return TokenSwap.calculateSwapMinimumReceiveAmount(estimated, slippage)
    }

    fun calculateAmountInConvertingToken(amount: String, from: Double, to: Double): Double {
        val currencyInFrom = from.div(to)
        val amountAsDouble = amount.toDoubleOrNull().valueOrZero()
        return amountAsDouble.times(currencyInFrom)
    }

    private fun calculateAmountInOtherToken(
        pool: Pool.PoolInfo,
        inputAmount: BigInteger,
        withFee: Boolean,
        tokenABalance: TokenAccountBalance,
        tokenBBalance: TokenAccountBalance
    ): BigInteger {

        val tokenSource = PublicKey(tokenKeyProvider.publicKey)
        val isReverse = pool.tokenAccountB.equals(tokenSource)

        val feeRatio = BigDecimal(pool.tradeFeeNumerator).divide(BigDecimal(pool.tradeFeeDenominator))

        val firstAmountInPool = if (isReverse) tokenBBalance.amount else tokenABalance.amount
        val secondAmountInPool = if (isReverse) tokenABalance.amount else tokenBBalance.amount

        val invariant = firstAmountInPool.multiply(secondAmountInPool)
        val newFromAmountInPool = firstAmountInPool.add(inputAmount)
        val newToAmountInPool = invariant.divide(newFromAmountInPool)
        val grossToAmount = secondAmountInPool.subtract(newToAmountInPool)
        val fees = if (withFee) {
            BigDecimal(grossToAmount).multiply(feeRatio)
        } else {
            BigDecimal.valueOf(0)
        }
        return BigDecimal(grossToAmount).subtract(fees).toBigInteger()
    }
}