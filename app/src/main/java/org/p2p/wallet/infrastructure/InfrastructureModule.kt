package org.p2p.wallet.infrastructure

import android.content.Context
import androidx.room.Room
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.p2p.wallet.common.crypto.keystore.EncoderDecoderMarshmallow
import org.p2p.wallet.common.crypto.keystore.KeyStoreWrapper
import org.p2p.wallet.common.di.InjectionModule
import org.p2p.wallet.common.glide.GlideManager
import org.p2p.wallet.infrastructure.db.WalletDatabase
import org.p2p.wallet.infrastructure.db.WalletDatabase.Companion.DATABASE_NAME
import org.p2p.wallet.infrastructure.security.SecureStorage
import org.p2p.wallet.infrastructure.security.SecureStorageContract
import org.p2p.wallet.infrastructure.update.TransactionSignatureHandler
import org.p2p.wallet.notification.AppNotificationManager
import org.p2p.wallet.updates.SocketUpdatesManager
import org.p2p.wallet.updates.UpdateHandler
import org.p2p.wallet.updates.UpdatesManager
import java.util.concurrent.Executors

object InfrastructureModule : InjectionModule {

    override fun create() = module {
        single {
            Room
                .databaseBuilder(get(), WalletDatabase::class.java, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .setQueryExecutor(Executors.newCachedThreadPool())
                .build()
        }

        single { get<WalletDatabase>().tokenDao() }
        single { get<WalletDatabase>().sessionDao() }

        single {
            val context = get<Context>()
            val name = "${context.packageName}.prefs"
            context.getSharedPreferences(name, Context.MODE_PRIVATE)
        }

        single {
            val encoderDecoder = EncoderDecoderMarshmallow(get())
            return@single KeyStoreWrapper(encoderDecoder)
        }

        factory { SecureStorage(get(), get()) } bind SecureStorageContract::class

        single { GlideManager(get()) }

        single {
            val updateHandlers = get<List<UpdateHandler>>(named<UpdateHandler>())
            SocketUpdatesManager(get(), get(), get(), updateHandlers)
        } bind UpdatesManager::class
        single(named<UpdateHandler>()) {
            listOf(
                TransactionSignatureHandler(get())
            )
        }

        single { AppNotificationManager(get()) }
    }
}