package org.p2p.wallet.history.interactor

import org.p2p.solanaj.kits.TokenTransaction
import org.p2p.solanaj.kits.transaction.BurnOrMintDetails
import org.p2p.solanaj.kits.transaction.CloseAccountDetails
import org.p2p.solanaj.kits.transaction.CreateAccountDetails
import org.p2p.solanaj.kits.transaction.SwapDetails
import org.p2p.solanaj.kits.transaction.TransactionDetails
import org.p2p.solanaj.kits.transaction.TransactionTypeParser
import org.p2p.solanaj.kits.transaction.TransferDetails
import org.p2p.solanaj.kits.transaction.UnknownDetails
import org.p2p.solanaj.model.types.AccountInfo
import org.p2p.solanaj.programs.TokenProgram
import org.p2p.wallet.history.model.HistoryTransaction
import org.p2p.wallet.history.model.TransactionConverter
import org.p2p.wallet.infrastructure.network.provider.TokenKeyProvider
import org.p2p.wallet.rpc.repository.RpcRepository
import org.p2p.wallet.user.repository.UserLocalRepository
import org.p2p.wallet.utils.Constants.SOL_SYMBOL
import org.p2p.wallet.utils.Constants.WRAPPED_SOL_MINT
import org.p2p.wallet.utils.toPublicKey

