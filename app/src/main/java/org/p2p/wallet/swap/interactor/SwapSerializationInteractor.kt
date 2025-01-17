package org.p2p.wallet.swap.interactor

import org.p2p.solanaj.core.Account
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.Transaction
import org.p2p.solanaj.core.TransactionInstruction
import org.p2p.solanaj.utils.crypto.Base64Utils
import org.p2p.wallet.feerelayer.repository.FeeRelayerRepository
import org.p2p.wallet.infrastructure.network.provider.TokenKeyProvider
import org.p2p.wallet.rpc.repository.RpcRepository
import org.p2p.wallet.transaction.TransactionManager
import org.p2p.wallet.transaction.model.AppTransaction
import org.p2p.wallet.utils.toPublicKey
import timber.log.Timber

class SwapSerializationInteractor(
    private val rpcRepository: RpcRepository,
    private val tokenKeyProvider: TokenKeyProvider,
    private val feeRelayerRepository: FeeRelayerRepository,
    private val transactionManager: TransactionManager
) {

    fun sendTransaction(appTransaction: AppTransaction) {
        transactionManager.addInQueue(appTransaction)
    }

    suspend fun serializeAndSend(
        instructions: List<TransactionInstruction>,
        recentBlockhash: String? = null,
        signers: List<Account>,
        isSimulation: Boolean,
        sourceSymbol: String,
        destinationSymbol: String
    ): String {
        val serializedTransaction = serializeTransaction(
            instructions = instructions,
            recentBlockhash = recentBlockhash,
            signers = signers
        )

        val transaction = AppTransaction(
            serializedTransaction = serializedTransaction,
            sourceSymbol = sourceSymbol,
            destinationSymbol = destinationSymbol,
            isSimulation = isSimulation
        )
        transactionManager.addInQueue(transaction)

        return serializedTransaction
    }

    suspend fun serializeTransaction(
        instructions: List<TransactionInstruction>,
        recentBlockhash: String? = null,
        signers: List<Account>,
        feePayer: PublicKey? = null
    ): String {
        // get recentBlockhash
        val blockhash = if (recentBlockhash.isNullOrEmpty()) {
            rpcRepository.getRecentBlockhash().recentBlockhash
        } else {
            recentBlockhash
        }

        val accountPublicKey = tokenKeyProvider.publicKey.toPublicKey()
        val feePayerPublicKey = feePayer ?: accountPublicKey

        // serialize transaction
        val transaction = Transaction()
        transaction.addInstructions(instructions)
        transaction.setFeePayer(feePayerPublicKey)
        transaction.setRecentBlockHash(blockhash)
        transaction.sign(signers)

        val serializedMessage = transaction.serialize()
        val serializedTransaction = Base64Utils.encode(serializedMessage)

        Timber.d("Serialized transaction: $serializedTransaction")
        return serializedTransaction
    }
}