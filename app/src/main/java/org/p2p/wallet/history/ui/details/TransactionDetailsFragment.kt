package org.p2p.wallet.history.ui.details

import android.os.Bundle
import android.text.SpannableString
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import org.p2p.wallet.R
import org.p2p.wallet.common.mvp.BaseMvpFragment
import org.p2p.wallet.common.ui.bottomsheet.DrawableContainer
import org.p2p.wallet.databinding.FragmentTransactionTransferBinding
import org.p2p.wallet.history.model.HistoryTransaction
import org.p2p.wallet.utils.args
import org.p2p.wallet.utils.copyToClipBoard
import org.p2p.wallet.utils.cutEnd
import org.p2p.wallet.utils.getColor
import org.p2p.wallet.utils.popBackStack
import org.p2p.wallet.utils.showUrlInCustomTabs
import org.p2p.wallet.utils.toast
import org.p2p.wallet.utils.viewbinding.viewBinding
import org.p2p.wallet.utils.withArgs
import org.p2p.wallet.utils.withTextOrGone

private const val EXTRA_TRANSACTION = "EXTRA_TRANSACTION"

class TransactionDetailsFragment :
    BaseMvpFragment<TransactionDetailsContract.View, TransactionDetailsContract.Presenter>(
        R.layout.fragment_transaction_transfer
    ),
    TransactionDetailsContract.View {

    companion object {
        fun create(transaction: HistoryTransaction) =
            TransactionDetailsFragment()
                .withArgs(EXTRA_TRANSACTION to transaction)
    }

    private val transaction: HistoryTransaction by args(EXTRA_TRANSACTION)

    private val binding: FragmentTransactionTransferBinding by viewBinding()

    override val presenter: TransactionDetailsContract.Presenter by inject {
        parametersOf(transaction)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { popBackStack() }
    }

    override fun showTitle(title: String) {
        binding.toolbar.title = title
    }

    override fun showDate(date: String) {
        binding.dateTextView.text = date
    }

    override fun showSourceInfo(iconContainer: DrawableContainer, primaryInfo: String, secondaryInfo: String?) {
        with(binding) {
            iconContainer.applyTo(sourceImageView)
            sourceTextView.text = primaryInfo
            sourceSecondaryTextView withTextOrGone secondaryInfo
        }
    }

    override fun showDestinationInfo(iconContainer: DrawableContainer, primaryInfo: String, secondaryInfo: String?) {
        with(binding) {
            iconContainer.applyTo(destinationImageView)
            destinationTextView.text = primaryInfo
            destinationSecondaryTextView withTextOrGone secondaryInfo
        }
    }

    override fun showSignature(signature: String) {
        with(binding) {
            transactionIdTitleTextView.setOnClickListener {
                requireContext().copyToClipBoard(signature)
                toast(R.string.common_copied)
            }
            transactionIdTextView.text = signature.cutEnd()
            explorerView.clipToOutline = true
            explorerView.setOnClickListener {
                val url = getString(R.string.solanaExplorer, signature)
                showUrlInCustomTabs(url)
            }
        }
    }

    override fun showAddresses(source: String, destination: String) {
        with(binding) {
            sourceTitleTextView.setOnClickListener {
                requireContext().copyToClipBoard(source)
                toast(R.string.common_copied)
            }
            sourceAddressTextView.text = source

            destinationTitleTextView.setOnClickListener {
                requireContext().copyToClipBoard(destination)
                toast(R.string.common_copied)
            }
            destinationAddressTextView.text = destination
        }
    }

    override fun showAmount(@StringRes label: Int, amount: CharSequence) {
        with(binding) {
            amountLabelTextView.setText(label)
            amountTextView.text = amount
        }
    }

    override fun showLiquidityProviderFees(sourceFee: SpannableString, destinationFee: SpannableString) {
        // todo: add fees
    }

    override fun showFee(renBtcFee: String?) {
        with(binding) {
            if (renBtcFee.isNullOrEmpty()) {
                feesTextView.text = getString(R.string.send_free_transaction)
                feesTextView.setTextColor(getColor(R.color.systemSuccessMain))
                freeTextView.isVisible = true
            } else {
                feesTextView.text = renBtcFee
            }
        }
    }

    override fun showBlockNumber(blockNumber: String) {
        binding.blockNumberTextView.text = blockNumber
    }

    override fun showLoading(isLoading: Boolean) {
        with(binding) {
            scrollView.isVisible = !isLoading
            progressView.isVisible = isLoading
        }
    }
}