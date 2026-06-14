package cdglacier.mytool.di

import cdglacier.mytool.data.repository.GoogleCalendarRepository
import cdglacier.mytool.data.repository.GoogleCalendarRepositoryImpl
import cdglacier.mytool.data.repository.JournalRepository
import cdglacier.mytool.data.repository.JournalRepositoryImpl
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.data.repository.ObsidianRepositoryImpl
import cdglacier.mytool.data.repository.WidgetConfigRepository
import cdglacier.mytool.data.repository.WidgetConfigRepositoryImpl
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

    @Binds
    @Singleton
    abstract fun bindJournalRepository(impl: JournalRepositoryImpl): JournalRepository

    @Binds
    @Singleton
    abstract fun bindWidgetConfigRepository(impl: WidgetConfigRepositoryImpl): WidgetConfigRepository
}
