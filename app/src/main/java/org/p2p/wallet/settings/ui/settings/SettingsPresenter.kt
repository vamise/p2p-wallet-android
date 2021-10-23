package org.p2p.wallet.settings.ui.settings

import org.p2p.wallet.auth.interactor.AuthInteractor
import org.p2p.wallet.auth.interactor.UsernameInteractor
import org.p2p.wallet.common.mvp.BasePresenter
import org.p2p.wallet.settings.interactor.SettingsInteractor
import org.p2p.wallet.user.interactor.UserInteractor
import kotlinx.coroutines.launch

class SettingsPresenter(
    private val authInteractor: AuthInteractor,
    private val userInteractor: UserInteractor,
    private val settingsInteractor: SettingsInteractor,
    private val usernameInteractor: UsernameInteractor
) : BasePresenter<SettingsContract.View>(), SettingsContract.Presenter {

    override fun loadData() {
        val isHidden = settingsInteractor.isZerosHidden()
        view?.showHiddenBalance(isHidden)

        val usernameExists = usernameInteractor.checkUsernameExist()
        if (usernameExists.isNullOrEmpty())
            view?.openReserveUsernameScreen()
        else
            view?.openUsernameScreen()

        view?.showUsername(usernameExists)
    }

    override fun setZeroBalanceHidden(isHidden: Boolean) {
        settingsInteractor.setZeroBalanceHidden(isHidden)
        view?.showHiddenBalance(isHidden)
    }

    override fun logout() {
        launch {
            authInteractor.logout()
            userInteractor.clearMemoryData()
            view?.showAuthorization()
        }
    }

    override fun checkUsername(): Boolean {
        val username = usernameInteractor.checkUsernameExist()
        return !username.isNullOrEmpty()
    }
}