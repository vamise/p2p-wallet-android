package org.p2p.solanaj.kits.transaction

import org.p2p.solanaj.kits.transaction.ConfirmedTransactionParsed.InstructionParsed
import org.p2p.solanaj.programs.SerumSwapProgram
import org.p2p.solanaj.programs.SerumSwapProgram.serumSwapPID
import org.p2p.solanaj.programs.SystemProgram.SPL_TOKEN_PROGRAM_ID
import java.math.BigInteger

// todo: Parser should be refactored and optimized
// Get some parsing info from here
// https://github.com/p2p-org/solana-swift/blob/main/Sources/SolanaSwift/Helpers/TransactionParser.swift#L74-L86
object TransactionTypeParser {

    private var details = mutableListOf<TransactionDetails>()

    fun parse(transaction: ConfirmedTransactionParsed): List<TransactionDetails> {
        details = ArrayList()
        val signature = transaction.transaction.signatures.firstOrNull().orEmpty()
        val instructions = transaction.transaction.message.instructions

        /* Orca Swap */
        val orcaSwapInstructionIndex = getOrcaSwapInstructionIndex(instructions)
        if (orcaSwapInstructionIndex != -1) {
            checkOrcaSwapDetails(orcaSwapInstructionIndex, transaction)
            return details
        }

        /* Serum Swap */
        val serumSwapInstructionIndex = getSerumSwapInstructionIndex(instructions)
        if (serumSwapInstructionIndex != -1) {
            parseSerumSwapTransaction(transaction, signature)
            return details
        }

        parseDetails(transaction, signature, details)
        return details
    }

    private fun checkOrcaSwapDetails(
        instructionIndex: Int,
        transaction: ConfirmedTransactionParsed
    ) {
        when {
            isLiquidityToPool(transaction.meta.innerInstructions) -> return
            isBurn(transaction.meta.innerInstructions) -> return
            else -> {
                val swapDetails = parseOrcaSwapTransaction(instructionIndex, transaction)
                swapDetails?.let { details.add(it) }
            }
        }
    }

    private fun parseOrcaSwapTransaction(
        index: Int,
        transaction: ConfirmedTransactionParsed
    ): TransactionDetails? {
        val signature = transaction.transaction.signatures.firstOrNull().orEmpty()
        val instructions = transaction.transaction.message.instructions

        // get instruction
        if (index >= instructions.size) return null

        val innerInstructions = transaction.meta.innerInstructions
        // check inner instructions
        val source = innerInstructions.firstOrNull() ?: return null
        val destination = innerInstructions.lastOrNull() ?: return null
        // get instructions
        val sourceInstructions = source.instructions.filter {
            it.parsed.type == "transfer"
        }

        val destinationInstructions = destination.instructions.filter {
            it.parsed.type == "transfer"
        }

        if (sourceInstructions.size < 2 || destinationInstructions.size < 2) return null

        val sourceInstruction = sourceInstructions[0]
        val destinationInstruction = destinationInstructions[1]

        val sourceInfo = sourceInstruction.parsed?.info ?: return null
        val destinationInfo = destinationInstruction.parsed?.info ?: return null

        // get source
        val fromAddress = sourceInfo["source"] as? String
        val alternateFromAddress = sourceInfo["destination"] as? String

        val toAddress = destinationInfo["destination"] as? String
        val alternateToAddress = destinationInfo["source"] as? String

        val amountA = sourceInfo["amount"] as? String ?: "0"
        val amountB = destinationInfo["amount"] as? String ?: "0"

        return SwapDetails(
            signature,
            transaction.blockTime,
            transaction.slot,
            transaction.meta.fee,
            fromAddress,
            toAddress,
            amountA,
            amountB,
            null,
            null,
            alternateFromAddress,
            alternateToAddress
        )
    }

