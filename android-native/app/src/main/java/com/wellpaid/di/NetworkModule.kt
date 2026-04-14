package com.wellpaid.di

import android.content.Context
import android.content.pm.ApplicationInfo
import com.wellpaid.BuildConfig
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.network.CategoriesApi
import com.wellpaid.core.network.DashboardApi
import com.wellpaid.core.network.EmergencyReserveApi
import com.wellpaid.core.network.FamiliesApi
import com.wellpaid.core.network.ExpensesApi
import com.wellpaid.core.network.GoalsApi
import com.wellpaid.core.network.IncomeCategoriesApi
import com.wellpaid.core.network.IncomesApi
import com.wellpaid.core.network.ShoppingListsApi
import com.wellpaid.core.network.UserApi
import com.wellpaid.core.network.WellPaidRetrofit
import com.wellpaid.core.network.auth.AuthApi
import com.wellpaid.core.network.auth.AuthHeaderInterceptor
import com.wellpaid.core.network.auth.TokenAuthenticator
import com.wellpaid.core.network.auth.TokenRefresher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthHeaderInterceptor(tokenStorage: TokenStorage): AuthHeaderInterceptor =
        AuthHeaderInterceptor(tokenStorage)

    @Provides
    @Singleton
    fun provideTokenRefresher(authApi: AuthApi, tokenStorage: TokenStorage): TokenRefresher =
        TokenRefresher(authApi, tokenStorage)

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        tokenStorage: TokenStorage,
        tokenRefresher: TokenRefresher,
    ): TokenAuthenticator = TokenAuthenticator(tokenStorage, tokenRefresher)

    @Provides
    @Singleton
    @Named("refresh")
    fun provideRefreshOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val debug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return WellPaidRetrofit.createOkHttpClient(debug) {}
    }

    @Provides
    @Singleton
    fun provideAuthApi(@Named("refresh") client: OkHttpClient): AuthApi =
        WellPaidRetrofit.createRetrofit(BuildConfig.API_BASE_URL, client)
            .create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        authHeaderInterceptor: AuthHeaderInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient {
        val debug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return WellPaidRetrofit.createOkHttpClient(debug) {
            addInterceptor(authHeaderInterceptor)
            authenticator(tokenAuthenticator)
        }
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        WellPaidRetrofit.createRetrofit(BuildConfig.API_BASE_URL, client)

    @Provides
    @Singleton
    fun provideDashboardApi(retrofit: Retrofit): DashboardApi =
        retrofit.create(DashboardApi::class.java)

    @Provides
    @Singleton
    fun provideExpensesApi(retrofit: Retrofit): ExpensesApi =
        retrofit.create(ExpensesApi::class.java)

    @Provides
    @Singleton
    fun provideCategoriesApi(retrofit: Retrofit): CategoriesApi =
        retrofit.create(CategoriesApi::class.java)

    @Provides
    @Singleton
    fun provideIncomesApi(retrofit: Retrofit): IncomesApi =
        retrofit.create(IncomesApi::class.java)

    @Provides
    @Singleton
    fun provideIncomeCategoriesApi(retrofit: Retrofit): IncomeCategoriesApi =
        retrofit.create(IncomeCategoriesApi::class.java)

    @Provides
    @Singleton
    fun provideGoalsApi(retrofit: Retrofit): GoalsApi =
        retrofit.create(GoalsApi::class.java)

    @Provides
    @Singleton
    fun provideEmergencyReserveApi(retrofit: Retrofit): EmergencyReserveApi =
        retrofit.create(EmergencyReserveApi::class.java)

    @Provides
    @Singleton
    fun provideShoppingListsApi(retrofit: Retrofit): ShoppingListsApi =
        retrofit.create(ShoppingListsApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi =
        retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideFamiliesApi(retrofit: Retrofit): FamiliesApi =
        retrofit.create(FamiliesApi::class.java)
}