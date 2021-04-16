package com.p2p.wallet.backupwallat.secretkeys.view

import android.os.Bundle
import android.view.View
import com.p2p.wallet.R
import com.p2p.wallet.auth.ui.pin.create.CreatePinFragment
import com.p2p.wallet.auth.ui.pin.create.PinLaunchMode
import com.p2p.wallet.backupwallat.secretkeys.adapter.SecretPhraseAdapter
import com.p2p.wallet.backupwallat.secretkeys.utils.hideSoftKeyboard
import com.p2p.wallet.backupwallat.secretkeys.viewmodel.SecretKeyViewModel
import com.p2p.wallet.common.mvp.BaseFragment
import com.p2p.wallet.dashboard.model.local.Keyword
import com.p2p.wallet.databinding.FragmentSecretKeyBinding
import com.p2p.wallet.utils.popAndReplaceFragment
import com.p2p.wallet.utils.popBackStack
import com.p2p.wallet.utils.viewbinding.viewBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

@Deprecated("Should be refactored")
class SecretKeyFragment : BaseFragment(R.layout.fragment_secret_key) {

    companion object {
        fun create() = SecretKeyFragment()
    }

    private val viewModel: SecretKeyViewModel by viewModel()
    private val binding: FragmentSecretKeyBinding by viewBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backImageView.setOnClickListener {
            popBackStack()
            requireContext().hideSoftKeyboard(this@SecretKeyFragment)
        }
        binding.rvSecretPhrase.adapter = SecretPhraseAdapter(requireContext(), viewModel)
        binding.phraseET.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.rvSecretPhrase.apply {
                    (adapter as SecretPhraseAdapter).addItem(Keyword(""))
                    v.visibility = View.GONE
                    visibility = View.VISIBLE
                    binding.txtErrorMessage.text = ""
                }
            }
        }

        binding.resetButton.setOnClickListener { viewModel.resetPhrase() }

        binding.phraseET.requestFocus()

        observeData()
    }

    private fun observeData() {
        viewModel.isCurrentCombination.observe(viewLifecycleOwner) {
            popAndReplaceFragment(CreatePinFragment.create(PinLaunchMode.RECOVER), inclusive = true)
        }
        viewModel.invadedPhrase.observe(viewLifecycleOwner) { errorMessage ->
            binding.txtErrorMessage.text = errorMessage
        }
        viewModel.shouldResetThePhrase.observe(viewLifecycleOwner) {
            binding.apply {
                (rvSecretPhrase.adapter as SecretPhraseAdapter?)?.clear()
                rvSecretPhrase.visibility = View.GONE
                phraseET.visibility = View.VISIBLE
                requireContext().hideSoftKeyboard(this@SecretKeyFragment)
            }
        }
    }
}