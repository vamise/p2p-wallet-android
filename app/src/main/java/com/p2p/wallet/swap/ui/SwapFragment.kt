package com.p2p.wallet.swap.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.bumptech.glide.Glide
import com.p2p.wallet.R
import com.p2p.wallet.common.mvp.BaseMvpFragment
import com.p2p.wallet.databinding.FragmentSwapBinding
import com.p2p.wallet.main.ui.select.SelectTokenFragment
import com.p2p.wallet.main.ui.transaction.TransactionInfo
import com.p2p.wallet.main.ui.transaction.TransactionStatusBottomSheet
import com.p2p.wallet.swap.model.Slippage
import com.p2p.wallet.token.model.Token
import com.p2p.wallet.utils.addFragment
import com.p2p.wallet.utils.popBackStack
import com.p2p.wallet.utils.viewbinding.viewBinding
import org.koin.android.ext.android.inject
import java.math.BigDecimal
import java.math.BigInteger

class SwapFragment :
    BaseMvpFragment<SwapContract.View, SwapContract.Presenter>(R.layout.fragment_swap),
    SwapContract.View {

    companion object {
        fun create() = SwapFragment()
    }

    override val presenter: SwapContract.Presenter by inject()

    private val binding: FragmentSwapBinding by viewBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            toolbar.setNavigationOnClickListener { popBackStack() }
            toolbar.setOnMenuItemClickListener { menu ->
                if (menu.itemId == R.id.slippageMenuItem) {
                    presenter.loadSlippageForSelection()
                    return@setOnMenuItemClickListener true
                }
                return@setOnMenuItemClickListener false
            }
            sourceImageView.setOnClickListener { presenter.loadTokensForSourceSelection() }
            destinationImageView.setOnClickListener { presenter.loadTokensForDestinationSelection() }
            availableTextView.setOnClickListener { presenter.feedAvailableValue() }
            amountEditText.doAfterTextChanged {
                presenter.setSourceAmount(it.toString())
            }

            reverseImageView.setOnClickListener {
                presenter.loadPrice(true)
            }

            swapButton.setOnClickListener { presenter.swap() }
        }

        presenter.loadInitialData()
    }

    override fun showSourceToken(token: Token) {
        with(binding) {
            Glide.with(sourceImageView).load(token.iconUrl).into(sourceImageView)
            sourceTextView.text = token.tokenSymbol
            availableTextView.text = getString(R.string.main_send_available, token.getFormattedTotal())
        }
    }

    override fun showDestinationToken(token: Token) {
        with(binding) {
            Glide.with(destinationImageView).load(token.iconUrl).into(destinationImageView)
            destinationTextView.text = token.tokenSymbol
            currencyTextView.isInvisible = true
        }
    }

    override fun updateInputValue(available: BigDecimal) {
        binding.amountEditText.setText("$available")
    }

    @SuppressLint("SetTextI18n")
    override fun showPrice(amount: Double, exchangeToken: String, perToken: String) {
        binding.priceGroup.isVisible = true
        binding.exchangeTextView.text = "$amount $exchangeToken per $perToken"
    }

    @SuppressLint("SetTextI18n")
    override fun showCalculations(data: CalculationsData) {
        binding.calculationsGroup.isVisible = true
        binding.receiveValueTextView.text = "${data.minReceive} ${data.minReceiveSymbol}"
        binding.feeValueTextView.text = "${data.fee} ${data.feeSymbol}"
        binding.destinationAmountTextView.text = data.destinationAmount
        binding.slippageValueTextView.text = "${data.slippage}"
    }

    override fun setAvailableTextColor(@ColorRes availableColor: Int) {
        binding.availableTextView.setTextColor(ContextCompat.getColor(requireContext(), availableColor))
    }

    override fun showAroundValue(aroundValue: BigDecimal) {
        binding.aroundTextView.text = getString(R.string.main_send_around_in_usd, aroundValue)
    }

    override fun showButtonEnabled(isEnabled: Boolean) {
        binding.swapButton.isEnabled = isEnabled
    }

    override fun showSwapSuccess(info: TransactionInfo) {
        TransactionStatusBottomSheet.show(
            fragment = this,
            info = info,
            onDismiss = { popBackStack() }
        )
    }

    override fun showNoPoolFound() {
        Toast.makeText(requireContext(), "Pool not found", Toast.LENGTH_SHORT).show()
    }

    override fun openSourceSelection(tokens: List<Token>) {
        addFragment(
            target = SelectTokenFragment.create(tokens) { presenter.setNewSourceToken(it) },
            enter = R.anim.slide_up,
            exit = 0,
            popExit = R.anim.slide_down,
            popEnter = 0
        )
    }

    override fun openDestinationSelection(tokens: List<Token>) {
        addFragment(
            target = SelectTokenFragment.create(tokens) { presenter.setNewDestinationToken(it) },
            enter = R.anim.slide_up,
            exit = 0,
            popExit = R.anim.slide_down,
            popEnter = 0
        )
    }

    override fun openSlippageSelection(currentSlippage: Slippage) {
        SlippageBottomSheet.show(childFragmentManager, currentSlippage) { presenter.setSlippage(it) }
    }

    override fun showFullScreenLoading(isLoading: Boolean) {
        binding.progressView.isVisible = isLoading
    }

    override fun showLoading(isLoading: Boolean) {
        binding.buttonProgressBar.isVisible = isLoading
        binding.swapButton.isVisible = !isLoading
    }
}