package com.tott.app.di

import android.content.Context
import com.tott.app.data.MyObjectBox
import com.tott.app.network.TottApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.objectbox.BoxStore
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideBoxStore(@ApplicationContext ctx: Context): BoxStore =
        MyObjectBox.builder().androidContext(ctx).build()

    // Networking stack (Retrofit) — required by spec, unused in V1 offline tasks.
    // Provided so future sync uses this instead of another client.
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("https://example.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideApi(r: Retrofit): TottApi = r.create(TottApi::class.java)
}