    private fun parseDetails(
        transaction: ConfirmedTransactionParsed,
        signature: String,
        details: MutableList<TransactionDetails>
    ) {
        val instructions = transaction.transaction.message.instructions
        instructions.forEach { parsedInstruction ->
            val parsedInfo = parsedInstruction.parsed
            when (parsedInfo?.type) {
                "burnChecked" -> parseBurnOrMintTransaction(signature, transaction, parsedInfo, details)
                "transfer",
                "transferChecked" -> parseTransferTransaction(signature, transaction, parsedInfo, details)
                "closeAccount" -> parseCloseTransaction(parsedInstruction, parsedInfo, transaction, signature, details)
                "create" -> parseCreateAccountTransaction(signature, transaction)
                else -> parseUnknownTransaction(parsedInfo, details, signature, transaction)
            }
        }
    }

    private fun parseCreateAccountTransaction(
        signature: String,
        transaction: ConfirmedTransactionParsed
    ) {
        details.add(CreateAccountDetails(signature, transaction.slot, transaction.blockTime, transaction.meta.fee))
    }

    private fun parseUnknownTransaction(
        parsedInfo: ConfirmedTransactionParsed.Parsed?,
        details: MutableList<TransactionDetails>,
        signature: String,
        transaction: ConfirmedTransactionParsed
    ) {
        if (parsedInfo != null) {
            details.add(UnknownDetails(signature, transaction.blockTime, transaction.slot, parsedInfo.info))
        }
    }

    private fun parseCloseTransaction(
        parsedInstruction: InstructionParsed,
        parsedInfo: ConfirmedTransactionParsed.Parsed,
        transaction: ConfirmedTransactionParsed,
        signature: String,
        details: MutableList<TransactionDetails>
    ) {
        if (parsedInstruction.programId.equals(SPL_TOKEN_PROGRAM_ID.toBase58())) {
            val closedTokenPublicKey = parsedInfo.info["account"] as String
            val preBalances = transaction.meta.preTokenBalances?.firstOrNull()?.mint
            val closeDetails = CloseAccountDetails(
                signature,
                transaction.blockTime,
                transaction.slot,
                closedTokenPublicKey,
                preBalances
            )
            details.add(closeDetails)
        }
    }

    private fun parseTransferTransaction(
        signature: String,
        transaction: ConfirmedTransactionParsed,
        parsedInfo: ConfirmedTransactionParsed.Parsed,
        details: MutableList<TransactionDetails>
    ) {
        val transferDetails = TransferDetails(
            signature,
            transaction.blockTime,
            transaction.slot,
            transaction.meta.fee,
            parsedInfo.type,
            parsedInfo.info
        )
        details.add(transferDetails)
    }

    private fun parseBurnOrMintTransaction(
        signature: String,
        transaction: ConfirmedTransactionParsed,
        parsedInfo: ConfirmedTransactionParsed.Parsed,
        details: MutableList<TransactionDetails>
    ) {
        val transferDetails = BurnOrMintDetails(
            signature,
            transaction.blockTime,
            transaction.slot,
            transaction.meta.fee,
            parsedInfo.type,
            parsedInfo.info
        )
        details.add(transferDetails)
    }

