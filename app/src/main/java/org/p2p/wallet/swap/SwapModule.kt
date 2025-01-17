package org.p2p.wallet.swap

import android.content.Context
import org.koin.dsl.bind
import org.koin.dsl.module
import org.p2p.wallet.R
import org.p2p.wallet.common.di.InjectionModule
import org.p2p.wallet.home.model.Token
import org.p2p.wallet.infrastructure.network.NetworkModule.getRetrofit
import org.p2p.wallet.rpc.interactor.TransactionAddressInteractor
import org.p2p.wallet.rpc.interactor.TransactionAmountInteractor
import org.p2p.wallet.swap.api.InternalWebApi
import org.p2p.wallet.swap.interactor.SwapInstructionsInteractor
import org.p2p.wallet.swap.interactor.SwapSerializationInteractor
import org.p2p.wallet.swap.interactor.orca.OrcaExecuteInteractor
import org.p2p.wallet.swap.interactor.orca.OrcaInstructionsInteractor
import org.p2p.wallet.swap.interactor.orca.OrcaPoolInteractor
import org.p2p.wallet.swap.interactor.orca.OrcaSwapInteractor
import org.p2p.wallet.swap.interactor.serum.SerumMarketInteractor
import org.p2p.wallet.swap.interactor.serum.SerumOpenOrdersInteractor
import org.p2p.wallet.swap.interactor.serum.SerumSwapAmountInteractor
import org.p2p.wallet.swap.interactor.serum.SerumSwapInteractor
import org.p2p.wallet.swap.interactor.serum.SerumSwapMarketInteractor
import org.p2p.wallet.swap.repository.OrcaSwapInternalRemoteRepository
import org.p2p.wallet.swap.repository.OrcaSwapInternalRepository
import org.p2p.wallet.swap.repository.OrcaSwapRemoteRepository
import org.p2p.wallet.swap.repository.OrcaSwapRepository
import org.p2p.wallet.swap.ui.orca.OrcaSwapContract
import org.p2p.wallet.swap.ui.orca.OrcaSwapPresenter

object SwapModule : InjectionModule {

    override fun create() = module {
        single {
            val baseUrl = get<Context>().getString(R.string.p2pWebBaseUrl)
            getRetrofit(baseUrl, "p2pWeb", null).create(InternalWebApi::class.java)
        }

        single {
            OrcaSwapInternalRemoteRepository(get(), get())
        } bind OrcaSwapInternalRepository::class

        single {
            SerumSwapInteractor(
                instructionsInteractor = get(),
                openOrdersInteractor = get(),
                marketInteractor = get(),
                swapMarketInteractor = get(),
                serializationInteractor = get(),
                tokenKeyProvider = get()
            )
        }

        factory { SerumMarketInteractor(get()) }
        factory { SerumOpenOrdersInteractor(get()) }
        factory { SwapSerializationInteractor(get(), get(), get(), get()) }
        factory { SerumSwapAmountInteractor(get(), get()) }
        factory { SwapInstructionsInteractor(get(), get()) }
        factory { SerumSwapMarketInteractor(get()) }

        single { OrcaPoolInteractor(get(), get(), get()) }
        single {
            OrcaSwapInteractor(
                swapRepository = get(),
                rpcRepository = get(),
                feeRelayerRepository = get(),
                internalRepository = get(),
                poolInteractor = get(),
                userInteractor = get(),
                orcaInstructionsInteractor = get(),
                transactionInteractor = get(),
                transactionManager = get(),
                tokenKeyProvider = get()
            )
        }
        factory { OrcaExecuteInteractor(get(), get()) }

        factory { OrcaInstructionsInteractor(get()) }
        factory { TransactionAddressInteractor(get(), get()) }
        single { TransactionAmountInteractor(get()) }
        factory { OrcaSwapRemoteRepository(get(), get()) } bind OrcaSwapRepository::class

        factory { (token: Token.Active?) ->
            OrcaSwapPresenter(token, get(), get(), get(), get(), get(), get(), get())
        } bind OrcaSwapContract.Presenter::class
    }
}