package com.genius.srss.di.modules

import com.genius.srss.di.services.converters.IConverters
import com.genius.srss.di.services.converters.SRSSConverters
import com.genius.srss.di.services.network.INetworkSource
import com.genius.srss.di.services.network.NetworkService
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module(includes = [CoreModule::class])
abstract class AppModule {

    @Singleton
    @Binds
    abstract fun provideNetworkSource(networkService: NetworkService): INetworkSource

    @Singleton
    @Binds
    abstract fun provideConverters(converters: SRSSConverters): IConverters

//    @Singleton
//    @Binds
//    abstract fun provideCartChangeService(service: CartService): ICartChange

//    @Singleton
//    @Binds
//    abstract fun provideAppGeocoder(googleGeocoder: GoogleGeocoder): AppGeocoder

//    @Singleton
//    @Binds
//    abstract fun provideTokenProvider(firebaseTokenProvider: FirebaseTokenProvider): IDeviceTokenProvider

//    @Singleton
//    @Binds
//    @Named(value = "mock_interceptor")
//    abstract fun provideMockInterceptor(mockInterceptor: MockInterceptor): Interceptor

//    @Singleton
//    @Binds
//    abstract fun provideLocalStorage(sharedPreferences: LocalStorageSharedPreferences): ILocalStorage
}