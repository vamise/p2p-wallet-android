package org.p2p.wallet.send.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.p2p.solanaj.core.PublicKey
import org.p2p.wallet.R
import org.p2p.wallet.common.mvp.BasePresenter
import org.p2p.wallet.history.model.HistoryTransaction
import org.p2p.wallet.history.model.TransferType
import org.p2p.wallet.home.model.Token
import org.p2p.wallet.home.model.TokenConverter
import org.p2p.wallet.infrastructure.network.provider.TokenKeyProvider
import org.p2p.wallet.renbtc.interactor.BurnBtcInteractor
import org.p2p.wallet.send.interactor.SearchInteractor
import org.p2p.wallet.send.interactor.SendInteractor
import org.p2p.wallet.send.model.CheckAddressResult
import org.p2p.wallet.send.model.CurrencyMode
import org.p2p.wallet.send.model.NetworkType
import org.p2p.wallet.send.model.SearchResult
import org.p2p.wallet.send.model.SendFee
import org.p2p.wallet.send.model.SendTotal
import org.p2p.wallet.send.model.Target
import org.p2p.wallet.send.model.SendResult
import org.p2p.wallet.settings.interactor.SettingsInteractor
import org.p2p.wallet.send.model.SendConfirmData
import org.p2p.wallet.transaction.model.ShowProgress
import org.p2p.wallet.user.interactor.UserInteractor
import org.p2p.wallet.utils.Constants.SOL_SYMBOL
import org.p2p.wallet.utils.Constants.USD_READABLE_SYMBOL
import org.p2p.wallet.utils.cutEnd
import org.p2p.wallet.utils.cutMiddle
import org.p2p.wallet.utils.fromLamports
import org.p2p.wallet.utils.isMoreThan
import org.p2p.wallet.utils.isZero
import org.p2p.wallet.utils.scaleLong
import org.p2p.wallet.utils.scaleMedium
import org.p2p.wallet.utils.toBigDecimalOrZero
import org.p2p.wallet.utils.toLamports
import org.p2p.wallet.utils.toPublicKey
import org.p2p.wallet.utils.toUsd
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlin.properties.Delegates

