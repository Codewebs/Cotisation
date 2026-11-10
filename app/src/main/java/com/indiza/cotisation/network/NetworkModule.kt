package com.indiza.cotisation.network

import android.content.Context
import com.indiza.cotisation.sync.ConnectivityObserver
import com.indiza.cotisation.sync.NetworkConnectivityObserver
import com.indiza.cotisation.tontine.TontineApi
import com.indiza.cotisation.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SettingsSessionManager
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.SupabaseClient
import io.ktor.client.engine.okhttp.OkHttp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSupabaseConfiguration(): SupabaseConfiguration {
        val url = if (BuildConfig.SUPABASE_URL.isNotBlank()) BuildConfig.SUPABASE_URL else "https://chdwcbaxshpncetksdwc.supabase.co"
        val key = if (BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) BuildConfig.SUPABASE_ANON_KEY else "sb_publishable_oM4xzSZ0g-okR4cLzpC6yQ_SYsf760F"
        return SupabaseConfiguration(url, key)
    }

    @Provides
    @Singleton
    fun provideSupabaseClient(configuration: SupabaseConfiguration): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = configuration.url.ifBlank { "https://chdwcbaxshpncetksdwc.supabase.co" },
            supabaseKey = configuration.anonKey.ifBlank { "sb_publishable_oM4xzSZ0g-okR4cLzpC6yQ_SYsf760F" }
        ) {
            httpEngine = OkHttp.create()
            install(Auth) {
                autoLoadFromStorage = true
                autoSaveToStorage = true
                alwaysAutoRefresh = true
                sessionManager = SettingsSessionManager()
            }
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }

    @Provides
    @Singleton
    fun provideTontineApi(api: SupabaseDataApi): TontineApi = api

    @Provides
    @Singleton
    fun provideRemoteApi(api: SupabaseDataApi): RemoteApi = api

    @Provides
    @Singleton
    fun provideConnectivityObserver(@ApplicationContext context: Context): ConnectivityObserver {
        return NetworkConnectivityObserver(context)
    }

}