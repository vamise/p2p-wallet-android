package com.p2p.wowlet.fragment.pincode.view

import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.Toast
import com.p2p.wowlet.R
import com.p2p.wowlet.activity.MainActivity
import com.p2p.wowlet.appbase.FragmentBaseMVVM
import com.p2p.wowlet.appbase.utils.dataBinding
import com.p2p.wowlet.appbase.viewcommand.Command
import com.p2p.wowlet.appbase.viewcommand.ViewCommand
import com.p2p.wowlet.databinding.FragmentPinCodeBinding
import com.p2p.wowlet.fragment.backupwallat.secretkeys.view.SecretKeyFragment
import com.p2p.wowlet.fragment.dashboard.view.DashboardFragment
import com.p2p.wowlet.fragment.fingetprint.view.FingerPrintFragment
import com.p2p.wowlet.fragment.notification.view.NotificationFragment
import com.p2p.wowlet.fragment.pincode.adapter.PinButtonAdapter
import com.p2p.wowlet.fragment.pincode.viewmodel.PinCodeViewModel
import com.p2p.wowlet.fragment.regfinish.view.RegFinishFragment
import com.p2p.wowlet.fragment.reglogin.view.RegLoginFragment
import com.p2p.wowlet.utils.isFingerPrintSet
import com.p2p.wowlet.utils.openFingerprintDialog
import com.p2p.wowlet.utils.popBackStack
import com.p2p.wowlet.utils.popBackStackTo
import com.p2p.wowlet.utils.replace
import com.p2p.wowlet.utils.withArgs
import com.wowlet.entities.enums.PinCodeFragmentType
import kotlinx.android.synthetic.main.fragment_pin_code.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class PinCodeFragment : FragmentBaseMVVM<PinCodeViewModel, FragmentPinCodeBinding>() {
    override val viewModel: PinCodeViewModel by viewModel()
    override val binding: FragmentPinCodeBinding by dataBinding(R.layout.fragment_pin_code)

    companion object {
        const val OPEN_FRAGMENT_SPLASH_SCREEN = "openFragmentSplashScreen"
        const val OPEN_FRAGMENT_BACKUP_DIALOG = "openFragmentBackupDialog"
        const val CREATE_NEW_PIN_CODE = "createNewPinCode"

        fun create(
            openSplashScreen: Boolean,
            isBackupDialog: Boolean,
            type: PinCodeFragmentType
        ): PinCodeFragment =
            PinCodeFragment().withArgs(
                OPEN_FRAGMENT_SPLASH_SCREEN to openSplashScreen,
                OPEN_FRAGMENT_BACKUP_DIALOG to isBackupDialog,
                CREATE_NEW_PIN_CODE to type
            )
    }

    private var isSplashScreen: Boolean = false
    private var isBackupDialog: Boolean = false
    private var isFingerprintEnabled: Boolean = false
    private lateinit var pinCodeFragmentType: PinCodeFragmentType
    private lateinit var pinButtonAdapter: PinButtonAdapter

    override fun initView() {
        viewModel.fingerPrintStatus()
        binding.run {
            viewModel = this@PinCodeFragment.viewModel

            Log.i("FingerPring", "initView: " + requireActivity().isFingerPrintSet())
            pinView.pinCodeFragmentType = pinCodeFragmentType
            pinView.setMaxPinSize(6)
            context?.let { context ->
                pinButtonAdapter = PinButtonAdapter(
                    isFingerprintEnabled,
                    context,
                    pinCodeFragmentType,
                    pinButtonClick = {
                        pinView.onPinButtonClicked(text = it)
                        vPinCodeNotMatch.visibility = View.INVISIBLE
                    },
                    pinFingerPrint = {
                        this@PinCodeFragment.viewModel.openFingerprintDialog()
                    },
                    removeCode = {
                        pinView.onDeleteButtonClicked()
                        vPinCodeNotMatch.visibility = View.INVISIBLE
                    })
                gridView.adapter = pinButtonAdapter
            }
            gridView.numColumns = 3

            resetPinCode.setOnClickListener {
                replace(SecretKeyFragment())
            }
        }
        initPinCodeMassage()
    }

    override fun initData() {
        arguments?.let {
            isSplashScreen = it.getBoolean(OPEN_FRAGMENT_SPLASH_SCREEN, false)
            isBackupDialog = it.getBoolean(OPEN_FRAGMENT_BACKUP_DIALOG, false)
            pinCodeFragmentType = it.get(CREATE_NEW_PIN_CODE) as PinCodeFragmentType
        }
    }

    private fun initPinCodeMassage() {
        if (isSplashScreen || isBackupDialog)
            vPinCodeMessage.text = getString(R.string.enter_the_code)
        else
            vPinCodeMessage.text = getString(R.string.create_a_pin_code_info)
    }

    override fun observes() {
        observe(viewModel.pinCodeSuccess) {
            if (isFingerprintEnabled) {
                if (isBackupDialog)
                    replace(DashboardFragment.create(true))
                else
                    viewModel.notificationStatus()
            } else {
                if (pinCodeFragmentType == PinCodeFragmentType.CREATE) {
                    replace(FingerPrintFragment())
                } else {
                    if (isBackupDialog)
                        replace(DashboardFragment.create(true))
                    else
                        viewModel.notificationStatus()
                }
            }
        }
        observe(viewModel.pinCodeSaved) {
            vPinCodeMessage.text = getString(R.string.confirm_pin_code)
            binding.pinView.clearPin()
            binding.pinView.isFirstPinInput = true
        }
        observe(viewModel.pinCodeError) {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
        observe(viewModel.isSkipFingerPrint) {
            isFingerprintEnabled = it
            pinButtonAdapter.updateFingerprintStatus(isFingerprintEnabled)
            if (it) {
                childFragmentManager.executePendingTransactions()
                viewModel.openFingerprintDialog()
            }
        }
        observe(viewModel.skipNotification) {
            if (it) {
                replace(RegFinishFragment())
            } else {
                replace(NotificationFragment())
            }
        }

        observe(viewModel.verifyPinCodeError) {
            when (pinCodeFragmentType) {
                PinCodeFragmentType.CREATE -> {
                    vPinCodeNotMatch.text = getString(R.string.pin_codes_invalid)
                    //vPinCodeMessage.text = getString(R.string.create_a_pin_code_info)
                    binding.pinView.isFirstPinInput = true
                    binding.pinView.clearPin()
                }
                PinCodeFragmentType.VERIFY -> {
                    binding.pinView.errorPinViewsDesign()
                    if (pinCodeFragmentType == PinCodeFragmentType.VERIFY)
                        binding.resetPinCode.visibility = View.VISIBLE
                    else
                        binding.resetPinCode.visibility = View.GONE
                    if (isBackupDialog)
                        vPinCodeNotMatch.text = getString(R.string.incorrect_password)
                    else
                        when (binding.pinView.wrongPinCodeCount) {
                            1 -> vPinCodeNotMatch.text =
                                getString(R.string.wrong_pin_code_left_2)
                            2 -> vPinCodeNotMatch.text =
                                getString(R.string.wrong_pin_code_left_1)
                            else -> vPinCodeNotMatch.text =
                                getString(R.string.wrong_pin_code_block)
                        }
                }
            }
        }
        observe(viewModel.openFingerprintDialog) {
            openFingerprintDialog {
                if (isBackupDialog)
                    replace(DashboardFragment.create(true))
                else
                    replace(FingerPrintFragment())
            }
        }

        binding.pinView.createPinCode = {
            viewModel.initCode(it)
        }
        binding.pinView.verifyPinCode = {
            viewModel.verifyPinCode(it)
        }
    }

    override fun processViewCommand(command: ViewCommand) {
        when (command) {
            is Command.NavigateUpViewCommand -> popBackStack()
            is Command.NavigateSecretKeyViewCommand -> replace(SecretKeyFragment())
            is Command.NavigateFingerPrintViewCommand -> replace(FingerPrintFragment())
            is Command.NavigateNotificationViewCommand -> replace(NotificationFragment())
            is Command.NavigateRegLoginViewCommand -> replace(RegLoginFragment())
            is Command.NavigateRegFinishViewCommand -> replace(RegFinishFragment())
            is Command.OpenMainActivityViewCommand -> {
                activity?.let {
                    val intent = Intent(it, MainActivity::class.java)
                    it.startActivity(intent)
                    it.finish()
                }
            }
        }
    }

//    override fun navigateUp() {
//        if (isSplashScreen)
//            requireActivity().finish()
//        else if (isBackupDialog) {
//            replace(DashboardFragment.create(false))
//        } else {
//            if (pinCodeFragmentType == PinCodeFragmentType.CREATE && binding.pinView.isFirstPinInput) {
//                vPinCodeMessage.text = getString(R.string.create_a_pin_code_info)
//                binding.vPinCodeNotMatch.visibility = View.GONE
//                binding.pinView.isFirstPinInput = false
//            } else {
//                popBackStackTo(RegLoginFragment::class)
//            }
//        }
//    }
}