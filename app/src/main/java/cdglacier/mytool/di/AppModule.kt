package cdglacier.mytool.di

import cdglacier.mytool.data.repository.AiRepository
import cdglacier.mytool.data.repository.AiRepositoryImpl
import cdglacier.mytool.data.repository.AutoCopyJournalScheduler
import cdglacier.mytool.data.repository.AutoCopyJournalSchedulerImpl
import cdglacier.mytool.data.repository.CalendarPermissionRepository
import cdglacier.mytool.data.repository.CalendarPermissionRepositoryImpl
import cdglacier.mytool.data.repository.GoogleCalendarRepository
import cdglacier.mytool.data.repository.GoogleCalendarRepositoryImpl
import cdglacier.mytool.data.repository.DailySummaryRepository
import cdglacier.mytool.data.repository.DailySummaryRepositoryImpl
import cdglacier.mytool.data.repository.HouseholdRepository
import cdglacier.mytool.data.repository.HouseholdRepositoryImpl
import cdglacier.mytool.data.repository.LocationPermissionRepository
import cdglacier.mytool.data.repository.LocationPermissionRepositoryImpl
import cdglacier.mytool.data.repository.LocationRecordRepository
import cdglacier.mytool.data.repository.LocationRecordRepositoryImpl
import cdglacier.mytool.data.repository.MoneyRepository
import cdglacier.mytool.data.repository.MoneyRepositoryImpl
import cdglacier.mytool.data.repository.TrackingStateRepository
import cdglacier.mytool.data.repository.TrackingStateRepositoryImpl
import cdglacier.mytool.data.repository.JournalRepository
import cdglacier.mytool.data.repository.JournalRepositoryImpl
import cdglacier.mytool.data.repository.ObsidianRepository
import cdglacier.mytool.data.repository.ObsidianRepositoryImpl
import cdglacier.mytool.data.repository.OgpRepository
import cdglacier.mytool.data.repository.OgpRepositoryImpl
import cdglacier.mytool.data.repository.RecipeRepository
import cdglacier.mytool.data.repository.RecipeRepositoryImpl
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

    @Binds
    @Singleton
    abstract fun bindDailySummaryRepository(impl: DailySummaryRepositoryImpl): DailySummaryRepository

    @Binds
    @Singleton
    abstract fun bindLocationRecordRepository(impl: LocationRecordRepositoryImpl): LocationRecordRepository

    @Binds
    @Singleton
    abstract fun bindTrackingStateRepository(impl: TrackingStateRepositoryImpl): TrackingStateRepository

    @Binds
    @Singleton
    abstract fun bindLocationPermissionRepository(impl: LocationPermissionRepositoryImpl): LocationPermissionRepository

    @Binds
    @Singleton
    abstract fun bindCalendarPermissionRepository(impl: CalendarPermissionRepositoryImpl): CalendarPermissionRepository

    @Binds
    @Singleton
    abstract fun bindAutoCopyJournalScheduler(impl: AutoCopyJournalSchedulerImpl): AutoCopyJournalScheduler

    @Binds
    @Singleton
    abstract fun bindMoneyRepository(impl: MoneyRepositoryImpl): MoneyRepository

    @Binds
    @Singleton
    abstract fun bindOgpRepository(impl: OgpRepositoryImpl): OgpRepository

    @Binds
    @Singleton
    abstract fun bindRecipeRepository(impl: RecipeRepositoryImpl): RecipeRepository

    @Binds
    @Singleton
    abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository

    @Binds
    @Singleton
    abstract fun bindHouseholdRepository(impl: HouseholdRepositoryImpl): HouseholdRepository
}
