package org.p2p.wallet.send.ui

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.p2p.wallet.common.mvp.MvpPresenter
import org.p2p.wallet.common.mvp.MvpView
import org.p2p.wallet.history.model.HistoryTransaction
import org.p2p.wallet.send.model.NetworkType
import org.p2p.wallet.send.model.SearchResult
import org.p2p.wallet.send.model.SendFee
import org.p2p.wallet.send.model.SendTotal
import org.p2p.wallet.transaction.model.ShowProgress
import org.p2p.wallet.home.model.Token
import org.p2p.wallet.send.model.SendConfirmData
import java.math.BigDecimal

interface SendContract {

    interface View : MvpView {
        fun showSourceToken(token: Token.Active)
        fun showTransactionDetails(transaction: HistoryTransaction)
        fun showTotal(data: SendTotal?)
        fun showWrongWalletError()
        fun showSuccessMessage(amount: String)
        fun showButtonText(@StringRes textRes: Int, @DrawableRes iconRes: Int? = null, vararg value: String)
        fun showInputValue(value: BigDecimal)
        fun showUsdAroundValue(usdValue: BigDecimal)
        fun showTokenAroundValue(tokenValue: BigDecimal, symbol: String)
        fun showAvailableValue(available: BigDecimal, symbol: String)
        fun showButtonEnabled(isEnabled: Boolean)
        fun showFullScreenLoading(isLoading: Boolean)
        fun showLoading(isLoading: Boolean)
        fun showProgressDialog(data: ShowProgress?)
        fun updateAvailableTextColor(@ColorRes availableColor: Int)

        fun showNetworkDestination(type: NetworkType)
        fun showNetworkSelectionView(isVisible: Boolean)
        fun navigateToNetworkSelection(currentNetworkType: NetworkType)

        fun navigateToTokenSelection(tokens: List<Token.Active>)

        fun showSearchLoading(isLoading: Boolean)
        fun showIdleTarget()
        fun showWrongAddressTarget(address: String)
        fun showFullTarget(address: String, username: String)
        fun showEmptyBalanceTarget(address: String)
        fun showAddressOnlyTarget(address: String)
        fun showSearchScreen(usernames: List<SearchResult>)

        fun showAccountFeeView(fee: SendFee?)
        fun showFeePayerTokenSelector(feePayerTokens: List<Token.Active>)

        fun showBiometricConfirmationPrompt(data: SendConfirmData)
    }

    interface Presenter : MvpPresenter<View> {
        fun send()
        fun sendOrConfirm()
        fun loadInitialData()
        fun loadTokensForSelection()
        fun loadAvailableValue()
        fun loadCurrentNetwork()
        fun loadFeePayerTokens()
        fun setSourceToken(newToken: Token.Active)
        fun setTargetResult(result: SearchResult?)
        fun validateTarget(value: String)
        fun setNewSourceAmount(amount: String)
        fun switchCurrency()
        fun setNetworkDestination(networkType: NetworkType)
        fun setFeePayerToken(feePayerToken: Token.Active)
    }
}