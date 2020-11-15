package com.p2p.wowlet.fragment.notification.view

import android.os.Bundle
import android.view.View
import com.p2p.wowlet.R
import com.p2p.wowlet.activity.MainActivity
import com.p2p.wowlet.appbase.FragmentBaseMVVM
import com.p2p.wowlet.appbase.utils.dataBinding
import com.p2p.wowlet.appbase.viewcommand.Command
import com.p2p.wowlet.appbase.viewcommand.ViewCommand
import com.p2p.wowlet.databinding.FragmentNotificationBinding
import com.p2p.wowlet.fragment.dashboard.dialog.backupingkey.BackingUpFromKeyDialog
import com.p2p.wowlet.fragment.notification.dialog.EnableNotificationDialog
import com.p2p.wowlet.fragment.notification.viewmodel.NotificationViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


class NotificationFragment :
    FragmentBaseMVVM<NotificationViewModel, FragmentNotificationBinding>() {

    companion object {
        fun newInstance() = NotificationFragment()
    }

    override val viewModel: NotificationViewModel by viewModel()
    override val binding: FragmentNotificationBinding by dataBinding(R.layout.fragment_notification)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.run {
            viewModel = this@NotificationFragment.viewModel
        }
        observes()
    }

    private fun observes() {
        observe(viewModel.showNotificationDialog) {
            EnableNotificationDialog() {
                viewModel.goToRefFinishFragment()
                viewModel.enableNotification()
            }.show(
                childFragmentManager,
                EnableNotificationDialog.TAG_ENABLE_NOTIFICATION_DIALOG
            )
        }
    }

    override fun processViewCommand(command: ViewCommand) {
        when (command) {
            is Command.NavigateUpViewCommand -> navigateFragment(command.destinationId)
            is Command.NavigateRegFinishViewCommand -> navigateFragment(command.destinationId)
        }
    }

    override fun navigateUp() {
        viewModel.navigateUp()
    }

}