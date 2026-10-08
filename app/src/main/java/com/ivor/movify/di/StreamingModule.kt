package com.ivor.movify.di

import com.ivor.movify.data.streaming.StreamingRepositoryImpl
import com.ivor.movify.domain.repository.StreamingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Stream providers are no longer declared here. They are created at runtime by
 * `ExtensionProviderRegistry` from whichever extensions the user installed from the marketplace.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class StreamingBindingsModule {
    @Binds
    abstract fun bindStreamingRepository(implementation: StreamingRepositoryImpl): StreamingRepository
}
