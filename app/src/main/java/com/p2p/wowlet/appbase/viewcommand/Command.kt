package com.p2p.wowlet.appbase.viewcommand

import android.os.Bundle
import com.p2p.wowlet.entities.local.ActivityItem
import com.p2p.wowlet.entities.local.EnterWallet
import com.p2p.wowlet.entities.local.WalletItem
import com.p2p.wowlet.entities.local.YourWallets

sealed class Command {

    class NetworkErrorViewCommand : ViewCommand

    /*Navigate fragment commands*/
    class FinishAppViewCommand constructor() : ViewCommand
    class NavigateUpViewCommand constructor(val destinationId: Int, val bundle: Bundle? = null) : ViewCommand
    class NavigateUpBackStackCommand constructor() : ViewCommand
    class NavigateRegLoginViewCommand constructor(val destinationId: Int) : ViewCommand
    // class NavigatePinCodeVerificationViewCommand constructor()(val destinationId: Int) : ViewCommand
    class NavigateRegFinishViewCommand constructor(val destinationId: Int) : ViewCommand
    class NavigateNotificationViewCommand constructor(val destinationId: Int) : ViewCommand
    class NavigateFingerPrintViewCommand constructor(val destinationId: Int) : ViewCommand
    class NavigateSecretKeyViewCommand constructor(val destinationId: Int) : ViewCommand

    class NavigateScannerViewCommand constructor(val destinationId: Int, val goBack: Boolean) : ViewCommand
    class OpenSendCoinDialogViewCommand constructor(val walletItem: WalletItem? = null, val walletAddress: String? = null) :
        ViewCommand

    class NavigateBlockChainViewCommand constructor(val destinationId: Int, val url: String) : ViewCommand

    class OpenProfileDialogViewCommand constructor() : ViewCommand
    class OpenMainActivityViewCommand constructor() : ViewCommand

    class OpenSwapBottomSheetViewCommand constructor(val walletData: WalletItem, val allMyWallets: List<WalletItem>) : ViewCommand
    class OpenSelectTokenToSwapBottomSheet constructor() : ViewCommand
    class OpenSlippageSettingsBottomSheet constructor() : ViewCommand

    class EnterWalletDialogViewCommand constructor(val list: List<EnterWallet>) : ViewCommand
    class YourWalletDialogViewCommand constructor(val enterWallet: EnterWallet) : ViewCommand
    class OpenMyWalletDialogViewCommand constructor() : ViewCommand
    class OpenBackupFailedDialogViewCommand constructor() : ViewCommand
    class OpenRecoveryPhraseDialogViewCommand constructor() : ViewCommand
    class OpenTransactionDialogViewCommand constructor(val itemActivity: ActivityItem) : ViewCommand
    class OpenEditWalletDialogViewCommand constructor(val walletItem: WalletItem) : ViewCommand
    class SendCoinDoneViewCommand constructor(val transactionInfo: ActivityItem) : ViewCommand
    class SendCoinViewCommand constructor() : ViewCommand
    class SwapCoinProcessingViewCommand constructor() : ViewCommand
    class OpenAllMyTokensDialogViewCommand constructor(val yourWallets: YourWallets) : ViewCommand
}