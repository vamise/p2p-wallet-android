package org.p2p.wallet.send.ui

import android.content.Context
import android.text.SpannedString
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isVisible
import org.p2p.wallet.R
import org.p2p.wallet.databinding.WidgetSendDetailsBinding
import org.p2p.wallet.send.model.SendFee
import org.p2p.wallet.send.model.SendTotal
import org.p2p.wallet.utils.SpanUtils
import org.p2p.wallet.utils.withTextOrGone

class SendDetailsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = WidgetSendDetailsBinding.inflate(
        LayoutInflater.from(context), this
    )

    private var isExpanded: Boolean = false

    init {
        orientation = VERTICAL

        with(binding) {
            headerView.setOnClickListener {
                showExpanded(!isExpanded)
            }
            headerView.isEnabled = false
        }
    }

    fun showTotal(data: SendTotal?) {
        with(binding) {
            val buildTotalText = buildTotalText(data)
            totalTextView.text = buildTotalText

            if (data == null) {
                showExpanded(false)
                totalView.isVisible = false
                receiveView.isVisible = false
                totalFeeTextView.isVisible = false
                headerView.isEnabled = false
                return@with
            }

            headerView.isEnabled = true
            totalView.isVisible = isExpanded
            receiveView.isVisible = isExpanded

            val color = context.getColor(R.color.textIconSecondary)
            receiveTextView.text = SpanUtils.highlightText(
                data.fullReceive,
                data.approxReceive.orEmpty(),
                color
            )
            totalSourceTextView.text = SpanUtils.highlightText(
                data.fullTotal,
                data.approxTotalUsd.orEmpty(),
                color
            )

            when (data.fee) {
                is SendFee.SolanaFee -> {
                    accountCreationFeeView.isVisible = isExpanded
                    val feeText = SpanUtils.highlightText(
                        data.fee.accountCreationFullFee,
                        data.fee.approxAccountCreationFeeUsd.orEmpty(),
                        color
                    )
                    accountCreationTokenTextView.text = feeText

                    if (data.fee.isFeePayerSame(data.sourceSymbol)) {
                        totalFeeTextView.isVisible = false
                    } else {
                        totalFeeTextView.isVisible = true
                        totalFeeTextView.text = feeText
                    }
                }
                is SendFee.RenBtcFee -> {
                    accountCreationFeeView.isVisible = false
                    paidByTextView.isVisible = false
                    totalFeeTextView.isVisible = isExpanded

                    val feeText = SpanUtils.highlightText(
                        data.fee.fullFee,
                        data.fee.approxFeeUsd.orEmpty(),
                        color
                    )
                    totalFeeTextView.text = feeText
                    freeTextView.text = feeText
                }
                else -> {
                    totalFeeTextView.isVisible = false
                    paidByTextView.isVisible = true
                    freeTextView.setText(R.string.send_free_transaction)
                    accountCreationFeeView.isVisible = false
                }
            }
        }
    }

    fun showError(@StringRes errorRes: Int?) {
        val error = errorRes?.let { context.getString(it) }
        binding.errorTextView withTextOrGone error
    }

    fun setOnPaidClickListener(callback: () -> Unit) {
        binding.transactionFeeView.setOnClickListener { callback() }
    }

    private fun buildTotalText(total: SendTotal?): SpannedString {
        val totalAmount = total?.getTotalFee() ?: context.getString(R.string.swap_total_zero_sol)
        val totalText = context.getString(R.string.swap_total)
        return buildSpannedString {
            color(context.getColor(R.color.textIconSecondary)) { append(totalText) }
            bold { append(totalAmount) }
        }
    }

    private fun showExpanded(isExpanded: Boolean) {
        with(binding) {
            transactionFeeView.isVisible = isExpanded
            feeDividerView.isVisible = isExpanded
            receiveView.isVisible = isExpanded
            totalView.isVisible = isExpanded
            totalFeeTextView.isVisible = isExpanded

            val rotationValue = if (isExpanded) 180f else 0f
            arrowImageView
                .animate()
                .rotation(rotationValue)
                .start()
        }

        this.isExpanded = isExpanded
    }
}