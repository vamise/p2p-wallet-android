package org.p2p.wallet.renbtc.interactor

import org.p2p.wallet.infrastructure.network.environment.EnvironmentManager
import org.p2p.wallet.infrastructure.network.provider.TokenKeyProvider
import org.p2p.wallet.rpc.repository.RpcRepository
import org.p2p.wallet.utils.fromLamports
import org.p2p.wallet.utils.scaleMedium
import org.p2p.wallet.utils.toPublicKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.p2p.solanaj.core.Account
import org.p2p.solanaj.kits.renBridge.BurnAndRelease
import org.p2p.solanaj.kits.renBridge.NetworkConfig
import org.p2p.solanaj.rpc.Environment
import org.p2p.solanaj.rpc.RpcException
import java.math.BigDecimal
import java.math.BigInteger

class BurnBtcInteractor(
    private val tokenKeyProvider: TokenKeyProvider,
    private val environmentManager: EnvironmentManager,
    private val rpcRepository: RpcRepository
) {

    companion object {
        private const val BURN_FEE_LENGTH = 97L
    }

    suspend fun submitBurnTransaction(recipient: String, amount: BigInteger): String = withContext(Dispatchers.IO) {
        val signer = tokenKeyProvider.publicKey.toPublicKey()
        val signerSecretKey = tokenKeyProvider.secretKey
        val burnAndRelease = BurnAndRelease(getNetworkConfig())

        val burnDetails = burnAndRelease.submitBurnTransaction(
            signer,
            amount.toString(),
            recipient,
            Account(signerSecretKey)
        )

        val burnState = burnAndRelease.getBurnState(burnDetails, amount.toString())
        val hash = try {
            burnAndRelease.release()
        } catch (e: RpcException) {
            // TODO: Handle this error [invalid burn info: cannot get burn info: decoding solana burn log: expected data len=97, got=0]
            // TODO: It crashes even if transaction is valid
            if (e.message?.startsWith("invalid burn info") == true) {
                return@withContext burnDetails.confirmedSignature
            } else {
                throw e
            }
        }
        println("txHash ${burnState.txHash} / $hash")
        return@withContext burnDetails.confirmedSignature
    }

    suspend fun getBurnFee(): BigDecimal {
        val fee = rpcRepository.getMinimumBalanceForRentExemption(BURN_FEE_LENGTH).toBigInteger()
        return fee.fromLamports().add(BigDecimal("0.000005")).scaleMedium()
    }

    private fun getNetworkConfig(): NetworkConfig =
        when (environmentManager.loadEnvironment()) {
            Environment.DEVNET -> NetworkConfig.DEVNET()
            Environment.RPC_POOL,
            Environment.MAINNET,
            Environment.SOLANA -> NetworkConfig.MAINNET()
        }
}