class HistoryInteractor(
    private val rpcRepository: RpcRepository,
    private val userLocalRepository: UserLocalRepository,
    private val tokenKeyProvider: TokenKeyProvider
) {

    suspend fun getConfirmedTransaction(tokenPublicKey: String, transactionId: String): HistoryTransaction? =
        parseTransactions(tokenPublicKey, listOf(transactionId)).firstOrNull()

    suspend fun getHistory(tokenPublicKey: String, before: String?, limit: Int): List<HistoryTransaction> {
        val signatures = rpcRepository.getConfirmedSignaturesForAddress(
            tokenPublicKey.toPublicKey(), before, limit
        ).map { it.signature }

        return parseTransactions(tokenPublicKey, signatures)
    }

    private suspend fun parseTransactions(tokenPublicKey: String, signatures: List<String>): List<HistoryTransaction> {
        val transactions = mutableListOf<TransactionDetails>()
        rpcRepository.getConfirmedTransactions(signatures)
            .forEach { response ->
                val data = TransactionTypeParser.parse(response)
                val swap = data.firstOrNull { it is SwapDetails }
                if (swap != null) {
                    transactions.add(swap)
                    return@forEach
                }

                val burnOrMint = data.firstOrNull { it is BurnOrMintDetails }
                if (burnOrMint != null) {
                    transactions.add(burnOrMint)
                    return@forEach
                }

                val transfer = data.firstOrNull { it is TransferDetails }
                if (transfer != null) {
                    transactions.add(transfer)
                    return@forEach
                }

                val create = data.firstOrNull { it is CreateAccountDetails }
                if (create != null) {
                    transactions.add(create)
                    return@forEach
                }

                val close = data.firstOrNull { it is CloseAccountDetails }
                if (close != null) {
                    transactions.add(close)
                    return@forEach
                }

                val unknown = data.firstOrNull { it is UnknownDetails }
                if (unknown != null) {
                    transactions.add(unknown)
                    return@forEach
                }
            }

        /*
         * Making one request for all accounts info and caching values locally
         * to avoid multiple requests when constructing transaction
         * */
        val accountsInfoIds = transactions
            .flatMap { details ->
                when (details) {
                    is SwapDetails -> listOf(
                        details.source,
                        details.alternateSource,
                        details.destination,
                        details.alternateDestination
                    )
                    else -> emptyList()
                }
            }
            .distinct()

        val accountsInfo = if (accountsInfoIds.isNotEmpty()) {
            rpcRepository.getAccountsInfo(accountsInfoIds)
        } else {
            emptyList()
        }

        val userPublicKey = tokenKeyProvider.publicKey

        return transactions
            .mapNotNull { details ->
                when (details) {
                    is SwapDetails -> parseOrcaSwapDetails(details, accountsInfo)
                    is BurnOrMintDetails -> parseBurnAndMintDetails(details, userPublicKey)
                    is TransferDetails -> parseTransferDetails(details, tokenPublicKey, userPublicKey)
                    is CloseAccountDetails -> parseCloseDetails(details)
                    is CreateAccountDetails -> TransactionConverter.fromNetwork(details)
                    is UnknownDetails -> TransactionConverter.fromNetwork(details)
                    else -> throw IllegalStateException("Unknown transaction details $details")
                }
            }
            .sortedByDescending { it.date.toInstant().toEpochMilli() }
    }

    private fun parseOrcaSwapDetails(
        details: SwapDetails,
        accountsInfo: List<Pair<String, AccountInfo>>
    ): HistoryTransaction? {
        val finalMintA = parseOrcaSource(details, accountsInfo) ?: return null
        val finalMintB = parseOrcaDestination(details, accountsInfo) ?: return null

        val sourceData = userLocalRepository.findTokenData(finalMintA) ?: return null
        val destinationData = userLocalRepository.findTokenData(finalMintB) ?: return null

        if (sourceData.mintAddress == destinationData.mintAddress) return null

        val destinationRate = userLocalRepository.getPriceByToken(destinationData.symbol)
        val sourceRate = userLocalRepository.getPriceByToken(sourceData.symbol)
        val source = tokenKeyProvider.publicKey
        return TransactionConverter.fromNetwork(
            details,
            sourceData,
            destinationData,
            destinationRate,
            sourceRate,
            source
        )
    }

    private fun parseOrcaSource(
        details: SwapDetails,
        accountsInfo: List<Pair<String, AccountInfo>>
    ): String? {
        if (!details.mintA.isNullOrEmpty()) {
            return details.mintA
        }

        val accountInfo = accountsInfo.find { it.first == details.source }?.second ?: return null
        val info = TokenTransaction.parseAccountInfoData(accountInfo, TokenProgram.PROGRAM_ID)
        if (info != null) return info.mint.toBase58()

        val account = accountsInfo.find { it.first == details.alternateSource } ?: return null
        val alternateInfo = TokenTransaction.parseAccountInfoData(account.second, TokenProgram.PROGRAM_ID)
        return alternateInfo?.mint?.toBase58()
    }

    private fun parseOrcaDestination(
        details: SwapDetails,
        accountsInfo: List<Pair<String, AccountInfo>>
    ): String? {
        if (!details.mintB.isNullOrEmpty()) {
            return details.mintB
        }

        val accountInfo = accountsInfo.find { it.first == details.destination }?.second ?: return null
        val info = TokenTransaction.parseAccountInfoData(accountInfo, TokenProgram.PROGRAM_ID)
        if (info != null) return info.mint.toBase58()

        val account = accountsInfo.find { it.first == details.alternateDestination } ?: return null
        val alternateInfo = TokenTransaction.parseAccountInfoData(account.second, TokenProgram.PROGRAM_ID)
        return alternateInfo?.mint?.toBase58()
    }

    private fun parseTransferDetails(
        transfer: TransferDetails,
        directPublicKey: String,
        publicKey: String
    ): HistoryTransaction? {
        val symbol = if (transfer.isSimpleTransfer) SOL_SYMBOL else findSymbol(transfer.mint)
        val rate = userLocalRepository.getPriceByToken(symbol)

        val mint = if (transfer.isSimpleTransfer) WRAPPED_SOL_MINT else transfer.mint
        val source = userLocalRepository.findTokenData(mint) ?: return null

        return TransactionConverter.fromNetwork(transfer, source, directPublicKey, publicKey, rate)
    }

    private fun parseBurnAndMintDetails(details: BurnOrMintDetails, userPublicKey: String): HistoryTransaction {
        val symbol = findSymbol(details.mint)
        val rate = userLocalRepository.getPriceByToken(symbol)
        return TransactionConverter.fromNetwork(details, userPublicKey, rate)
    }

    private fun parseCloseDetails(
        details: CloseAccountDetails
    ): HistoryTransaction {
        val symbol = findSymbol(details.mint)
        return TransactionConverter.fromNetwork(details, symbol)
    }

    private fun findSymbol(mint: String): String =
        if (mint.isNotEmpty()) userLocalRepository.findTokenData(mint)?.symbol.orEmpty() else ""
}