package org.p2p.wallet.settings.ui.settings

import org.p2p.wallet.auth.model.Username
import org.p2p.wallet.common.mvp.MvpPresenter
import org.p2p.wallet.common.mvp.MvpView

interface SettingsContract {

    interface View : MvpView {
        fun showHiddenBalance(isHidden: Boolean)
        fun showAuthorization()
        fun showUsername(username: Username?)
        fun openUsernameScreen()
        fun openReserveUsernameScreen()
    }

    interface Presenter : MvpPresenter<View> {
        fun loadData()
        fun setZeroBalanceHidden(isHidden: Boolean)
        fun logout()
        fun onUsernameClicked()
    }
}