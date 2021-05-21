package com.stevecc.topgithubrepos.api.repository

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryApiModule {
    @Provides
    @Singleton
    fun provideRepositoryService(retrofit: Retrofit): RepositoryService {
        return retrofit.create(RepositoryService::class.java)
    }
}
