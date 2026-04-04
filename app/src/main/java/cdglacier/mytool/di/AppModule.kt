package cdglacier.mytool.di

import cdglacier.mytool.data.repository.GoogleCalendarRepository
import cdglacier.mytool.data.repository.GoogleCalendarRepositoryImpl
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.data.repository.ObsidianRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindObsidianRepository(impl: ObsidianRepositoryImpl): ObsidianRepository

    @Binds
    @Singleton
    abstract fun bindGoogleCalendarRepository(impl: GoogleCalendarRepositoryImpl): GoogleCalendarRepository
}
