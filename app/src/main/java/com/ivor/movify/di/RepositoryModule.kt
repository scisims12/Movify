package com.ivor.movify.di

import com.ivor.movify.data.repository.AnimeRepositoryImpl
import com.ivor.movify.data.repository.DownloadRepositoryImpl
import com.ivor.movify.data.repository.WatchLaterRepositoryImpl
import com.ivor.movify.data.repository.WatchProgressRepositoryImpl
import com.ivor.movify.data.repository.CombinedSubtitleRepository
import com.ivor.movify.domain.repository.SubtitleRepository
import com.ivor.movify.data.extensions.ExtensionRepositoryImpl
import com.ivor.movify.domain.repository.AnimeRepository
import com.ivor.movify.domain.repository.DownloadRepository
import com.ivor.movify.domain.repository.ExtensionRepository
import com.ivor.movify.domain.repository.WatchLaterRepository
import com.ivor.movify.domain.repository.WatchProgressRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAnimeRepository(
        animeRepositoryImpl: AnimeRepositoryImpl
    ): AnimeRepository

    @Binds
    abstract fun bindWatchLaterRepository(
        watchLaterRepositoryImpl: WatchLaterRepositoryImpl
    ): WatchLaterRepository

    @Binds
    abstract fun bindDownloadRepository(
        downloadRepositoryImpl: DownloadRepositoryImpl
    ): DownloadRepository

    @Binds
    abstract fun bindExtensionRepository(
        extensionRepositoryImpl: ExtensionRepositoryImpl
    ): ExtensionRepository

    @Binds
    abstract fun bindWatchProgressRepository(
        watchProgressRepositoryImpl: WatchProgressRepositoryImpl
    ): WatchProgressRepository

    @Binds
    abstract fun bindSubtitleRepository(
        combinedSubtitleRepository: CombinedSubtitleRepository
    ): SubtitleRepository
}
