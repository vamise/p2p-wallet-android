package org.p2p.wallet.settings.ui.reset

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import org.p2p.wallet.R
import org.p2p.wallet.common.mvp.BaseMvpFragment
import org.p2p.wallet.databinding.FragmentChangePinBinding
import org.p2p.wallet.utils.BiometricPromptWrapper
import org.p2p.wallet.utils.popAndReplaceFragment
import org.p2p.wallet.utils.vibrate
import org.p2p.wallet.utils.viewbinding.viewBinding
import org.koin.android.ext.android.inject
import org.p2p.wallet.utils.SpanUtils
import javax.crypto.Cipher

class ResetPinFragment :
    BaseMvpFragment<ResetPinContract.View, ResetPinContract.Presenter>(R.layout.fragment_change_pin),
    ResetPinContract.View {

    companion object {
        fun create() = ResetPinFragment()
    }

    override val presenter: ResetPinContract.Presenter by inject()

    private val binding: FragmentChangePinBinding by viewBinding()

    private val biometricWrapper by lazy {
        BiometricPromptWrapper(
            this,
            onError = { presenter.resetPinWithoutBiometrics() },
            onSuccess = { presenter.resetPinWithBiometrics(it) }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            pinView.onPinCompleted = { presenter.setPinCode(it) }
            resetTextView.text = buildResetText()
            resetTextView.setOnClickListener {
                TODO()
            }
        }
    }

    override fun showResetSuccess() {
        popAndReplaceFragment(ResetSuccessFragment.create())
    }

    override fun showEnterNewPin() {
        binding.toolbar.setTitle(R.string.settings_security_change_enter_pin)
        binding.pinView.clearPin()
    }

    override fun showConfirmationError() {
        val message = getString(R.string.settings_security_pin_codes_matching)
        binding.pinView.startErrorAnimation(message)
        binding.pinView.clearPin()
    }

    override fun showLoading(isLoading: Boolean) {
        binding.pinView.showLoading(isLoading)
    }

    override fun showCurrentPinIncorrectError() {
        val message = getString(R.string.auth_pin_code_wrong)
        binding.pinView.startErrorAnimation(message)
    }

    override fun showConfirmNewPin() {
        binding.toolbar.setTitle(R.string.settings_security_change_confirm_pin)
        binding.pinView.clearPin()
    }

    override fun showWalletLocked(seconds: Long) {
        val message = getString(R.string.auth_locked_message, seconds.toString())
        binding.pinView.showLockedState(message)
    }

    override fun showWalletUnlocked() {
        binding.pinView.showUnlockedState()
    }

    override fun clearPin() {
        binding.pinView.clearPin()
    }

    override fun showBiometricDialog(cipher: Cipher) {
        biometricWrapper.authenticate(cipher)
    }

    override fun vibrate(duration: Long) {
        requireContext().vibrate(duration)
    }

    private fun buildResetText(): SpannableString {
        val message = getString(R.string.settings_forgot_your_pin)
        val resetMessage = getString(R.string.settings_reset_it)

        val span = SpanUtils.setTextBold(message, resetMessage)

        val clickableReset = object : ClickableSpan() {
            override fun onClick(widget: View) {
                TODO()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }

        val resetStart = span.indexOf(resetMessage)
        val resetEnd = span.indexOf(resetMessage) + resetMessage.length
        span.setSpan(clickableReset, resetStart, resetEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        return span
    }
}