package com.wowlet.data.di


import com.squareup.moshi.Moshi
import com.wowlet.data.dataservice.RetrofitService
import com.wowlet.data.datastore.DashboardRepository
import com.wowlet.data.datastore.PreferenceService
import com.wowlet.data.datastore.WowletApiCallRepository
import com.wowlet.data.repository.DashboardRepositoryImpl
import com.wowlet.data.repository.PreferenceServiceImpl
import com.wowlet.data.repository.WowletApiCallRepositoryImpl
import com.wowlet.data.util.HeaderInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import org.p2p.solanaj.rpc.Cluster
import org.p2p.solanaj.rpc.RpcClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory


val apiModule = module {
    single { Moshi.Builder().build() }
    single { RpcClient(Cluster.TESTNET) }
    single<Retrofit> {
        Retrofit.Builder()
            .baseUrl("https://testnet.wowlet.com/")
            .addConverterFactory(MoshiConverterFactory.create())
            .apply {
                client(
                    OkHttpClient.Builder()
                        .addInterceptor(HeaderInterceptor())
                        .addInterceptor(HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        })
                        .build()
                )
            }
            .build()
    }

    single<RetrofitService> { get<Retrofit>().create(RetrofitService::class.java) }
}


val databaseModule = module {

}

val repositoryModule = module {
    single<WowletApiCallRepository> { WowletApiCallRepositoryImpl(get(),get()) }
    single<PreferenceService> { PreferenceServiceImpl(get()) }
    single<DashboardRepository> { DashboardRepositoryImpl() }
}

