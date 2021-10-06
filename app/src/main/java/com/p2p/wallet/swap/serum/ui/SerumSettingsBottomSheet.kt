package com.p2p.wallet.swap.serum.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.p2p.wallet.R
import com.p2p.wallet.databinding.DialogSwapSettingsBinding
import com.p2p.wallet.swap.model.Slippage
import com.p2p.wallet.utils.args
import com.p2p.wallet.utils.viewbinding.viewBinding
import com.p2p.wallet.utils.withArgs

class SerumSettingsBottomSheet(
    private val onSlippageSelected: (Double) -> Unit
) : BottomSheetDialogFragment() {

    companion object {
        private const val EXTRA_SLIPPAGE = "EXTRA_SLIPPAGE"
        fun show(fm: FragmentManager, slippage: Slippage, onSlippageSelected: (Double) -> Unit) {
            SerumSettingsBottomSheet(onSlippageSelected)
                .withArgs(
                    EXTRA_SLIPPAGE to slippage,
                )
                .show(fm, SerumSlippageBottomSheet::javaClass.name)
        }
    }

    private val binding: DialogSwapSettingsBinding by viewBinding()

    private val slippage: Slippage by args(EXTRA_SLIPPAGE)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.dialog_swap_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            slippageView.setBottomText("${slippage.doubleValue} %")
            slippageView.setOnClickListener { openSlippage() }
            payView.setOnClickListener {
                Toast.makeText(requireContext(), "Not implemented yet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openSlippage() {
        SerumSlippageBottomSheet.show(parentFragmentManager, slippage) {
            onSlippageSelected(it)
            binding.slippageView.setBottomText("$it %")
        }
    }

    override fun getTheme(): Int = R.style.WalletTheme_BottomSheet_Rounded
}