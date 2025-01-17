package org.p2p.wallet.settings.ui.settings

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import org.koin.android.ext.android.inject
import org.p2p.wallet.R
import org.p2p.wallet.auth.model.ReserveMode
import org.p2p.wallet.auth.ui.username.ReserveUsernameFragment
import org.p2p.wallet.auth.ui.username.UsernameFragment
import org.p2p.wallet.common.mvp.BaseMvpFragment
import org.p2p.wallet.databinding.FragmentSettingsBinding
import org.p2p.wallet.settings.model.SettingsRow
import org.p2p.wallet.settings.ui.network.SettingsNetworkFragment
import org.p2p.wallet.settings.ui.reset.ResetPinFragment
import org.p2p.wallet.settings.ui.security.SecurityFragment
import org.p2p.wallet.settings.ui.zerobalances.SettingsZeroBalanceFragment
import org.p2p.wallet.utils.addFragment
import org.p2p.wallet.utils.attachAdapter
import org.p2p.wallet.utils.replaceFragment
import org.p2p.wallet.utils.showInfoDialog
import org.p2p.wallet.utils.viewbinding.viewBinding

private const val EXTRA_REQUEST_KEY = "EXTRA_REQUEST_KEY"
private const val EXTRA_NETWORK_NAME = "EXTRA_NETWORK_NAME"
private const val EXTRA_IS_PIN_CHANGED = "EXTRA_IS_PIN_CHANGED"

class SettingsFragment :
    BaseMvpFragment<SettingsContract.View, SettingsContract.Presenter>(R.layout.fragment_settings),
    SettingsContract.View {

    companion object {

        fun create() = SettingsFragment()
    }

    override val presenter: SettingsContract.Presenter by inject()

    private val binding: FragmentSettingsBinding by viewBinding()
    private val adapter = SettingsAdapter(::onItemClickListener, ::onLogoutClickListener)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            recyclerView.attachAdapter(adapter)
        }

        requireActivity().supportFragmentManager.setFragmentResultListener(
            EXTRA_REQUEST_KEY,
            viewLifecycleOwner
        ) { _, result ->
            when {
                result.containsKey(EXTRA_IS_PIN_CHANGED) -> {
                    val isPinChanged = result.getBoolean(EXTRA_IS_PIN_CHANGED)
                    if (isPinChanged) {
                        showSnackbar(
                            message = getString(R.string.settings_a_new_wallet_pin_is_set),
                            iconRes = R.drawable.ic_done
                        )
                    }
                }
                result.containsKey(EXTRA_NETWORK_NAME) -> {
                    val networkName = result.getString(EXTRA_NETWORK_NAME)
                    if (!networkName.isNullOrEmpty()) {
                        presenter.onNetworkChanged(newName = networkName)
                    }
                }
            }
        }
        presenter.loadData()
    }

    override fun showSettings(item: List<SettingsRow>) {
        adapter.setData(item)
    }

    override fun showReserveUsername() {
        replaceFragment(ReserveUsernameFragment.create(ReserveMode.POP))
    }

    override fun showUsername() {
        replaceFragment(UsernameFragment.create())
    }

    private fun onItemClickListener(@StringRes titleResId: Int) {
        when (titleResId) {
            R.string.settings_username -> presenter.onUsernameClicked()

            R.string.settings_wallet_pin -> {
                replaceFragment(ResetPinFragment.create(EXTRA_REQUEST_KEY, EXTRA_IS_PIN_CHANGED))
            }
            R.string.settings_app_security -> {
                replaceFragment(SecurityFragment.create())
            }
            R.string.settings_network -> {
                addFragment(
                    SettingsNetworkFragment.create(EXTRA_REQUEST_KEY, EXTRA_NETWORK_NAME),
                    enter = 0,
                    exit = 0,
                    popEnter = 0,
                    popExit = 0
                )
            }
            R.string.settings_zero_balances -> {
                addFragment(
                    SettingsZeroBalanceFragment.create(),
                    enter = 0,
                    exit = 0,
                    popEnter = 0,
                    popExit = 0
                )
            }
        }
    }

    private fun onLogoutClickListener() {
        showInfoDialog(
            titleRes = R.string.settings_logout_title,
            messageRes = R.string.settings_logout_message,
            primaryButtonRes = R.string.common_logout,
            primaryCallback = { presenter.logout() },
            secondaryButtonRes = R.string.common_stay,
            secondaryCallback = { },
            primaryButtonTextColor = R.color.systemErrorMain
        )
    }
}