package com.indiza.cotisation.network

import android.content.Context
import android.content.SharedPreferences
import com.indiza.cotisation.caisses.CaisseApi
import com.indiza.cotisation.sync.ConnectivityObserver
import com.indiza.cotisation.sync.NetworkConnectivityObserver
import com.indiza.cotisation.tontine.TontineApi
import com.indiza.cotisation.users.NetworkUserApi
import com.indiza.cotisation.users.UserApi
import com.indiza.cotisation.users.UserApiImpl
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val DEFAULT_URL = "http://192.168.1.50:3000/"
    private const val KEY_API_URL = "api_base_url"

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient, 
        moshi: Moshi,
        prefs: SharedPreferences
    ): Retrofit {
        val baseUrl = prefs.getString(KEY_API_URL, DEFAULT_URL) ?: DEFAULT_URL
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideTontineApi(retrofit: Retrofit): TontineApi = retrofit.create(TontineApi::class.java)

    @Provides
    @Singleton
    fun provideCaisseApi(retrofit: Retrofit): CaisseApi = retrofit.create(CaisseApi::class.java)

    @Provides
    @Singleton
    fun provideNetworkUserApi(retrofit: Retrofit): NetworkUserApi = retrofit.create(NetworkUserApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(remoteApi: RemoteApi): UserApi = UserApiImpl(remoteApi)

    @Provides
    @Singleton
    fun provideRemoteApi(
        tontineApi: TontineApi,
        caisseApi: CaisseApi,
        networkUserApi: NetworkUserApi
    ): RemoteApi {
        return RemoteApiImpl(tontineApi, caisseApi, networkUserApi)
    }


    @Provides
    @Singleton
    fun provideConnectivityObserver(@ApplicationContext context: Context): ConnectivityObserver {
        return NetworkConnectivityObserver(context)
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("cotisation_prefs", Context.MODE_PRIVATE)
    }

}