class SendPresenter(
    private val initialToken: Token.Active?,
    private val sendInteractor: SendInteractor,
    private val userInteractor: UserInteractor,
    private val searchInteractor: SearchInteractor,
    private val burnBtcInteractor: BurnBtcInteractor,
    private val settingsInteractor: SettingsInteractor,
    private val tokenKeyProvider: TokenKeyProvider
) : BasePresenter<SendContract.View>(), SendContract.Presenter {

    companion object {
        private const val VALID_ADDRESS_LENGTH = 24
        private const val ROUNDING_VALUE = 6
    }

    private var token: Token.Active? by Delegates.observable(null) { _, _, newValue ->
        if (newValue != null) view?.showSourceToken(newValue)
    }

    private val userTokens = mutableListOf<Token.Active>()

    private var inputAmount: String = "0"

    private var tokenAmount: BigDecimal = BigDecimal.ZERO
    private var usdAmount: BigDecimal = BigDecimal.ZERO

    private var mode: CurrencyMode = CurrencyMode.Token(initialToken?.tokenSymbol ?: SOL_SYMBOL)

    private var networkType: NetworkType = NetworkType.SOLANA

    private var target: SearchResult? = null

    private var calculationJob: Job? = null
    private var feeJob: Job? = null
    private var checkAddressJob: Job? = null

    override fun loadInitialData() {
        launch {
            try {
                view?.showFullScreenLoading(true)

                userTokens += userInteractor.getUserTokens()
                val userPublicKey = tokenKeyProvider.publicKey
                val sol = userTokens.find { it.isSOL && it.publicKey == userPublicKey }
                    ?: throw IllegalStateException("No SOL account found")
                token = initialToken ?: sol

                calculateTotal(sendFee = null)
                view?.showNetworkDestination(networkType)

                sendInteractor.initialize(sol)
            } catch (e: Throwable) {
                Timber.e(e, "Error loading initial data")
            } finally {
                view?.showFullScreenLoading(false)
            }
        }
    }

    override fun setSourceToken(newToken: Token.Active) {
        token = newToken
        if (newToken.isRenBTC) {
            view?.showNetworkSelectionView(true)
        } else {
            /* reset to default network and updating UI if source is not renBTC */
            networkType = NetworkType.SOLANA
            view?.showNetworkDestination(networkType)
            view?.showNetworkSelectionView(false)
        }

        val feePayerToken = userTokens.firstOrNull { it.isSOL } ?: newToken
        setFeePayerToken(feePayerToken)

        calculateRenBtcFeeIfNeeded()
        calculateData(newToken)
        checkAddress(target?.address)
    }

    override fun setTargetResult(result: SearchResult?) {
        target = result

        when (result) {
            is SearchResult.Full -> handleFullResult(result)
            is SearchResult.AddressOnly -> handleAddressOnlyResult(result)
            is SearchResult.EmptyBalance -> handleEmptyBalanceResult(result)
            is SearchResult.Wrong -> handleWrongResult(result)
            else -> handleIdleTarget()
        }

        val sourceToken = token ?: return
        calculateData(sourceToken)
    }

    override fun validateTarget(value: String) {
        launch {
            try {
                view?.showSearchLoading(true)
                val target = Target(value)
                when (target.validation) {
                    Target.Validation.USERNAME -> searchByUsername(target.trimmedUsername)
                    Target.Validation.ADDRESS -> searchByNetwork(target.value)
                    Target.Validation.EMPTY -> view?.showIdleTarget()
                    Target.Validation.INVALID -> view?.showWrongAddressTarget(target.value)
                }
            } catch (e: Throwable) {
                Timber.e(e, "Error validating target: $value")
            } finally {
                view?.showSearchLoading(false)
            }
        }
    }

    override fun setNewSourceAmount(amount: String) {
        inputAmount = amount

        val token = token ?: return
        calculateRenBtcFeeIfNeeded()
        calculateData(token)
    }

    override fun setNetworkDestination(networkType: NetworkType) {
        this.networkType = networkType
        view?.showNetworkDestination(networkType)
        calculateRenBtcFeeIfNeeded()
    }

    override fun send() {
        val token = token ?: throw IllegalStateException("Token cannot be null!")
        val address = target?.address ?: throw IllegalStateException("Target address cannot be null!")

        when (networkType) {
            NetworkType.SOLANA -> sendInSolana(token, address)
            NetworkType.BITCOIN -> sendInBitcoin(token, address)
        }
    }

    override fun sendOrConfirm() {
        val token = token ?: throw IllegalStateException("Token cannot be null!")
        val address = target?.address ?: throw IllegalStateException("Target address cannot be null!")

        val isConfirmationRequired = settingsInteractor.isBiometricsConfirmationEnabled()
        if (isConfirmationRequired) {
            val data = SendConfirmData(
                token = token,
                amount = tokenAmount.toString(),
                amountUsd = usdAmount.toString(),
                destination = address
            )
            view?.showBiometricConfirmationPrompt(data)
        } else {
            send()
        }
    }

    override fun loadTokensForSelection() {
        launch {
            val tokens = userInteractor.getUserTokens()
            val result = tokens.filter { token -> !token.isZero }
            view?.navigateToTokenSelection(result)
        }
    }

    override fun loadAvailableValue() {
        val token = token ?: return

        val totalAvailable = when (mode) {
            is CurrencyMode.Usd -> token.totalInUsd
            is CurrencyMode.Token -> token.total.scaleLong()
        } ?: return

        view?.showInputValue(totalAvailable)
        setNewSourceAmount(totalAvailable.toString())
    }

    override fun loadCurrentNetwork() {
        view?.navigateToNetworkSelection(networkType)
    }

    override fun loadFeePayerTokens() {
        val token = token ?: return
        launch {
            val feePayerTokens = sendInteractor.getFeeTokenAccounts(token.publicKey)
            view?.showFeePayerTokenSelector(feePayerTokens)
        }
    }

    override fun setFeePayerToken(feePayerToken: Token.Active) {
        launch {
            try {
                sendInteractor.setFeePayerToken(feePayerToken)
                calculateFeeRelayerFee(feePayerToken)
            } catch (e: Throwable) {
                Timber.e(e, "Error updating fee payer token")
            }
        }
    }

    override fun switchCurrency() {
        val token = token ?: return
        mode = when (mode) {
            is CurrencyMode.Token -> CurrencyMode.Usd
            is CurrencyMode.Usd -> CurrencyMode.Token(token.tokenSymbol)
        }

        calculateData(token)
    }

    private fun handleFullResult(result: SearchResult.Full) {
        view?.showFullTarget(result.address, result.username)
        checkAddress(result.address)
    }

    private fun handleAddressOnlyResult(result: SearchResult.AddressOnly) {
        view?.showAddressOnlyTarget(result.address)
        checkAddress(result.address)
    }

    private fun handleEmptyBalanceResult(result: SearchResult.EmptyBalance) {
        view?.showEmptyBalanceTarget(result.address.cutEnd())
        checkAddress(result.address)
    }

    private fun handleWrongResult(result: SearchResult.Wrong) {
        view?.showWrongAddressTarget(result.address.cutEnd())
        view?.showAccountFeeView(null)
    }

    private fun handleIdleTarget() {
        view?.showIdleTarget()
        view?.showTotal(data = null)
        view?.showAccountFeeView(fee = null)
        calculateTotal(sendFee = null)
    }

    private fun checkAddress(address: String?) {
        if (address.isNullOrEmpty()) return
        val token = token ?: return

        checkAddressJob?.cancel()
        checkAddressJob = launch {
            try {
                view?.showSearchLoading(true)
                val checkAddressResult = sendInteractor.checkAddress(address.toPublicKey(), token)
                handleCheckAddressResult(checkAddressResult)
            } catch (e: CancellationException) {
                Timber.w("Cancelled check address request")
            } catch (e: Throwable) {
                Timber.e(e, "Error checking address")
            } finally {
                view?.showSearchLoading(false)
            }
        }
    }

    private suspend fun handleCheckAddressResult(checkAddressResult: CheckAddressResult) {
        when (checkAddressResult) {
            is CheckAddressResult.NewAccountNeeded -> calculateFeeRelayerFee(null)
            is CheckAddressResult.AccountExists,
            is CheckAddressResult.InvalidAddress -> view?.showAccountFeeView(null)
        }
    }

    private fun sendInBitcoin(token: Token.Active, address: String) {
        launch {
            try {
                view?.showLoading(true)
                val amount = tokenAmount.toLamports(token.decimals)
                val transactionId = burnBtcInteractor.submitBurnTransaction(address, amount)
                Timber.d("Bitcoin successfully burned and released! $transactionId")
                handleResult(SendResult.Success(transactionId))
            } catch (e: Throwable) {
                Timber.e(e, "Error sending token")
            } finally {
                view?.showLoading(false)
            }
        }
    }

    private fun sendInSolana(token: Token.Active, address: String) {
        launch {
            try {
                val destinationAddress = address.toPublicKey()
                val lamports = tokenAmount.toLamports(token.decimals)

                val data = ShowProgress(
                    title = R.string.send_transaction_being_processed,
                    subTitle = "$tokenAmount ${token.tokenSymbol} → ${destinationAddress.toBase58().cutMiddle()}",
                    transactionId = "",
                    onPrimaryCallback = { buildTransaction() }
                )
                view?.showProgressDialog(data)

                val result = sendInteractor.sendTransaction(
                    destinationAddress = destinationAddress,
                    token = token,
                    lamports = lamports
                )

                handleResult(result)
            } catch (e: Throwable) {
                Timber.e(e, "Error sending token")
                view?.showErrorMessage(e)
            } finally {
                view?.showProgressDialog(null)
            }
        }
    }

    private fun handleResult(result: SendResult) {
        when (result) {
            is SendResult.Success -> {
                buildTransaction(result.transactionId)
                view?.showSuccessMessage("${tokenAmount.toPlainString()} ${token?.tokenSymbol}")
            }
            is SendResult.WrongWallet -> view?.showWrongWalletError()
            is SendResult.Error -> view?.showErrorMessage(result.messageRes)
        }
    }

    private fun buildTransaction(transactionId: String = "") {
        val transaction = HistoryTransaction.Transfer(
            signature = transactionId,
            date = ZonedDateTime.now(),
            blockNumber = 0, // fixme: find block number
            type = TransferType.SEND,
            senderAddress = tokenKeyProvider.publicKey,
            tokenData = TokenConverter.toTokenData(token!!),
            totalInUsd = usdAmount,
            total = tokenAmount,
            destination = target!!.address,
            fee = BigInteger.ZERO
        )
        view?.showTransactionDetails(transaction)
    }

    private fun calculateData(token: Token.Active) {
        if (calculationJob?.isActive == true) return

        calculationJob = launch {
            when (mode) {
                is CurrencyMode.Token -> calculateByToken(token)
                is CurrencyMode.Usd -> calculateByUsd(token)
            }

            calculateTotal(sendFee = null)
        }
    }

    private fun calculateByUsd(token: Token.Active) {
        usdAmount = inputAmount.toBigDecimalOrZero()
        tokenAmount = if (token.usdRateOrZero.isZero()) {
            BigDecimal.ZERO
        } else {
            usdAmount.divide(token.usdRateOrZero, ROUNDING_VALUE, RoundingMode.HALF_EVEN)
                .stripTrailingZeros()
        }

        val tokenAround = if (usdAmount.isZero() || token.usdRateOrZero.isZero()) {
            BigDecimal.ZERO
        } else {
            usdAmount.divide(token.usdRateOrZero, ROUNDING_VALUE, RoundingMode.HALF_EVEN)
                .stripTrailingZeros()
        }
        view?.showTokenAroundValue(tokenAround, token.tokenSymbol)
        view?.showAvailableValue(token.totalInUsd ?: BigDecimal.ZERO, USD_READABLE_SYMBOL)

        updateButtonText(token)
        setButtonEnabled(usdAmount, token.totalInUsd ?: BigDecimal.ZERO)
    }

    private fun calculateByToken(token: Token.Active) {
        tokenAmount = inputAmount.toBigDecimalOrZero()
        usdAmount = tokenAmount.multiply(token.usdRateOrZero)

        val usdAround = tokenAmount.times(token.usdRateOrZero).scaleMedium()
        val total = token.total.scaleLong()
        view?.showUsdAroundValue(usdAround)
        view?.showAvailableValue(total, token.tokenSymbol)

        updateButtonText(token)
        setButtonEnabled(tokenAmount, total)
    }

    private fun calculateTotal(sendFee: SendFee?) {
        val sourceToken = token ?: return
        val receive = inputAmount.toBigDecimalOrZero()

        val data = SendTotal(
            total = tokenAmount,
            totalUsd = usdAmount,
            receive = "$receive ${sourceToken.tokenSymbol}",
            receiveUsd = "${receive.toUsd(sourceToken)}",
            fee = sendFee,
            sourceSymbol = sourceToken.tokenSymbol
        )

        view?.showTotal(data)
    }

    private fun calculateRenBtcFeeIfNeeded() {
        if (networkType == NetworkType.SOLANA) {
            view?.showTotal(null)
            return
        }

        if (feeJob?.isActive == true) return
        val sourceToken = token ?: return

        launch {
            val fee = burnBtcInteractor.getBurnFee()
            calculateTotal(SendFee.RenBtcFee(fee, sourceToken))
        }
    }

    /*
    * Assume this to be called only if associated account address creation needed
    * */
    private suspend fun calculateFeeRelayerFee(feePayerToken: Token.Active?) {
        val feePayer = feePayerToken ?: sendInteractor.getFeePayerToken()

        val fees = sendInteractor.calculateFeesForFeeRelayer()

        val feeAmount = if (!feePayer.isSOL && fees.feeInPayingToken != null) {
            fees.feeInPayingToken.fromLamports(feePayer.decimals).scaleMedium()
        } else {
            fees.feeInSol.fromLamports(feePayer.decimals).scaleMedium()
        }

        val fee = SendFee.SolanaFee(feeAmount, feePayer)
        view?.showAccountFeeView(fee)

        calculateTotal(fee)
    }

    private suspend fun searchByUsername(username: String) {
        val usernames = searchInteractor.searchByName(username)
        if (usernames.isEmpty()) {
            view?.showWrongAddressTarget(username)
            return
        }

        if (usernames.size > 1) {
            view?.showSearchScreen(usernames)
            return
        }

        val result = usernames.first()
        setTargetResult(result)
    }

    private suspend fun searchByNetwork(address: String) {
        when (networkType) {
            NetworkType.SOLANA -> searchByAddress(address)
            /* No search for bitcoin network */
            NetworkType.BITCOIN -> setTargetResult(SearchResult.AddressOnly(address))
        }
    }

    private suspend fun searchByAddress(address: String) {
        val validatedAddress = try {
            PublicKey(address)
        } catch (e: Throwable) {
            view?.showWrongAddressTarget(address.cutEnd())
            null
        } ?: return

        val results = searchInteractor.searchByAddress(validatedAddress.toBase58())
        if (results.isEmpty()) return

        val first = results.first()
        setTargetResult(first)
    }

    private fun updateButtonText(source: Token.Active) {
        val decimalAmount = inputAmount.toBigDecimalOrZero()
        val isMoreThanBalance = decimalAmount.isMoreThan(source.total)
        val address = target?.address

        when {
            isMoreThanBalance ->
                view?.showButtonText(R.string.swap_funds_not_enough)
            decimalAmount.isZero() ->
                view?.showButtonText(R.string.main_enter_the_amount)
            address.isNullOrBlank() ->
                view?.showButtonText(R.string.send_enter_address)
            else -> {
                val amount = "$tokenAmount ${token!!.tokenSymbol}"
                view?.showButtonText(R.string.send_format, R.drawable.ic_send_simple, amount)
            }
        }
    }

    private fun setButtonEnabled(amount: BigDecimal, total: BigDecimal) {
        val isMoreThanBalance = amount.isMoreThan(total)
        val isNotZero = !amount.isZero()
        val isValidAddress = isAddressValid(target?.address)
        val isEnabled = isNotZero && !isMoreThanBalance && isValidAddress

        val availableColor = if (isMoreThanBalance) R.color.systemError else R.color.textIconSecondary
        view?.updateAvailableTextColor(availableColor)
        view?.showButtonEnabled(isEnabled)
    }

    private fun isAddressValid(address: String?): Boolean =
        !address.isNullOrBlank() && address.trim().length >= VALID_ADDRESS_LENGTH
}