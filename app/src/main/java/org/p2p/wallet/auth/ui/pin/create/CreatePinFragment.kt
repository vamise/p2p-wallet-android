package org.p2p.wallet.auth.ui.pin.create

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import org.p2p.wallet.R
import org.p2p.wallet.auth.ui.biometric.BiometricFragment
import org.p2p.wallet.auth.ui.done.AuthDoneFragment
import org.p2p.wallet.auth.ui.onboarding.OnboardingFragment
import org.p2p.wallet.auth.ui.welcomeback.WelcomeBackFragment
import org.p2p.wallet.common.mvp.BaseMvpFragment
import org.p2p.wallet.databinding.FragmentCreatePinBinding
import org.p2p.wallet.utils.args
import org.p2p.wallet.utils.edgetoedge.Edge
import org.p2p.wallet.utils.edgetoedge.edgeToEdge
import org.p2p.wallet.utils.popAndReplaceFragment
import org.p2p.wallet.utils.popBackStackTo
import org.p2p.wallet.utils.replaceFragment
import org.p2p.wallet.utils.vibrate
import org.p2p.wallet.utils.viewbinding.viewBinding
import org.p2p.wallet.utils.withArgs
import org.koin.android.ext.android.inject

class CreatePinFragment :
    BaseMvpFragment<CreatePinContract.View, CreatePinContract.Presenter>(R.layout.fragment_create_pin),
    CreatePinContract.View {

    companion object {
        private const val EXTRA_LAUNCH_MODE = "EXTRA_LAUNCH_MODE"
        fun create(mode: PinLaunchMode) =
            CreatePinFragment()
                .withArgs(EXTRA_LAUNCH_MODE to mode)
    }

    override val presenter: CreatePinContract.Presenter by inject()

    private val binding: FragmentCreatePinBinding by viewBinding()

    private val mode: PinLaunchMode by args(EXTRA_LAUNCH_MODE)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            edgeToEdge {
                contentView.fitMargin { Edge.Bottom }
            }
            pinView.onPinCompleted = {
                presenter.setPinCode(it)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            popBackStackTo(OnboardingFragment::class)
        }
    }

    override fun showLoading(isLoading: Boolean) {
        binding.pinView.showLoading(isLoading)
    }

    override fun showCreation() {
        with(binding) {
            pinView.isEnabled = true
            infoTextView.setText(R.string.auth_create_pin_code)
            pinView.clearPin()
        }
    }

    override fun showConfirmation() {
        with(binding) {
            pinView.isEnabled = true
            infoTextView.setText(R.string.auth_repeat_new_pin_code)
            pinView.clearPin()
        }
    }

    override fun onAuthFinished() {
        when (mode) {
            PinLaunchMode.RECOVER -> popAndReplaceFragment(WelcomeBackFragment.create(), inclusive = true)
            PinLaunchMode.CREATE -> popAndReplaceFragment(AuthDoneFragment.create(), inclusive = true)
        }
    }

    override fun navigateToBiometric(createdPin: String) {
        replaceFragment(BiometricFragment.create(createdPin))
    }

    override fun showConfirmationError() {
        binding.pinView.startErrorAnimation(getString(R.string.auth_pin_codes_match_error))
    }

    override fun lockPinKeyboard() {
        binding.pinView.isEnabled = false
    }

    override fun vibrate(duration: Long) {
        requireContext().vibrate(duration)
    }
}