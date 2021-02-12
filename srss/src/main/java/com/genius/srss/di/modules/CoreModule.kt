package com.genius.srss.di.modules

import android.content.Context
import com.genius.srss.BuildConfig
import com.genius.srss.di.services.database.DatabaseService
import com.genius.srss.di.services.database.dao.SubscriptionsDao
import com.genius.srss.di.services.network.NetworkService
import dagger.Module
import dagger.Provides
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Named
import javax.inject.Singleton

@Module
object CoreModule {

//    @Singleton
//    @Provides
//    fun provideMoshi(converters: STMLConverters): Moshi = converters.moshi

//    @Singleton
//    @Provides
//    fun provideSBConverters(): STMLConverters = STMLConverters()

//    @Provides
//    @Singleton
//    @Named(value = "stimul_interceptor")
//    fun provideStimulInterceptor(accessTokenProvider: Lazy<AccessTokenProvider>): Interceptor = StimulInterceptor(accessTokenProvider)

//    @Provides
//    @Singleton
//    @Named(value = "stetho_interceptor")
//    fun provideStethoInterceptor(): Interceptor = StethoInterceptor()

    @Provides
    @Singleton
    @Named(value = "logging_interceptor")
    fun provideLoggingInterceptor(): Interceptor = HttpLoggingInterceptor().setLevel(
        when {
            BuildConfig.DEBUG -> HttpLoggingInterceptor.Level.BODY
            else -> HttpLoggingInterceptor.Level.NONE
        }
    )

//    @Provides
//    @Singleton
//    fun provideBasketDataSource(databaseService: DatabaseService): ICartDataSource {
//        return object : ICartDataSource {
//            override suspend fun getItemByRequest(request: BasketChangeRequest): CartDataRequestWrapper? {
//                when (request) {
//                    is SingleItem -> {
//                        val product = databaseService.productsDao.loadProductWithOffersByProductId(request.itemId.toInt()) ?: return null
//                        val singlePrice: Double = product.offers.firstOrNull { it.id == request.variantId?.toInt() }?.price?.toDouble() ?: product.product.price.toDouble()
//                        return CartDataRequestWrapper(product.product.id.toString(), singlePrice)
//                    }
//                    else -> throw IllegalArgumentException("Unsupported request type for class ${request::class.java.simpleName}")
//                }
//            }
//
//            override suspend fun getMultipleItemByRequest(request: BasketChangeRequest): List<CartDataRequestWrapper> {
//                throw UnsupportedOperationException("Multiple requests are not supported yet")
//            }
//        }
//    }

    @Provides
    @Singleton
    fun provideDatabaseService(context: Context): DatabaseService =
        DatabaseService.getDatabase(context)

    @Provides
    @Singleton
    fun provideSubscriptionsDao(databaseService: DatabaseService): SubscriptionsDao = databaseService.subscriptionsDao

    @Provides
    @Singleton
    fun provideOkHttpClient(apiService: NetworkService): OkHttpClient = apiService.okHttpClient
}