    private fun parseSerumSwapTransaction(
        transaction: ConfirmedTransactionParsed,
        signature: String
    ) {
        val instructions = transaction.transaction.message.instructions
        val swapInstructionIndex = getSerumSwapInstructionIndex(instructions)

        val preTokenBalances = transaction.meta.preTokenBalances

        val innerInstruction = transaction.meta.innerInstructions.firstOrNull()

        // get swapInstruction
        val swapInstruction = instructions.getOrNull(swapInstructionIndex) ?: return

        // get all mints
        val mints = preTokenBalances.map { it.mint }.distinct().toMutableList()
        if (mints.size < 2) return

        // transitive swap: remove usdc or usdt if exists
        if (mints.size == 3) {
            mints.removeAll { isUsdx(it) }
        }

        // define swap type
        val isTransitiveSwap = mints.none { isUsdx(it) }

        // assert
        val accounts = swapInstruction.accounts
        if (accounts.isEmpty()) return

        if (isTransitiveSwap && accounts.size != 27) return

        if (!isTransitiveSwap && accounts.size != 16) return

        // get from and to address
        var fromAddress: String
        var toAddress: String

        if (isTransitiveSwap) { // transitive
            fromAddress = accounts[6]
            toAddress = accounts[21]
        } else { // direct
            fromAddress = accounts[10]
            toAddress = accounts[12]

            if (isUsdx(mints.firstOrNull()) && !isUsdx(mints.lastOrNull())) {
                val temp = fromAddress
                fromAddress = toAddress
                toAddress = temp
            }
        }

        // amounts
        val fromInstruction = innerInstruction?.instructions?.find {
            it.parsed?.type == "transfer" && it.parsed.info["source"] == fromAddress
        }

        val amountString = fromInstruction?.parsed?.info?.get("amount") as? String
        val fromAmount = amountString?.let { BigInteger(it) } ?: BigInteger.ZERO

        val toInstruction = innerInstruction?.instructions?.find {
            it.parsed?.type == "transfer" && it.parsed.info["destination"] == toAddress
        }
        val toAmountString = toInstruction?.parsed?.info?.get("amount") as? String
        val toAmount = toAmountString?.let { BigInteger(it) } ?: BigInteger.ZERO

        // if swap from native sol, detect if from or to address is a new account
        val createAccountInstruction = instructions.firstOrNull {
            it.parsed?.type == "createAccount" && it.parsed.info["newAccount"] == fromAddress
        }

        val realSource = createAccountInstruction?.parsed?.info?.get("source") as? String
        if (!realSource.isNullOrEmpty()) {
            fromAddress = realSource
        }

        // get token from mint address and finish request
        val transactionDetails = SwapDetails(
            signature,
            transaction.blockTime,
            transaction.slot,
            transaction.meta.fee,
            fromAddress,
            toAddress,
            fromAmount.toString(),
            toAmount.toString(),
            mints[0],
            mints[1],
            null,
            null
        )

        details.add(transactionDetails)
    }

    // Serum swap
    private fun getSerumSwapInstructionIndex(instructions: List<InstructionParsed>): Int {
        return instructions.indexOfLast { it.programId == serumSwapPID.toBase58() }
    }

    private fun getOrcaSwapInstructionIndex(instructions: List<InstructionParsed>): Int =
        instructions.indexOfFirst {
            SwapDetails.KNOWN_SWAP_PROGRAM_IDS.contains(it.programId)
        }

    /**
     * Check liquidity to pool
     * @param: inner instructions
     * */
    private fun isLiquidityToPool(innerInstructions: List<ConfirmedTransactionParsed.InnerInstruction>?): Boolean {
        val instructions = innerInstructions?.firstOrNull()?.instructions
        return when (instructions?.size) {
            3 -> {
                instructions[0].parsed?.type == "transfer" &&
                    instructions[1].parsed?.type == "transfer" &&
                    instructions[2].parsed?.type == "mintTo"
            }
            else -> false
        }
    }

    /**
     Check liquidity to pool
     - Parameter instructions: inner instructions
     */
    private fun isBurn(innerInstructions: List<ConfirmedTransactionParsed.InnerInstruction>?): Boolean {
        val instructions = innerInstructions?.firstOrNull()?.instructions
        return when (instructions?.size) {
            3 -> {
                instructions[0].parsed?.type == "burn" &&
                    instructions[1].parsed?.type == "transfer" &&
                    instructions[2].parsed?.type == "transfer"
            }
            else -> false
        }
    }

    private fun isUsdx(value: String?): Boolean {
        return value == SerumSwapProgram.usdcMint.toBase58() || value == SerumSwapProgram.usdtMint.toBase58()
    }
}